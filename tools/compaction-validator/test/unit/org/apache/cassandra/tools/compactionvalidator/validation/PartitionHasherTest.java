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
 * Unit tests for {@link PartitionHasher} that exercise the small-partition fast path
 * without spinning up a full Cassandra server.
 *
 * <p>The harness constructs in-memory {@link UnfilteredRowIterator}s using the same
 * idioms as {@code UnfilteredRowIteratorsTest} so we don't need on-disk SSTables.
 */
public class PartitionHasherTest
{
    private static final TableMetadata METADATA;
    private static final ColumnMetadata V1;
    private static final ColumnMetadata V2;

    static
    {
        METADATA = TableMetadata.builder("ks", "cf")
                                .addPartitionKeyColumn("pk", Int32Type.instance)
                                .addClusteringColumn("ck", Int32Type.instance)
                                .addRegularColumn("v1", Int32Type.instance)
                                .addRegularColumn("v2", Int32Type.instance)
                                .partitioner(Murmur3Partitioner.instance)
                                .offline()
                                .build();
        V1 = METADATA.regularAndStaticColumns().columns(false).getSimple(0);
        V2 = METADATA.regularAndStaticColumns().columns(false).getSimple(1);
    }

    @BeforeClass
    public static void setUp()
    {
        // Static initialisers in some Cassandra classes require DatabaseDescriptor to be
        // initialised at least to the "in-tools" level. Initialising lazily here is enough
        // for the offline TableMetadata above.
        DatabaseDescriptor.clientInitialization(false);
    }

    @Test
    public void identicalPartitionsHaveEqualHash()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20));

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertEquals("hashes of identical partitions must match", hashA, hashB);
    }

    @Test
    public void differentValuesProduceDifferentHash()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(1, 1, 10), makeRow(2, 99, 20));

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertNotEquals("hashes must differ when a cell value differs", hashA, hashB);
    }

    @Test
    public void differentPartitionKeyProducesDifferentHash()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 1, 10));
        UnfilteredRowIterator b = makePartition(2, makeRow(1, 1, 10));

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertNotEquals("hashes must differ when the partition key differs", hashA, hashB);
    }

    @Test
    public void differentRowOrderingProducesDifferentHash()
    {
        UnfilteredRowIterator a = makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20));
        UnfilteredRowIterator b = makePartition(1, makeRow(2, 2, 20), makeRow(1, 1, 10));

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        // The hasher does not sort; the order matters because compaction outputs are
        // produced in clustering order, and any divergence in order indicates a bug.
        Assert.assertNotEquals("hashes must reflect row ordering", hashA, hashB);
    }

    @Test
    public void differentRowCountProducesDifferentHash()
    {
        UnfilteredRowIterator shorter = makePartition(1, makeRow(1, 1, 10));
        UnfilteredRowIterator longer  = makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20));

        long hashShort = PartitionHasher.hashPartition(shorter);
        long hashLong  = PartitionHasher.hashPartition(longer);
        Assert.assertNotEquals("hashes must differ when row counts differ", hashShort, hashLong);
    }

    @Test
    public void differentTimestampProducesDifferentHash()
    {
        UnfilteredRowIterator a = makePartition(1, makeRowWithTs(1, 1, 1L));
        UnfilteredRowIterator b = makePartition(1, makeRowWithTs(1, 1, 2L));

        long hashA = PartitionHasher.hashPartition(a);
        long hashB = PartitionHasher.hashPartition(b);
        Assert.assertNotEquals("hashes must differ when timestamps differ", hashA, hashB);
    }

    @Test
    public void hashingTheSameLogicalPartitionTwiceIsDeterministic()
    {
        long h1 = PartitionHasher.hashPartition(makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20)));
        long h2 = PartitionHasher.hashPartition(makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20)));
        long h3 = PartitionHasher.hashPartition(makePartition(1, makeRow(1, 1, 10), makeRow(2, 2, 20)));
        Assert.assertEquals(h1, h2);
        Assert.assertEquals(h2, h3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPartitionRejected()
    {
        PartitionHasher.hashPartition(null);
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

    private static Row makeRow(int ck, int v1, int v2)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(cell(V1, v1, 1L));
        b.addCell(cell(V2, v2, 1L));
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
