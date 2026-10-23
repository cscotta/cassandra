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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.Util;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.partitions.ImmutableBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the iterator a partition without range tombstones uses ({@link RowsOnlyIterator}) yields exactly
 * what {@link RowAndDeletionMergeIterator} yields with an empty range tombstone source: the same rows, filtered by
 * the column selection and by the partition deletion.
 */
public class RowsOnlyIteratorTest
{
    private static final String KEYSPACE1 = "RowsOnlyIteratorTest";
    private static final String CF_STANDARD1 = "Standard1";

    private DecoratedKey dk;
    private TableMetadata cfm;
    private ColumnMetadata defA;
    private ColumnMetadata defB;

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        DatabaseDescriptor.daemonInitialization();

        TableMetadata.Builder builder =
            TableMetadata.builder(KEYSPACE1, CF_STANDARD1)
                         .addPartitionKeyColumn("key", AsciiType.instance)
                         .addClusteringColumn("col1", Int32Type.instance)
                         .addRegularColumn("a", Int32Type.instance)
                         .addRegularColumn("b", Int32Type.instance);

        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace(KEYSPACE1, KeyspaceParams.simple(1), builder);
    }

    @Before
    public void setup()
    {
        dk = Util.dk("key0");
        ColumnFamilyStore cfs = Keyspace.open(KEYSPACE1).getColumnFamilyStore(CF_STANDARD1);
        cfm = cfs.metadata();
        defA = cfm.getColumn(new ColumnIdentifier("a", true));
        defB = cfm.getColumn(new ColumnIdentifier("b", true));
    }

    @Test
    public void testWholePartition()
    {
        ImmutableBTreePartition partition = partition(DeletionTime.LIVE);

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false),
                           rows(0, 1, 2, 3, 4), ColumnFilter.all(cfm), DeletionTime.LIVE, false);

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, true),
                           rows(4, 3, 2, 1, 0), ColumnFilter.all(cfm), DeletionTime.LIVE, true);
    }

    @Test
    public void testSingleRowSlice()
    {
        ImmutableBTreePartition partition = partition(DeletionTime.LIVE);
        Slices slice = Slices.with(cfm.comparator, Slice.make(cfm.comparator.make(2)));

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), slice, false),
                           rows(2), ColumnFilter.all(cfm), DeletionTime.LIVE, false);
    }

    @Test
    public void testRowsByName()
    {
        ImmutableBTreePartition partition = partition(DeletionTime.LIVE);

        Iterator<Unfiltered> expected = merge(rows(1, 3), ColumnFilter.all(cfm), DeletionTime.LIVE, false);
        assertEquals(describe(expected),
                     describe(partition.unfilteredIterator(ColumnFilter.all(cfm), names(1, 3), false)));
    }

    @Test
    public void testMissingRowByName()
    {
        ImmutableBTreePartition partition = partition(DeletionTime.LIVE);

        Iterator<Unfiltered> actual = partition.unfilteredIterator(ColumnFilter.all(cfm), names(42), false);
        assertFalse(actual.hasNext());
    }

    /**
     * The column selection is applied by the merge iterator per row; the rows-only iterator has to apply it too.
     */
    @Test
    public void testColumnSelection()
    {
        ImmutableBTreePartition partition = partition(DeletionTime.LIVE);
        ColumnFilter selection = ColumnFilter.selection(RegularAndStaticColumns.of(defA));

        UnfilteredRowIterator iterator = partition.unfilteredIterator(selection, Slices.ALL, false);
        List<Unfiltered> actual = collect(iterator);
        assertEquals(descriptions(rows(0, 1, 2, 3, 4), selection, DeletionTime.LIVE, false), describe(actual));

        // and the other column must really be gone, not just equal to the merge output
        for (Unfiltered unfiltered : actual)
        {
            assertTrue(unfiltered.kind() == Unfiltered.Kind.ROW);
            List<Cell<?>> cells = ImmutableList.copyOf(((Row) unfiltered).cells());
            assertEquals(1, cells.size());
            assertEquals(defA, cells.get(0).column());
        }
    }

    /**
     * A partition tombstone that shadows every row leaves nothing to return, exactly as the merge iterator does.
     */
    @Test
    public void testPartitionDeletionShadowsRows()
    {
        DeletionTime deletion = DeletionTime.build(1000L, FBUtilities.nowInSeconds());
        ImmutableBTreePartition partition = partition(deletion);

        UnfilteredRowIterator iterator = partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false);
        assertFalse(iterator.hasNext());
        assertEquals(descriptions(rows(0, 1, 2, 3, 4), ColumnFilter.all(cfm), deletion, false), describe(collect(iterator)));

        // the deletion time is still reported
        assertEquals(deletion, iterator.partitionLevelDeletion());
    }

    /**
     * A partition that does have range tombstones keeps using the merge iterator, markers included.
     */
    @Test
    public void testRangeTombstonesAreStillMerged()
    {
        long delTime = FBUtilities.nowInSeconds() + 1L;
        RangeTombstone tombstone = new RangeTombstone(Slice.make(BufferClusteringBound.BOTTOM,
                                                                  BufferClusteringBound.inclusiveEndOf(bb(1))),
                                                      DeletionTime.build(delTime * 1000L, delTime));

        PartitionUpdate.Builder update = update();
        for (int i = 0; i < 5; i++)
            update.add(row(i));
        update.add(tombstone);

        ImmutableBTreePartition partition = ImmutableBTreePartition.create(update.build().unfilteredIterator());

        try (UnfilteredRowIterator iterator = partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false))
        {
            assertEquals(ClusteringPrefix.Kind.INCL_START_BOUND, iterator.next().clustering().kind());
            assertEquals(Unfiltered.Kind.RANGE_TOMBSTONE_MARKER, iterator.next().kind());
            for (int i = 2; i < 5; i++)
            {
                Unfiltered next = iterator.next();
                assertEquals(Unfiltered.Kind.ROW, next.kind());
                assertEquals(cfm.comparator.make(i), next.clustering());
            }
            assertFalse(iterator.hasNext());
        }
    }

    private void assertMatchesMerge(UnfilteredRowIterator actual, Iterator<Row> expectedRows, ColumnFilter selection, DeletionTime deletion, boolean reversed)
    {
        assertEquals(descriptions(expectedRows, selection, deletion, reversed), describe(collect(actual)));
    }

    private ImmutableBTreePartition partition(DeletionTime deletion)
    {
        PartitionUpdate.Builder update = update();
        for (int i = 0; i < 5; i++)
            update.add(row(i));
        if (!deletion.isLive())
            update.addPartitionDeletion(deletion);
        return ImmutableBTreePartition.create(update.build().unfilteredIterator());
    }

    private PartitionUpdate.Builder update()
    {
        return new PartitionUpdate.Builder(cfm, dk, cfm.regularAndStaticColumns(), 8);
    }

    /**
     * A names query asks for specific clusterings, in query order.
     */
    private NavigableSet<Clustering<?>> names(int... clusters)
    {
        NavigableSet<Clustering<?>> names = new TreeSet<>(cfm.comparator);
        for (int cluster : clusters)
            names.add(cfm.comparator.make(cluster));
        return names;
    }

    private Row row(int cluster)
    {
        return BTreeRow.singleCellRow(cfm.comparator.make(cluster), makeCell(defA, cluster, 0));
    }

    private Iterator<Row> rows(int... clusters)
    {
        List<Row> rows = new ArrayList<>(clusters.length);
        for (int cluster : clusters)
            rows.add(row(cluster));
        return rows.iterator();
    }

    private List<Unfiltered> collect(UnfilteredRowIterator iterator)
    {
        try (UnfilteredRowIterator it = iterator)
        {
            List<Unfiltered> result = new ArrayList<>();
            it.forEachRemaining(result::add);
            return result;
        }
    }

    private List<String> descriptions(Iterator<Row> rows, ColumnFilter selection, DeletionTime deletion, boolean reversed)
    {
        return describe(collect(merge(rows, selection, deletion, reversed)));
    }


    private List<String> describe(List<Unfiltered> unfiltereds)
    {
        List<String> result = new ArrayList<>(unfiltereds.size());
        for (Unfiltered unfiltered : unfiltereds)
        {
            if (unfiltered.kind() == Unfiltered.Kind.ROW)
                result.add(unfiltered.kind() + " " + unfiltered.clustering() + " " + ((Row) unfiltered).toString(cfm));
            else
                result.add(unfiltered.kind() + " " + unfiltered.clustering());
        }
        return result;
    }

    private List<String> describe(Iterator<Unfiltered> unfiltereds)
    {
        List<Unfiltered> list = new ArrayList<>();
        unfiltereds.forEachRemaining(list::add);
        return describe(list);
    }

    private UnfilteredRowIterator merge(Iterator<Row> rows, ColumnFilter selection, DeletionTime deletion, boolean reversed)
    {
        return new RowAndDeletionMergeIterator(cfm, dk, deletion, selection, Rows.EMPTY_STATIC_ROW, reversed,
                                               EncodingStats.NO_STATS, rows, Collections.emptyIterator(), true);
    }

    private Cell<?> makeCell(ColumnMetadata columnMetadata, int value, long timestamp)
    {
        return BufferCell.live(columnMetadata, timestamp, ((AbstractType) columnMetadata.cellValueType()).decompose(value));
    }

    private static ByteBuffer bb(int i)
    {
        return ByteBufferUtil.bytes(i);
    }
}
