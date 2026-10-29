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

package org.apache.cassandra.db.rows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.Util;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.UnfilteredDeserializer;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The row deserializers take their {@link Row.Builder} from the per-thread pool, and a row builder now sizes its
 * cell btree for a narrow row. Both are allocation changes: these assert the content a row is built with is
 * unaffected, including rows wider than the initial cell capacity and repeated deserializations that reuse a pooled
 * builder on the same thread.
 */
public class UnfilteredSerializerPooledBuilderTest
{
    private static final int COLUMN_COUNT = 8;

    private static TableMetadata metadata;
    private static ColumnMetadata[] columns;
    private static RegularAndStaticColumns regularColumns;

    @BeforeClass
    public static void setUpClass()
    {
        DatabaseDescriptor.daemonInitialization();

        TableMetadata.Builder builder = TableMetadata.builder("ks", "cf")
                                                     .addPartitionKeyColumn("pk", Int32Type.instance)
                                                     .addClusteringColumn("ck", Int32Type.instance);
        for (int i = 0; i < COLUMN_COUNT; i++)
            builder.addRegularColumn("v" + i, Int32Type.instance);
        builder.addStaticColumn("s0", Int32Type.instance);
        builder.addStaticColumn("s1", Int32Type.instance);
        metadata = builder.build();
        regularColumns = metadata.regularAndStaticColumns();

        columns = new ColumnMetadata[COLUMN_COUNT];
        for (int i = 0; i < COLUMN_COUNT; i++)
            columns[i] = metadata.getColumn(ByteBufferUtil.bytes("v" + i));
    }

    private static DecoratedKey key(int pk)
    {
        return new BufferDecoratedKey(new Murmur3Partitioner.LongToken(pk), ByteBufferUtil.bytes(pk));
    }

    private static Cell<?> cell(ColumnMetadata column, int value)
    {
        return new BufferCell(column, 1L, BufferCell.NO_TTL, BufferCell.NO_DELETION_TIME, ByteBufferUtil.bytes(value), null);
    }

    private static Row row(int ck, int... values)
    {
        Row.Builder builder = BTreeRow.sortedBuilder();
        builder.newRow(Util.clustering(metadata.comparator, ck));
        for (int i = 0; i < values.length; i++)
            builder.addCell(cell(columns[i], values[i]));
        return builder.build();
    }

    private static Row staticRow(int... values)
    {
        Row.Builder builder = BTreeRow.sortedBuilder();
        builder.newRow(Clustering.STATIC_CLUSTERING);
        for (int i = 0; i < values.length; i++)
            builder.addCell(cell(metadata.getColumn(ByteBufferUtil.bytes("s" + i)), values[i]));
        return builder.build();
    }

    private static UnfilteredRowIterator rows(DecoratedKey key, Row... rows)
    {
        return rowsWithStaticRow(key, Rows.EMPTY_STATIC_ROW, rows);
    }

    private static UnfilteredRowIterator rowsWithStaticRow(DecoratedKey key, Row staticRow, Row... rows)
    {
        Iterator<Row> iterator = Arrays.asList(rows).iterator();
        return new AbstractUnfilteredRowIterator(metadata, key, DeletionTime.LIVE, regularColumns, staticRow,
                                                 false, EncodingStats.NO_STATS)
        {
            protected Unfiltered computeNext()
            {
                return iterator.hasNext() ? iterator.next() : endOfData();
            }
        };
    }

    private static List<Row> deserialize(DataOutputBuffer buffer) throws IOException
    {
        try (DataInputBuffer in = new DataInputBuffer(buffer.buffer(), false);
             UnfilteredRowIterator iterator = UnfilteredRowIteratorSerializer.serializer.deserialize(in, MessagingService.current_version,
                                                                                                   metadata, ColumnFilter.all(metadata),
                                                                                                   DeserializationHelper.Flag.LOCAL))
        {
            List<Row> rows = new ArrayList<>();
            while (iterator.hasNext())
                rows.add((Row) iterator.next());
            return rows;
        }
    }

    private static void assertRows(List<Row> actual, Row... expected)
    {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++)
            assertEquals("row " + i, expected[i], actual.get(i));
    }

    @Test
    public void rowContentSurvivesRepeatedDeserialization() throws IOException
    {
        Row narrow = row(1, 1, 2);
        Row single = row(2, 3);
        // wider than the initial cell capacity of a row builder, so the btree has to grow
        Row wide = row(3, 4, 5, 6, 7, 8);

        DataOutputBuffer buffer = new DataOutputBuffer();
        UnfilteredRowIterator source = rows(key(1), narrow, single, wide);
        UnfilteredRowIteratorSerializer.serializer.serialize(source, ColumnFilter.all(metadata), buffer,
                                                             MessagingService.current_version);
        source.close();

        // repeated on the same thread: the second and later passes reuse the pooled builders
        for (int i = 0; i < 3; i++)
            assertRows(deserialize(buffer), narrow, single, wide);
    }

    @Test
    public void staticRowAndRowsSurviveDeserialization() throws IOException
    {
        // the static row goes through deserializeStaticRow, which takes its builder from the pool too
        Row staticRow = staticRow(11, 12);
        Row narrow = row(1, 1, 2);
        Row single = row(2, 3);

        DataOutputBuffer buffer = new DataOutputBuffer();
        UnfilteredRowIterator source = rowsWithStaticRow(key(3), staticRow, narrow, single);
        UnfilteredRowIteratorSerializer.serializer.serialize(source, ColumnFilter.all(metadata), buffer,
                                                             MessagingService.current_version);
        source.close();

        for (int i = 0; i < 2; i++)
        {
            try (DataInputBuffer in = new DataInputBuffer(buffer.buffer(), false);
                 UnfilteredRowIterator iterator = UnfilteredRowIteratorSerializer.serializer.deserialize(in, MessagingService.current_version,
                                                                                                        metadata, ColumnFilter.all(metadata),
                                                                                                        DeserializationHelper.Flag.LOCAL))
            {
                assertEquals(staticRow, iterator.staticRow());
                List<Row> rows = new ArrayList<>();
                while (iterator.hasNext())
                    rows.add((Row) iterator.next());
                assertRows(rows, narrow, single);
            }
        }
    }

    @Test
    public void sstableRowsDeserializeOneBuilderPerRow() throws IOException
    {
        // the sstable reader deserializes one row per UnfilteredDeserializer.readNext() call; write the partition the
        // way an sstable stores it (a serialization header for an sstable, one framed row body per row)
        Row narrow = row(1, 1, 2);
        Row wide = row(2, 4, 5, 6, 7, 8);

        SerializationHeader header = new SerializationHeader(true, metadata, regularColumns, EncodingStats.NO_STATS);
        DataOutputBuffer buffer = new DataOutputBuffer();
        SerializationHelper helper = new SerializationHelper(header);
        for (Row row : new Row[]{ narrow, wide })
            UnfilteredSerializer.serializer.serialize(row, helper, buffer, 0, MessagingService.current_version);
        UnfilteredSerializer.serializer.writeEndOfPartition(buffer);

        for (int i = 0; i < 2; i++)
        {
            List<Row> rows = new ArrayList<>();
            try (DataInputBuffer in = new DataInputBuffer(buffer.buffer(), false))
            {
                UnfilteredDeserializer deserializer = UnfilteredDeserializer.create(metadata, in, header,
                                                                                    new DeserializationHelper(metadata, MessagingService.current_version,
                                                                                                              DeserializationHelper.Flag.LOCAL));
                while (deserializer.hasNext())
                    rows.add((Row) deserializer.readNext());
            }
            assertRows(rows, narrow, wide);
        }
    }

    @Test
    public void emptyPartitionDeserializesToNoRows() throws IOException
    {
        DataOutputBuffer buffer = new DataOutputBuffer();
        UnfilteredRowIterator source = rows(key(2));
        UnfilteredRowIteratorSerializer.serializer.serialize(source, ColumnFilter.all(metadata), buffer,
                                                             MessagingService.current_version);
        source.close();

        assertRows(deserialize(buffer));
    }

    @Test
    public void buildingARowWiderThanTheInitialCellCapacityKeepsEveryCellInOrder()
    {
        // exercises the cell btree growth directly, without a round trip
        Row.Builder builder = BTreeRow.sortedBuilder();
        builder.newRow(Util.clustering(metadata.comparator, 5));
        for (int i = 0; i < COLUMN_COUNT; i++)
            builder.addCell(cell(columns[i], i));
        Row built = builder.build();

        List<ColumnMetadata> seen = new ArrayList<>();
        for (Cell<?> cell : built.cells())
            seen.add(cell.column());
        assertEquals(COLUMN_COUNT, seen.size());
        for (int i = 0; i < COLUMN_COUNT; i++)
            assertEquals(columns[i], seen.get(i));
        assertEquals(COLUMN_COUNT, built.columnCount());
        assertFalse(built.isEmpty());
        assertEquals(Util.clustering(metadata.comparator, 5), built.clustering());
    }
}
