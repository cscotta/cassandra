/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.tools.compactionvalidator.validation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.rows.AbstractUnfilteredRowIterator;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Unit tests for {@link FormatAuditor} — drives the auditor with in-memory
 * {@link UnfilteredRowIterator}s constructed to either honor or deliberately
 * violate each invariant, and asserts the auditor classifies them correctly.
 *
 * <p>Tests on synthetic iterators (rather than real on-disk SSTables) keep the
 * suite fast — the per-invariant checks live in the auditor's static helpers
 * which don't touch the SSTable byte format directly. Round-trip coverage on
 * real bytes is provided by the end-to-end smoke run.
 */
public class FormatAuditorTest
{
    private static final TableMetadata METADATA;
    private static final ColumnMetadata REGULAR_INT;

    static
    {
        METADATA = TableMetadata.builder("ks", "cf")
                                .addPartitionKeyColumn("pk", Int32Type.instance)
                                .addClusteringColumn("ck", Int32Type.instance)
                                .addRegularColumn("v", Int32Type.instance)
                                .partitioner(Murmur3Partitioner.instance)
                                .offline()
                                .build();
        REGULAR_INT = METADATA.regularAndStaticColumns().columns(false).getSimple(0);
    }

    @BeforeClass
    public static void setUp()
    {
        DatabaseDescriptor.clientInitialization(false);
    }

    @Test
    public void cleanPartitionProducesNoViolations()
    {
        // All cells live with no TTL: the auditor's cell checks are no-ops.
        UnfilteredRowIterator p = makePartition(1,
            makeRow(1, liveCell(REGULAR_INT, 10, 1L)),
            makeRow(2, liveCell(REGULAR_INT, 20, 2L)));
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        Assert.assertTrue("clean partition must produce zero violations: " + out, out.isEmpty());
    }

    @Test
    public void wellFormedExpiringCellProducesNoViolation()
    {
        // Properly formed expiring cell: localDeletionTime == writetime + ttl.
        long timestamp = 1_700_000_000_000_000L;     // micros
        long writetime = timestamp / 1_000_000L;
        int ttl = 86_400;
        int expiration = (int) (writetime + ttl);
        Cell<?> ok = new BufferCell(REGULAR_INT, timestamp, ttl, expiration,
                                    ByteBufferUtil.bytes(42), null);

        UnfilteredRowIterator p = makePartition(1, makeRow(1, ok));
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        Assert.assertTrue("well-formed expiring cell must produce no violation: " + out,
                          out.isEmpty());
    }

    @Test
    public void brokenExpirationFormulaIsFlagged()
    {
        // Bug 1A signature: cell claims to be expiring but its localDeletionTime
        // doesn't equal writetime + ttl. The auditor should report
        // FLAG_DELETE_AND_EXPIRE.
        long timestamp = 1_700_000_000_000_000L;
        long writetime = timestamp / 1_000_000L;
        int ttl = 86_400;
        int wrongExpiration = (int) (writetime + ttl + 100_000);   // 100K seconds off
        Cell<?> broken = new BufferCell(REGULAR_INT, timestamp, ttl, wrongExpiration,
                                         ByteBufferUtil.bytes(42), null);

        UnfilteredRowIterator p = makePartition(1, makeRow(1, broken));
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "buggy.db", out);
        Assert.assertEquals("expected exactly one FLAG_DELETE_AND_EXPIRE violation",
                            1, out.size());
        Assert.assertEquals(FormatViolation.Kind.FLAG_DELETE_AND_EXPIRE, out.get(0).kind);
        Assert.assertEquals("buggy.db", out.get(0).sstableFilename);
    }

    @Test
    public void expirationFormulaWithinToleranceIsClean()
    {
        // The auditor allows ±2 seconds of slack on the expiration formula. A
        // cell that's 1 second off should NOT be flagged — the rare-but-real
        // future-Cassandra-rounding edge case the tolerance is there for.
        long timestamp = 1_700_000_000_000_000L;
        long writetime = timestamp / 1_000_000L;
        int ttl = 86_400;
        int slightlyOff = (int) (writetime + ttl + 1);  // 1 second over
        Cell<?> nearBoundary = new BufferCell(REGULAR_INT, timestamp, ttl, slightlyOff,
                                               ByteBufferUtil.bytes(42), null);

        UnfilteredRowIterator p = makePartition(1, makeRow(1, nearBoundary));
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        Assert.assertTrue("1-second-off cell within tolerance must produce no violation",
                          out.isEmpty());
    }

    @Test
    public void rowOutOfOrderIsFlagged()
    {
        // Construct a partition with rows in DECREASING clustering order. The
        // auditor's clustering-comparator check should flag the second row.
        UnfilteredRowIterator p = makePartition(1,
            makeRow(2, liveCell(REGULAR_INT, 20, 2L)),
            makeRow(1, liveCell(REGULAR_INT, 10, 1L)));     // ← ck=1 after ck=2
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        Assert.assertFalse("out-of-order rows must produce a violation", out.isEmpty());
        // Strict reverse — must be classified as ROW_OUT_OF_ORDER (not DUPLICATE_CLUSTERING).
        Assert.assertEquals(FormatViolation.Kind.ROW_OUT_OF_ORDER, out.get(0).kind);
    }

    @Test
    public void duplicateClusteringIsFlaggedSeparately()
    {
        // Construct a partition with two rows that share the SAME clustering.
        // The auditor's check should report DUPLICATE_CLUSTERING — not
        // ROW_OUT_OF_ORDER — so the errata matcher can suppress this case
        // (it's the downstream cascade of bug 1A) while still failing loudly
        // on real strict-reverse ordering bugs.
        UnfilteredRowIterator p = makePartition(1,
            makeRow(7, liveCell(REGULAR_INT, 10, 1L)),
            makeRow(7, liveCell(REGULAR_INT, 20, 2L)));     // ← same ck as previous row
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        boolean foundDup = false;
        for (FormatViolation v : out)
            if (v.kind == FormatViolation.Kind.DUPLICATE_CLUSTERING) { foundDup = true; break; }
        Assert.assertTrue("duplicate clustering must produce DUPLICATE_CLUSTERING (not ROW_OUT_OF_ORDER): "
                          + out, foundDup);
        // And NOT ROW_OUT_OF_ORDER — the kinds are deliberately split for the
        // errata-suppression decision.
        for (FormatViolation v : out)
            Assert.assertNotEquals("duplicate-clustering must not be reported as ROW_OUT_OF_ORDER",
                                   FormatViolation.Kind.ROW_OUT_OF_ORDER, v.kind);
    }

    @Test
    public void negativeTimestampIsFlagged()
    {
        Cell<?> bad = new BufferCell(REGULAR_INT,
                                      -1L,                          // ← negative timestamp
                                      Cell.NO_TTL,
                                      Cell.NO_DELETION_TIME,
                                      ByteBufferUtil.bytes(42),
                                      null);
        UnfilteredRowIterator p = makePartition(1, makeRow(1, bad));
        List<FormatViolation> out = new ArrayList<>();
        FormatAuditor.auditPartition(p, METADATA.comparator, "synthetic", out);
        // Sanity: at least one violation, and the negative-timestamp kind is among them.
        Assert.assertTrue("negative-timestamp cell must produce at least one violation",
                          !out.isEmpty());
        boolean found = false;
        for (FormatViolation v : out)
            if (v.kind == FormatViolation.Kind.TIMESTAMP_NEGATIVE) { found = true; break; }
        Assert.assertTrue("expected TIMESTAMP_NEGATIVE among reported violations: " + out,
                          found);
    }

    @Test
    public void emptyAuditOnEmptySSTableList()
    {
        List<FormatViolation> out = FormatAuditor.audit(java.util.Collections.emptyList(),
                                                        METADATA.comparator);
        Assert.assertTrue("empty input must produce empty output", out.isEmpty());
    }

    // ---- Helpers --------------------------------------------------------

    private static DecoratedKey dk(int pk)
    {
        ByteBuffer key = ByteBufferUtil.bytes(pk);
        return new BufferDecoratedKey(METADATA.partitioner.getToken(key), key);
    }

    private static UnfilteredRowIterator makePartition(int pk, Row... rows)
    {
        final Iterator<Row> it = Arrays.asList(rows).iterator();
        return new AbstractUnfilteredRowIterator(METADATA,
                                                 dk(pk),
                                                 DeletionTime.LIVE,
                                                 METADATA.regularAndStaticColumns(),
                                                 Rows.EMPTY_STATIC_ROW,
                                                 false,
                                                 EncodingStats.NO_STATS)
        {
            @Override
            protected Unfiltered computeNext()
            {
                return it.hasNext() ? it.next() : endOfData();
            }
        };
    }

    private static Row makeRow(int ck, Cell<?> cell)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(cell);
        return b.build();
    }

    private static Cell<?> liveCell(ColumnMetadata col, int value, long timestamp)
    {
        return new BufferCell(col,
                              timestamp,
                              Cell.NO_TTL,
                              Cell.NO_DELETION_TIME,
                              ByteBufferUtil.bytes(value),
                              null);
    }
}
