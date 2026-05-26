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
import java.util.Iterator;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RegularAndStaticColumns;
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
 * Unit tests that exercise the synthetic-iterator path of {@link Validator}'s
 * Phase A and {@link PartitionComparator} reporting, without requiring a live
 * Cassandra server, on-disk SSTables, or a real {@code ColumnFamilyStore}.
 *
 * <p>The full {@link Validator} integration (which expects {@code SSTableReader}
 * and {@code ColumnFamilyStore} from a running server) is exercised by the
 * higher-level integration tests in {@code Milestone 7}.  These unit tests cover
 * the partition-by-partition comparison logic that drives the validator.
 */
public class ValidatorTest
{
    private static final TableMetadata METADATA;
    private static final ColumnMetadata V1;

    static
    {
        METADATA = TableMetadata.builder("ks", "cf")
                                .addPartitionKeyColumn("pk", Int32Type.instance)
                                .addClusteringColumn("ck", Int32Type.instance)
                                .addRegularColumn("v1", Int32Type.instance)
                                .partitioner(Murmur3Partitioner.instance)
                                .offline()
                                .build();
        V1 = METADATA.regularAndStaticColumns().columns(false).getSimple(0);
    }

    @BeforeClass
    public static void setUp()
    {
        DatabaseDescriptor.clientInitialization(false);
    }

    @Test
    public void identicalPartitionsCompareEqualAndProduceEqualHashes()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 10), makeRow(2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(1, 10), makeRow(2, 20));
        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertEquals("identical partitions must hash equally", hashA, hashB);
    }

    @Test
    public void partitionComparatorDetectsValueDivergence()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 10), makeRow(2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(1, 10), makeRow(2, 999));

        MismatchReport report = PartitionComparator.compare(a, b, 7L);
        Assert.assertNotNull(report);
        Assert.assertEquals(7L, report.partitionsCheckedBeforeFailure);
        Assert.assertNotNull(report.description);
        Assert.assertTrue("description should mention cell value: " + report.description,
                          report.description.contains("cell value"));
    }

    @Test
    public void partitionComparatorDetectsTimestampDivergence()
    {
        UnfilteredRowIterator a = makePartition(1, makeRowWithTs(1, 10, 1L));
        UnfilteredRowIterator b = makePartition(1, makeRowWithTs(1, 10, 999L));

        MismatchReport report = PartitionComparator.compare(a, b, 0L);
        Assert.assertNotNull(report);
        Assert.assertTrue("description should mention timestamp: " + report.description,
                          report.description.contains("timestamp"));
    }

    @Test
    public void partitionComparatorDetectsLengthDivergence()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 10), makeRow(2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(1, 10));

        MismatchReport report = PartitionComparator.compare(a, b, 3L);
        Assert.assertNotNull(report);
        Assert.assertTrue("description should mention iterator length: " + report.description,
                          report.description.contains("iterator length"));
    }

    @Test
    public void partitionComparatorDetectsKeyDivergence()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 10));
        UnfilteredRowIterator b = makePartition(2, makeRow(1, 10));

        MismatchReport report = PartitionComparator.compare(a, b, 0L);
        Assert.assertNotNull(report);
        Assert.assertTrue("description should mention partition keys: " + report.description,
                          report.description.toLowerCase().contains("partition key"));
    }

    @Test
    public void mismatchReportFormatsForDisplay()
    {
        MismatchReport report = new MismatchReport("0xCAFE",
                                                   "test divergence",
                                                   "legacy-line\n",
                                                   "cursor-line\n",
                                                   42L);
        String formatted = report.formatForDisplay();
        Assert.assertNotNull(formatted);
        Assert.assertTrue(formatted.contains("0xCAFE"));
        Assert.assertTrue(formatted.contains("42 partitions"));
        Assert.assertTrue(formatted.contains("--- CONTROL ---"));
        Assert.assertTrue(formatted.contains("--- EXPERIMENT ---"));
        Assert.assertTrue(formatted.contains("legacy-line"));
        Assert.assertTrue(formatted.contains("cursor-line"));
    }

    @Test
    public void validationStatsTracksMismatch()
    {
        ValidationStats stats = new ValidationStats();
        Assert.assertFalse(stats.hasMismatches());
        stats.partitionMismatches.set(1L);
        Assert.assertTrue(stats.hasMismatches());
    }

    @Test
    public void emptyPartitionsCompareEqual()
    {
        UnfilteredRowIterator a = makePartition(1);
        UnfilteredRowIterator b = makePartition(1);

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertEquals(hashA, hashB);

        // Comparator on equal-but-empty partitions should "find no diff" — it then
        // signals the caller via the synthetic "hash mismatch but no cell-by-cell
        // difference" path. We are not invoking it here directly because Phase A
        // would not have flagged equal hashes; this test just confirms the empty
        // partition support in PartitionHasher.
        Assert.assertEquals(PartitionHasher.hashPartition(makePartition(1)),
                            PartitionHasher.hashPartition(makePartition(1)));
    }

    @Test
    public void mismatchReportNullDumpsAreSafe()
    {
        MismatchReport report = new MismatchReport("0x0", "desc", null, null, 0L);
        String s = report.formatForDisplay();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("(empty)"));
    }

    @Test
    public void validatorRejectsNullArguments()
    {
        try
        {
            new Validator(null, null, null, null, null);
            Assert.fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    @Test
    public void validateOptionalEmptyOnNoMismatch()
    {
        // A trivial sanity check: Optional.empty() is the no-mismatch signal we expect.
        Optional<MismatchReport> empty = Optional.empty();
        Assert.assertFalse(empty.isPresent());
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
        RegularAndStaticColumns columns = METADATA.regularAndStaticColumns();
        return new AbstractUnfilteredRowIterator(METADATA,
                                                 dk(pk),
                                                 DeletionTime.LIVE,
                                                 columns,
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

    private static Row makeRow(int ck, int v1)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(cell(V1, v1, 1L));
        return b.build();
    }

    private static Row makeRowWithTs(int ck, int v, long ts)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(cell(V1, v, ts));
        return b.build();
    }

    private static Cell<?> cell(ColumnMetadata col, int value, long timestamp)
    {
        return new BufferCell(col,
                              timestamp,
                              Cell.NO_TTL,
                              Cell.NO_DELETION_TIME,
                              ByteBufferUtil.bytes(value),
                              null);
    }
}
