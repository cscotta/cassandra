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
import org.apache.cassandra.db.partitions.Partition;
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
 * Checks that the iterator a partition without range tombstones uses ({@link RowsOnlyIterator}) yields exactly what
 * {@link RowAndDeletionMergeIterator} yields with an empty range tombstone source: the same rows, filtered by the
 * column selection, and by the partition deletion when the partition may hold data that deletion shadows.
 */
public class RowsOnlyIteratorTest
{
    private static final String KEYSPACE1 = "RowsOnlyIteratorTest";
    private static final String CF_STANDARD1 = "Standard1";

    private DecoratedKey dk;
    private TableMetadata cfm;
    private ColumnMetadata defA;

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
    }

    @Test
    public void testWholePartition()
    {
        Partition partition = partition(true);

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false),
                           rows(0, 1, 2, 3, 4), ColumnFilter.all(cfm), DeletionTime.LIVE, false, true);

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, true),
                           rows(4, 3, 2, 1, 0), ColumnFilter.all(cfm), DeletionTime.LIVE, true, true);
    }

    @Test
    public void testSingleRowSlice()
    {
        Partition partition = partition(true);
        Slices slice = Slices.with(cfm.comparator, Slice.make(cfm.comparator.make(2)));

        assertMatchesMerge(partition.unfilteredIterator(ColumnFilter.all(cfm), slice, false),
                           rows(2), ColumnFilter.all(cfm), DeletionTime.LIVE, false, true);
    }

    @Test
    public void testRowsByName()
    {
        Partition partition = partition(true);

        assertEquals(descriptions(rows(1, 3), ColumnFilter.all(cfm), DeletionTime.LIVE, false, true),
                     describe(partition.unfilteredIterator(ColumnFilter.all(cfm), names(1, 3), false)));
    }

    @Test
    public void testMissingRowByName()
    {
        Partition partition = partition(true);

        Iterator<Unfiltered> actual = partition.unfilteredIterator(ColumnFilter.all(cfm), names(42), false);
        assertFalse(actual.hasNext());
    }

    /**
     * The column selection is applied by the merge iterator per row; the rows-only iterator has to apply it too.
     */
    @Test
    public void testColumnSelection()
    {
        Partition partition = partition(true);
        ColumnFilter selection = ColumnFilter.selection(RegularAndStaticColumns.of(defA));

        List<Unfiltered> actual = collect(partition.unfilteredIterator(selection, Slices.ALL, false));
        assertEquals(descriptions(rows(0, 1, 2, 3, 4), selection, DeletionTime.LIVE, false, true), describe(actual));

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
     * A partition that may hold shadowed data loses the rows and cells its partition deletion shadows, exactly as the
     * merge iterator does.
     */
    @Test
    public void testPartitionDeletionShadowsRowsOfAPartitionThatMayHoldShadowedData()
    {
        DeletionTime deletion = DeletionTime.build(1000L, FBUtilities.nowInSeconds());
        Partition partition = partition(true, deletion, 0L);

        UnfilteredRowIterator iterator = partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false);
        assertEquals(descriptions(rows(0, 1, 2, 3, 4), ColumnFilter.all(cfm), deletion, false, true), describe(collect(iterator)));
        assertFalse(iterator.hasNext());
        assertEquals(deletion, iterator.partitionLevelDeletion());
    }

    /**
     * Only the cells the deletion does not shadow stay, and the fully shadowed rows are skipped, not returned as null.
     */
    @Test
    public void testPartitionDeletionKeepsTheCellsItDoesNotShadow()
    {
        DeletionTime deletion = DeletionTime.build(1000L, FBUtilities.nowInSeconds());
        PartitionUpdate.Builder update = update(true, deletion);
        update.add(BTreeRow.singleCellRow(cfm.comparator.make(0), makeCell(defA, 0, 5000L)));
        for (int i = 1; i < 5; i++)
            update.add(row(i));

        Partition partition = update.build();
        try (UnfilteredRowIterator iterator = partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false))
        {
            assertTrue(iterator.hasNext());
            Unfiltered next = iterator.next();
            assertEquals(cfm.comparator.make(0), next.clustering());
            List<Cell<?>> cells = ImmutableList.copyOf(((Row) next).cells());
            assertEquals(1, cells.size());
            assertEquals(5000L, cells.get(0).timestamp());
            assertFalse(iterator.hasNext());
        }
    }

    /**
     * A partition that cannot hold shadowed data keeps its cells: the merge iterator does not apply the partition
     * deletion to such partitions either, so the rows-only iterator must not either.
     */
    @Test
    public void testPartitionDeletionIsNotAppliedToAPartitionThatCannotHoldShadowedData()
    {
        DeletionTime deletion = DeletionTime.build(1000L, FBUtilities.nowInSeconds());
        Partition partition = partition(false, deletion, 0L);

        List<Unfiltered> actual = collect(partition.unfilteredIterator(ColumnFilter.all(cfm), Slices.ALL, false));
        assertEquals(descriptions(rows(0, 1, 2, 3, 4), ColumnFilter.all(cfm), deletion, false, false), describe(actual));
        assertEquals(5, actual.size());
        for (Unfiltered unfiltered : actual)
        {
            Row row = (Row) unfiltered;
            assertEquals(1, ImmutableList.copyOf(row.cells()).size());
            assertEquals(0L, row.cells().iterator().next().timestamp());
        }
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

        PartitionUpdate.Builder update = update(true);
        for (int i = 0; i < 5; i++)
            update.add(row(i));
        update.add(tombstone);

        Partition partition = update.build();
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

    private void assertMatchesMerge(UnfilteredRowIterator actual, Iterator<Row> expectedRows, ColumnFilter selection,
                                    DeletionTime deletion, boolean reversed, boolean removeShadowedData)
    {
        assertEquals(descriptions(expectedRows, selection, deletion, reversed, removeShadowedData), describe(collect(actual)));
    }

    /**
     * A partition as a memtable or an sstable-backed partition presents it: it may hold data shadowed by its
     * partition deletion.
     */
    private Partition partition(boolean canHaveShadowedData)
    {
        return partition(canHaveShadowedData, DeletionTime.LIVE, 0L);
    }

    private Partition partition(boolean canHaveShadowedData, DeletionTime deletion, long timestamp)
    {
        PartitionUpdate.Builder update = update(canHaveShadowedData, deletion);
        for (int i = 0; i < 5; i++)
            update.add(BTreeRow.singleCellRow(cfm.comparator.make(i), makeCell(defA, i, timestamp)));
        return update.build();
    }

    private PartitionUpdate.Builder update(boolean canHaveShadowedData)
    {
        return update(canHaveShadowedData, DeletionTime.LIVE);
    }

    private PartitionUpdate.Builder update(boolean canHaveShadowedData, DeletionTime deletion)
    {
        PartitionUpdate.Builder update = new PartitionUpdate.Builder(cfm, dk, cfm.regularAndStaticColumns(), 8, canHaveShadowedData);
        if (!deletion.isLive())
            update.addPartitionDeletion(deletion);
        return update;
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

    private List<String> descriptions(Iterator<Row> rows, ColumnFilter selection, DeletionTime deletion,
                                      boolean reversed, boolean removeShadowedData)
    {
        return describe(collect(merge(rows, selection, deletion, reversed, removeShadowedData)));
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

    private UnfilteredRowIterator merge(Iterator<Row> rows, ColumnFilter selection, DeletionTime deletion,
                                        boolean reversed, boolean removeShadowedData)
    {
        return new RowAndDeletionMergeIterator(cfm, dk, deletion, selection, Rows.EMPTY_STATIC_ROW, reversed,
                                               EncodingStats.NO_STATS, rows, Collections.emptyIterator(), removeShadowedData);
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
