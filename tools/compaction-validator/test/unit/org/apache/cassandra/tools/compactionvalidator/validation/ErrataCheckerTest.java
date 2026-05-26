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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.marshal.BytesType;
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
 * Unit tests for {@link ErrataChecker} — synthesises partition pairs that match
 * (or specifically don't match) the {@code equal-ts-tiebreaker} cursor-compaction
 * defect and verifies the matcher's classification:
 *
 * <ul>
 *   <li>identical partitions → empty rule set (no errata fired)</li>
 *   <li>cell-value-only diff at equal timestamp → rule fires</li>
 *   <li>cell-value diff + timestamp diff → empty set (real mismatch)</li>
 *   <li>missing row on one side → empty set (real mismatch)</li>
 *   <li>no rules active → empty set (matcher is a no-op)</li>
 *   <li>static row covers the exact pattern from the original bug report</li>
 * </ul>
 *
 * <p>This is the cheap proof-of-correctness for the matcher; the end-to-end
 * "real seed reproduces" verification is in the soak-run integration tests.
 */
public class ErrataCheckerTest
{
    private static final TableMetadata METADATA;
    private static final ColumnMetadata STATIC_BLOB;
    private static final ColumnMetadata REGULAR_INT;

    static
    {
        METADATA = TableMetadata.builder("ks", "cf")
                                .addPartitionKeyColumn("pk", Int32Type.instance)
                                .addClusteringColumn("ck", Int32Type.instance)
                                .addStaticColumn("s1", BytesType.instance)
                                .addRegularColumn("v", Int32Type.instance)
                                .partitioner(Murmur3Partitioner.instance)
                                .offline()
                                .build();
        STATIC_BLOB = METADATA.regularAndStaticColumns().columns(true).getSimple(0);
        REGULAR_INT = METADATA.regularAndStaticColumns().columns(false).getSimple(0);
    }

    @BeforeClass
    public static void setUp()
    {
        DatabaseDescriptor.clientInitialization(false);
    }

    @Test
    public void noActiveRulesIsNoOp()
    {
        ErrataChecker c = new ErrataChecker(Collections.emptySet());
        Assert.assertFalse(c.isActive());

        // Two divergent partitions — without active rules, matcher should report
        // empty set (caller treats as real mismatch).
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 20, 1L))));
        Assert.assertTrue("matcher must be a no-op when no rules active", fired.isEmpty());
    }

    @Test
    public void identicalPartitionsFireNoRules()
    {
        ErrataChecker c = new ErrataChecker(EnumSet.allOf(ErrataRule.class));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))));
        Assert.assertTrue("identical partitions should fire no errata rules", fired.isEmpty());
    }

    @Test
    public void equalTimestampValueDiffFiresRule()
    {
        // The bug signature: two cells in the same row, same column, same
        // timestamp, same TTL, same localDeletionTime — but DIFFERENT value bytes.
        // Legacy correctly keeps the larger value (0x20); cursor incorrectly keeps
        // the smaller (0x10). ErrataChecker should classify this as the known rule.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x20, 1779670427680000L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x10, 1779670427680000L))));

        Assert.assertEquals("equal-ts value-only diff must fire EQUAL_TIMESTAMP_TIEBREAKER",
                            EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER), fired);
    }

    @Test
    public void valueDiffWithDifferentTimestampIsRealMismatch()
    {
        // Value differs AND timestamps differ — that's not the tie-breaker bug,
        // it's a real timestamp-resolution divergence. Rule must NOT fire.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x20, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x10, 2L))));

        Assert.assertTrue("different-timestamp diff must NOT be suppressed by equal-ts rule",
                          fired.isEmpty());
    }

    @Test
    public void staticRowEqualTimestampValueDiffFiresRule()
    {
        // The original reported bug: STATIC blob cell with equal timestamps and
        // differing value bytes. Same path through ErrataChecker as regular cells,
        // but exercises the staticRow branch explicitly.
        ByteBuffer larger  = ByteBuffer.wrap(new byte[] { (byte) 0xb9, (byte) 0xe5 });
        ByteBuffer smaller = ByteBuffer.wrap(new byte[] { (byte) 0x94, (byte) 0xf2 });
        long ts = 1779670427680000L;

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartitionWithStatic(1, larger,  ts),
            makePartitionWithStatic(1, smaller, ts));

        Assert.assertEquals("static-cell equal-ts diff must fire EQUAL_TIMESTAMP_TIEBREAKER",
                            EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER), fired);
    }

    @Test
    public void missingRowIsRealMismatch()
    {
        // Legacy has two rows, cursor has one — no rule covers a missing-row
        // divergence. Must surface as real mismatch.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L)),
                            makeRowWithCell(2, intCell(REGULAR_INT, 20, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))));

        Assert.assertTrue("missing row must NOT be suppressed", fired.isEmpty());
    }

    @Test
    public void cliParseListRejectsUnknownRules()
    {
        // The CLI parser should fail loudly on typos rather than silently no-op'ing.
        try
        {
            ErrataRule.parseCliList("equal-ts-tiebreaker,bogus-rule");
            Assert.fail("expected IllegalArgumentException on unknown rule");
        }
        catch (IllegalArgumentException expected)
        {
            Assert.assertTrue("error message should name the unknown rule",
                              expected.getMessage().contains("bogus-rule"));
        }
    }

    @Test
    public void cliParseListAcceptsEmptyAndNull()
    {
        Assert.assertTrue(ErrataRule.parseCliList(null).isEmpty());
        Assert.assertTrue(ErrataRule.parseCliList("").isEmpty());
        Assert.assertTrue(ErrataRule.parseCliList(" , , ").isEmpty());
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

    /**
     * Partition with a single static-cell value (no body rows). Mirrors the
     * shape of the bug-report partition: only the static row differs.
     */
    private static UnfilteredRowIterator makePartitionWithStatic(int pk, ByteBuffer staticBlobValue, long ts)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(org.apache.cassandra.db.Clustering.STATIC_CLUSTERING);
        b.addCell(new BufferCell(STATIC_BLOB, ts, Cell.NO_TTL, Cell.NO_DELETION_TIME, staticBlobValue, null));
        final Row staticRow = b.build();

        return new AbstractUnfilteredRowIterator(METADATA,
                                                 dk(pk),
                                                 DeletionTime.LIVE,
                                                 METADATA.regularAndStaticColumns(),
                                                 staticRow,
                                                 false,
                                                 EncodingStats.NO_STATS)
        {
            @Override
            protected Unfiltered computeNext()
            {
                return endOfData();
            }
        };
    }

    private static Row makeRowWithCell(int ck, Cell<?> c)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(c);
        return b.build();
    }

    private static Cell<?> intCell(ColumnMetadata col, int value, long timestamp)
    {
        return new BufferCell(col,
                              timestamp,
                              Cell.NO_TTL,
                              Cell.NO_DELETION_TIME,
                              ByteBufferUtil.bytes(value),
                              null);
    }
}
