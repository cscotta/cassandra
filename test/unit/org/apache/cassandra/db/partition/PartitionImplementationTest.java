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
package org.apache.cassandra.db.partition;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.Util;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.Clusterable;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.ImmutableBTreePartition;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.RangeTombstoneBoundMarker;
import org.apache.cassandra.db.rows.RangeTombstoneBoundaryMarker;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Row.Deletion;
import org.apache.cassandra.db.rows.RowsOnlyIterator;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PartitionImplementationTest
{
    private static final String KEYSPACE = "PartitionImplementationTest";
    private static final String CF = "Standard";
    private static final String CF_TWO_CLUSTERING = "TwoClustering";

    private static final int ENTRIES = 250;
    private static final int TESTS = 1000;
    private static final int KEY_RANGE = ENTRIES * 5;

    private static final int TIMESTAMP = KEY_RANGE + 1;

    private static TableMetadata metadata;
    private static TableMetadata twoClusteringMetadata;
    private Random rand = new Random(2);

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        SchemaLoader.prepareServer();

        metadata =
            TableMetadata.builder(KEYSPACE, CF)
                         .addPartitionKeyColumn("pk", AsciiType.instance)
                         .addClusteringColumn("ck", AsciiType.instance)
                         .addRegularColumn("col", AsciiType.instance)
                         .addStaticColumn("static_col", AsciiType.instance)
                         .build();

        twoClusteringMetadata =
            TableMetadata.builder(KEYSPACE, CF_TWO_CLUSTERING)
                         .addPartitionKeyColumn("pk", AsciiType.instance)
                         .addClusteringColumn("ck1", AsciiType.instance)
                         .addClusteringColumn("ck2", AsciiType.instance)
                         .addRegularColumn("col", AsciiType.instance)
                         .build();

        SchemaLoader.createKeyspace(KEYSPACE, KeyspaceParams.simple(1), metadata, twoClusteringMetadata);
    }

    public static BufferClusteringBound inclusiveStartOf(ClusteringPrefix<ByteBuffer> prefix)
    {
        ByteBuffer[] values = new ByteBuffer[prefix.size()];
        for (int i = 0; i < prefix.size(); i++)
            values[i] = prefix.get(i);
        return BufferClusteringBound.inclusiveStartOf(values);
    }

    public static BufferClusteringBound exclusiveStartOf(ClusteringPrefix<ByteBuffer> prefix)
    {
        ByteBuffer[] values = new ByteBuffer[prefix.size()];
        for (int i = 0; i < prefix.size(); i++)
            values[i] = prefix.get(i);
        return BufferClusteringBound.exclusiveStartOf(values);
    }

    public static BufferClusteringBound inclusiveEndOf(ClusteringPrefix<ByteBuffer> prefix)
    {
        ByteBuffer[] values = new ByteBuffer[prefix.size()];
        for (int i = 0; i < prefix.size(); i++)
            values[i] = prefix.get(i);
        return BufferClusteringBound.inclusiveEndOf(values);
    }

    private List<Row> generateRows()
    {
        List<Row> content = new ArrayList<>();
        Set<Integer> keysUsed = new HashSet<>();
        for (int i = 0; i < ENTRIES; ++i)
        {
            int rk;
            do
            {
                rk = rand.nextInt(KEY_RANGE);
            }
            while (!keysUsed.add(rk));
            content.add(makeRow(clustering(rk), "Col" + rk));
        }
        return content; // not sorted
    }

    Row makeRow(Clustering<?> clustering, String colValue)
    {
        ColumnMetadata defCol = metadata.getColumn(new ColumnIdentifier("col", true));
        Row.Builder row = BTreeRow.unsortedBuilder();
        row.newRow(clustering);
        row.addCell(BufferCell.live(defCol, TIMESTAMP, ByteBufferUtil.bytes(colValue)));
        return row.build();
    }

    Row makeStaticRow()
    {
        ColumnMetadata defCol = metadata.getColumn(new ColumnIdentifier("static_col", true));
        Row.Builder row = BTreeRow.unsortedBuilder();
        row.newRow(Clustering.STATIC_CLUSTERING);
        row.addCell(BufferCell.live(defCol, TIMESTAMP, ByteBufferUtil.bytes("static value")));
        return row.build();
    }

    private List<Unfiltered> generateMarkersOnly()
    {
        return addMarkers(new ArrayList<>());
    }

    private List<Unfiltered> generateUnfiltereds()
    {
        List<Unfiltered> content = new ArrayList<>(generateRows());
        return addMarkers(content);
    }

    List<Unfiltered> addMarkers(List<Unfiltered> content)
    {
        List<RangeTombstoneMarker> markers = new ArrayList<>();
        Set<Integer> delTimes = new HashSet<>();
        for (int i = 0; i < ENTRIES / 10; ++i)
        {
            int delTime;
            do
            {
                delTime = rand.nextInt(KEY_RANGE);
            }
            while (!delTimes.add(delTime));

            int start = rand.nextInt(KEY_RANGE);
            DeletionTime dt = DeletionTime.build(delTime, delTime);
            RangeTombstoneMarker open = RangeTombstoneBoundMarker.inclusiveOpen(false, clustering(start), dt);
            int end = start + rand.nextInt((KEY_RANGE - start) / 4 + 1);
            RangeTombstoneMarker close = RangeTombstoneBoundMarker.inclusiveClose(false, clustering(end), dt);
            markers.add(open);
            markers.add(close);
        }
        markers.sort(metadata.comparator);

        RangeTombstoneMarker toAdd = null;
        Set<DeletionTime> open = new HashSet<>();
        DeletionTime current = DeletionTime.LIVE;
        for (RangeTombstoneMarker marker : markers)
        {
            if (marker.isOpen(false))
            {
                DeletionTime delTime = marker.openDeletionTime(false);
                open.add(delTime);
                if (delTime.supersedes(current))
                {
                    if (toAdd != null)
                    {
                        if (metadata.comparator.compare(toAdd, marker) != 0)
                            content.add(toAdd);
                        else
                        {
                            // gotta join
                            current = toAdd.isClose(false) ? toAdd.closeDeletionTime(false) : DeletionTime.LIVE;
                        }
                    }
                    if (current != DeletionTime.LIVE)
                        marker = RangeTombstoneBoundaryMarker.makeBoundary(false, marker.openBound(false).invert(), marker.openBound(false), current, delTime);
                    toAdd = marker;
                    current = delTime;
                }
            }
            else
            {
                assert marker.isClose(false);
                DeletionTime delTime = marker.closeDeletionTime(false);
                boolean removed = open.remove(delTime);
                assert removed;
                if (current.equals(delTime))
                {
                    if (toAdd != null)
                    {
                        if (metadata.comparator.compare(toAdd, marker) != 0)
                            content.add(toAdd);
                        else
                        {
                            // gotta join
                            current = toAdd.closeDeletionTime(false);
                            marker = new RangeTombstoneBoundMarker(marker.closeBound(false), current);
                        }
                    }
                    DeletionTime best = open.stream().max(DeletionTime::compareTo).orElse(DeletionTime.LIVE);
                    if (best != DeletionTime.LIVE)
                        marker = RangeTombstoneBoundaryMarker.makeBoundary(false, marker.closeBound(false), marker.closeBound(false).invert(), current, best);
                    toAdd = marker;
                    current = best;
                }
            }
        }
        content.add(toAdd);
        assert current == DeletionTime.LIVE;
        assert open.isEmpty();
        return content;
    }

    private Clustering<ByteBuffer> clustering(int i)
    {
        return (Clustering<ByteBuffer>) metadata.comparator.make(String.format("Row%06d", i));
    }

    private void test(Supplier<Collection<? extends Unfiltered>> content, Row staticRow)
    {
        for (int i = 0; i<TESTS; ++i)
        {
            try
            {
                rand = new Random(i);
                testIter(content, staticRow);
            }
            catch (Throwable t)
            {
                throw new AssertionError("Test failed with seed " + i, t);
            }
        }
    }

    private void testIter(Supplier<Collection<? extends Unfiltered>> contentSupplier, Row staticRow)
    {
        NavigableSet<Clusterable> sortedContent = new TreeSet<Clusterable>(metadata.comparator);
        sortedContent.addAll(contentSupplier.get());
        AbstractBTreePartition partition;
        try (UnfilteredRowIterator iter = new Util.UnfilteredSource(metadata, Util.dk("pk"), staticRow, sortedContent.stream().map(x -> (Unfiltered) x).iterator()))
        {
            partition = ImmutableBTreePartition.create(iter);
        }

        ColumnMetadata defCol = metadata.getColumn(new ColumnIdentifier("col", true));
        ColumnFilter cf = ColumnFilter.selectionBuilder().add(defCol).build();
        Function<? super Clusterable, ? extends Clusterable> colFilter = x -> x instanceof Row ? ((Row) x).filter(cf, metadata) : x;
        Slices slices = Slices.with(metadata.comparator, Slice.make(clustering(KEY_RANGE / 4), clustering(KEY_RANGE * 3 / 4)));
        Slices multiSlices = makeSlices();

        // lastRow
        assertRowsEqual((Row) get(sortedContent.descendingSet(), x -> x instanceof Row),
                        partition.lastRow());
        // get(static)
        assertRowsEqual(staticRow,
                        partition.getRow(Clustering.STATIC_CLUSTERING));

        // get
        for (int i=0; i < KEY_RANGE; ++i)
        {
            Clustering<?> cl = clustering(i);
            assertRowsEqual(getRow(sortedContent, cl),
                            partition.getRow(cl));
        }
        // isEmpty
        assertEquals(sortedContent.isEmpty() && staticRow == null,
                     partition.isEmpty());
        // hasRows
        assertEquals(sortedContent.stream().anyMatch(x -> x instanceof Row),
                     partition.hasRows());

        // iterator
        assertIteratorsEqual(sortedContent.stream().filter(x -> x instanceof Row).iterator(),
                             partition.iterator());

        // unfiltered iterator
        assertIteratorsEqual(sortedContent.iterator(),
                             partition.unfilteredIterator());

        // unfiltered iterator
        assertIteratorsEqual(sortedContent.iterator(),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), Slices.ALL, false));
        // column-filtered
        assertIteratorsEqual(sortedContent.stream().map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, Slices.ALL, false));
        // sliced
        assertIteratorsEqual(slice(sortedContent, slices.get(0)),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), slices, false));
        assertIteratorsEqual(streamOf(slice(sortedContent, slices.get(0))).map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, slices, false));
        // single clustering slices, as produced by a names query
        testPointSlices(sortedContent, partition, cf, colFilter);
        // randomly multi-sliced
        assertIteratorsEqual(slice(sortedContent, multiSlices),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), multiSlices, false));
        assertIteratorsEqual(streamOf(slice(sortedContent, multiSlices)).map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, multiSlices, false));
        // reversed
        assertIteratorsEqual(sortedContent.descendingIterator(),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), Slices.ALL, true));
        assertIteratorsEqual(sortedContent.descendingSet().stream().map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, Slices.ALL, true));
        assertIteratorsEqual(invert(slice(sortedContent, slices.get(0))),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), slices, true));
        assertIteratorsEqual(streamOf(invert(slice(sortedContent, slices.get(0)))).map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, slices, true));
        assertIteratorsEqual(invert(slice(sortedContent, multiSlices)),
                             partition.unfilteredIterator(ColumnFilter.all(metadata), multiSlices, true));
        assertIteratorsEqual(streamOf(invert(slice(sortedContent, multiSlices))).map(colFilter).iterator(),
                             partition.unfilteredIterator(cf, multiSlices, true));

        // clustering iterator
        testClusteringsIterator(sortedContent, partition, ColumnFilter.all(metadata), false);
        testClusteringsIterator(sortedContent, partition, cf, false);
        testClusteringsIterator(sortedContent, partition, ColumnFilter.all(metadata), true);
        testClusteringsIterator(sortedContent, partition, cf, true);

        // sliceable iter
        testSlicingOfIterators(sortedContent, partition, ColumnFilter.all(metadata), false);
        testSlicingOfIterators(sortedContent, partition, cf, false);
        testSlicingOfIterators(sortedContent, partition, ColumnFilter.all(metadata), true);
        testSlicingOfIterators(sortedContent, partition, cf, true);
    }

    /**
     * A names query asks for exact clusterings, i.e. slices whose start and end bound hold that single clustering
     * ({@link Slice#make(Clustering)}). Iterating them must return exactly the requested row, in both directions,
     * whether or not that row exists.
     */
    private void testPointSlices(NavigableSet<Clusterable> sortedContent,
                                 Partition partition,
                                 ColumnFilter cf,
                                 Function<? super Clusterable, ? extends Clusterable> colFilter)
    {
        for (int i = 0; i < KEY_RANGE; i += 13)
        {
            Slice point = Slice.make(clustering(i));
            Slices pointSlice = Slices.with(metadata.comparator, point);

            assertIteratorsEqual(slice(sortedContent, point),
                                 partition.unfilteredIterator(ColumnFilter.all(metadata), pointSlice, false));
            assertIteratorsEqual(streamOf(slice(sortedContent, point)).map(colFilter).iterator(),
                                 partition.unfilteredIterator(cf, pointSlice, false));
            assertIteratorsEqual(invert(slice(sortedContent, point)),
                                 partition.unfilteredIterator(ColumnFilter.all(metadata), pointSlice, true));
        }
    }

    /**
     * The single clustering fast path compares the components of the two bounds, and must not treat a slice on a
     * prefix of the clustering columns as a single clustering. Both cases are exercised here on a table with two
     * clustering columns, against a reference built from the slice bounds alone.
     */
    @Test
    public void testSliceShapesWithTwoClusteringColumns()
    {
        ClusteringComparator comparator = twoClusteringMetadata.comparator;
        ColumnMetadata defCol = twoClusteringMetadata.getColumn(new ColumnIdentifier("col", true));
        List<Unfiltered> rows = new ArrayList<>();
        for (String[] values : new String[][]{ { "a", "a" }, { "a", "b" }, { "b", "a" }, { "c", "c" } })
        {
            Row.Builder row = BTreeRow.unsortedBuilder();
            row.newRow(comparator.make((Object[]) values));
            row.addCell(BufferCell.live(defCol, TIMESTAMP, ByteBufferUtil.bytes(values[0] + values[1])));
            rows.add(row.build());
        }

        AbstractBTreePartition partition;
        try (UnfilteredRowIterator iter = new Util.UnfilteredSource(twoClusteringMetadata, Util.dk("pk"), null, rows.iterator()))
        {
            partition = ImmutableBTreePartition.create(iter);
        }

        Slice prefixOfA = Slice.make(BufferClusteringBound.inclusiveStartOf(ByteBufferUtil.bytes("a")),
                                     BufferClusteringBound.inclusiveEndOf(ByteBufferUtil.bytes("a")));

        List<Slice> slices = new ArrayList<>();
        slices.add(Slice.make(comparator.make("a", "a")));     // first row, a full clustering
        slices.add(Slice.make(comparator.make("a", "b")));     // last row of the "a" prefix
        slices.add(Slice.make(comparator.make("b", "a")));
        slices.add(Slice.make(comparator.make("b", "b")));     // absent clustering
        slices.add(Slice.make(comparator.make("a", "a"), comparator.make("b", "a")));
        slices.add(prefixOfA);                                   // prefix slice, spans two rows
        slices.add(Slice.ALL);

        for (Slice slice : slices)
        {
            List<Unfiltered> expected = new ArrayList<>();
            for (Unfiltered row : rows)
            {
                if (slice.includes(comparator, row.clustering()))
                    expected.add(row);
            }

            for (boolean reversed : new boolean[]{ false, true })
            {
                List<Unfiltered> ordered = new ArrayList<>(expected);
                if (reversed)
                    Collections.reverse(ordered);
                try (UnfilteredRowIterator iter = partition.unfilteredIterator(ColumnFilter.all(twoClusteringMetadata),
                                                                               Slices.with(comparator, slice),
                                                                               reversed))
                {
                    assertIteratorsEqual(ordered.iterator(), iter);
                }
            }
        }
    }

    private void testClusteringsIterator(NavigableSet<Clusterable> sortedContent, Partition partition, ColumnFilter cf, boolean reversed)
    {
        Function<? super Clusterable, ? extends Clusterable> colFilter = x -> x instanceof Row ? ((Row) x).filter(cf, metadata) : x;
        NavigableSet<Clustering<?>> clusteringsInQueryOrder = makeClusterings(reversed);

        // fetch each clustering in turn
        for (Clustering clustering : clusteringsInQueryOrder)
        {
            NavigableSet<Clustering<?>> single = new TreeSet<>(metadata.comparator);
            single.add(clustering);
            try (UnfilteredRowIterator slicedIter = partition.unfilteredIterator(cf, single, reversed))
            {
                assertIteratorsEqual(streamOf(directed(slice(sortedContent, Slice.make(clustering)), reversed)).map(colFilter).iterator(),
                                     slicedIter);
            }
        }

        // Fetch all slices at once
        try (UnfilteredRowIterator slicedIter = partition.unfilteredIterator(cf, clusteringsInQueryOrder, reversed))
        {
            List<Iterator<? extends Clusterable>> clusterableIterators = new ArrayList<>();
            clusteringsInQueryOrder.forEach(clustering -> clusterableIterators.add(directed(slice(sortedContent, Slice.make(clustering)), reversed)));

            assertIteratorsEqual(Iterators.concat(clusterableIterators.toArray(new Iterator[0])), slicedIter);
        }
    }

    private Slices makeSlices()
    {
        int pos = 0;
        Slices.Builder builder = new Slices.Builder(metadata.comparator);
        while (pos <= KEY_RANGE)
        {
            int skip = rand.nextInt(KEY_RANGE / 10) * (rand.nextInt(3) + 2 / 3); // increased chance of getting 0
            pos += skip;
            int sz = rand.nextInt(KEY_RANGE / 10) + (skip == 0 ? 1 : 0);    // if start is exclusive need at least sz 1
            Clustering<ByteBuffer> start = clustering(pos);
            pos += sz;
            Clustering<ByteBuffer> end = clustering(pos);
            Slice slice = Slice.make(skip == 0 ? exclusiveStartOf(start) : inclusiveStartOf(start), inclusiveEndOf(end));
            builder.add(slice);
        }
        return builder.build();
    }

    private NavigableSet<Clustering<?>> makeClusterings(boolean reversed)
    {
        int pos = 0;
        NavigableSet<Clustering<?>> clusterings = new TreeSet<>(reversed ? metadata.comparator.reversed() : metadata.comparator);
        while (pos <= KEY_RANGE)
        {
            int skip = rand.nextInt(KEY_RANGE / 10) * (rand.nextInt(3) + 2 / 3); // increased chance of getting 0
            pos += skip;
            clusterings.add(clustering(pos));
        }
        return clusterings;
    }

    /**
     * A names query that names a single clustering is served by one lookup in the partition's tree instead of an
     * iterator over the requested clusterings. It must return exactly what that clustering addresses: the row with
     * the column selection applied, merged with the range tombstones that cover it, and nothing at all for a
     * clustering the partition does not hold.
     */
    @Test
    public void testSingleClusteringNamesQuery()
    {
        testSingleClusteringNamesQuery(false);
    }

    /**
     * The same queries on a partition that holds range tombstones, so a query for a covered clustering has to merge
     * the deletion with the row rather than just return it.
     */
    @Test
    public void testSingleClusteringNamesQueryWithTombstones()
    {
        testSingleClusteringNamesQuery(true);
    }

    /**
     * A response partition is built from the rows of a read. A partition that holds exactly one row is built as a
     * singleton tree instead of through a btree builder, so pin the content of the partitions that are built at the
     * boundary of that fast path: nothing, one row, two rows, and one row next to range tombstone markers.
     */
    @Test
    public void testBuildResponsePartition()
    {
        ClusteringComparator comparator = twoClusteringMetadata.comparator;
        ColumnMetadata defCol = twoClusteringMetadata.getColumn(new ColumnIdentifier("col", true));

        Row rowA = row(comparator, defCol, "a", "a");
        Row rowB = row(comparator, defCol, "a", "b");
        DeletionTime deletion = DeletionTime.build(TIMESTAMP, TIMESTAMP);

        List<List<Unfiltered>> contents = new ArrayList<>();
        contents.add(Collections.emptyList());
        contents.add(Collections.singletonList(rowA));
        contents.add(Arrays.asList(rowA, rowB));
        contents.add(Arrays.asList(rowA,
                                    RangeTombstoneBoundMarker.inclusiveOpen(false, comparator.make("a", "a0"), deletion),
                                    rowB,
                                    RangeTombstoneBoundMarker.inclusiveClose(false, comparator.make("a", "c0"), deletion)));

        for (List<Unfiltered> content : contents)
        {
            List<Unfiltered> sorted = new ArrayList<>(content);
            sorted.sort(comparator);

            AbstractBTreePartition partition;
            try (UnfilteredRowIterator iter = new Util.UnfilteredSource(twoClusteringMetadata, Util.dk("pk"), null, sorted.iterator()))
            {
                partition = ImmutableBTreePartition.create(iter);
            }

            try (UnfilteredRowIterator expected = new Util.UnfilteredSource(twoClusteringMetadata, Util.dk("pk"), null, sorted.iterator()))
            {
                assertIteratorsEqual(partition.unfilteredIterator(), expected, twoClusteringMetadata);
            }
        }
    }

    private static Row row(ClusteringComparator comparator, ColumnMetadata defCol, String c1, String c2)
    {
        Row.Builder builder = BTreeRow.unsortedBuilder();
        builder.newRow(comparator.make(c1, c2));
        builder.addCell(BufferCell.live(defCol, TIMESTAMP, ByteBufferUtil.bytes(c1 + c2)));
        return builder.build();
    }

    private void testSingleClusteringNamesQuery(boolean withTombstones)
    {
        ClusteringComparator comparator = twoClusteringMetadata.comparator;
        ColumnMetadata defCol = twoClusteringMetadata.getColumn(new ColumnIdentifier("col", true));

        List<Unfiltered> content = new ArrayList<>();
        for (String[] values : new String[][]{ { "a", "a" }, { "a", "b" }, { "a", "c" }, { "c", "a" }, { "c", "c" } })
        {
            Row.Builder row = BTreeRow.unsortedBuilder();
            row.newRow(comparator.make((Object[]) values));
            row.addCell(BufferCell.live(defCol, TIMESTAMP, ByteBufferUtil.bytes(values[0] + values[1])));
            content.add(row.build());
        }

        if (withTombstones)
        {
            // Strictly covers the (a, b) and (a, c) rows; neither bound is a clustering the queries below ask for,
            // so the boundaries themselves do not decide the expected markers.
            DeletionTime deletion = DeletionTime.build(TIMESTAMP, TIMESTAMP);
            content.add(RangeTombstoneBoundMarker.inclusiveOpen(false, comparator.make("a", "b0"), deletion));
            content.add(RangeTombstoneBoundMarker.inclusiveClose(false, comparator.make("a", "c0"), deletion));
        }
        content.sort(comparator);

        AbstractBTreePartition partition;
        try (UnfilteredRowIterator iter = new Util.UnfilteredSource(twoClusteringMetadata, Util.dk("pk"), null, content.iterator()))
        {
            partition = ImmutableBTreePartition.create(iter);
        }

        NavigableSet<Clusterable> sortedContent = new TreeSet<>(comparator);
        sortedContent.addAll(content);

        ColumnFilter cf = ColumnFilter.selectionBuilder().add(defCol).build();
        Function<? super Clusterable, ? extends Clusterable> colFilter = x -> x instanceof Row ? ((Row) x).filter(cf, twoClusteringMetadata) : x;
        Clustering<?> absent = comparator.make("b", "b");

        String[][] probes = { { "a", "a" }, { "a", "b" }, { "a", "c" }, { "a", "d" }, { "b", "a" },
                              { "c", "a" }, { "c", "b" }, { "c", "c" }, { "d", "a" } };
        for (boolean reversed : new boolean[]{ false, true })
        {
            for (String[] values : probes)
            {
                Clustering<?> clustering = comparator.make((Object[]) values);
                // A reversed names query is given its clusterings in the order it iterates them, like
                // ClusteringIndexNamesFilter does.
                NavigableSet<Clustering<?>> single = new TreeSet<>(reversed ? comparator.reversed() : comparator);
                single.add(clustering);
                // The same clustering requested together with one the partition does not hold, which keeps the
                // general path, so the two paths are also compared against each other.
                NavigableSet<Clustering<?>> withAbsent = new TreeSet<>(reversed ? comparator.reversed() : comparator);
                withAbsent.add(clustering);
                withAbsent.add(absent);

                try (UnfilteredRowIterator iter = partition.unfilteredIterator(cf, single, reversed))
                {
                    // Pin the fast path this test exists for: with no range tombstone to merge, a names query for
                    // one clustering returns the rows-only iterator, not the general ClusteringsIterator.
                    if (!withTombstones)
                        assertTrue("expected the rows-only iterator, got " + iter.getClass().getName(),
                                   iter instanceof RowsOnlyIterator);
                    assertIteratorsEqual(streamOf(directed(slice(sortedContent, Slice.make(clustering)), reversed)).map(colFilter).iterator(),
                                         iter, twoClusteringMetadata);
                }
                try (UnfilteredRowIterator iter = partition.unfilteredIterator(ColumnFilter.all(twoClusteringMetadata), single, reversed))
                {
                    assertIteratorsEqual(directed(slice(sortedContent, Slice.make(clustering)), reversed), iter, twoClusteringMetadata);
                }
                try (UnfilteredRowIterator iter = partition.unfilteredIterator(cf, withAbsent, reversed))
                {
                    assertIteratorsEqual(streamOf(directed(slice(sortedContent, Slice.make(clustering)), reversed)).map(colFilter).iterator(),
                                         iter, twoClusteringMetadata);
                }
            }
        }
    }

    private void testSlicingOfIterators(NavigableSet<Clusterable> sortedContent, AbstractBTreePartition partition, ColumnFilter cf, boolean reversed)
    {
        Function<? super Clusterable, ? extends Clusterable> colFilter = x -> x instanceof Row ? ((Row) x).filter(cf, metadata) : x;
        Slices slices = makeSlices();

        // fetch each slice in turn
        for (Slice slice : (Iterable<Slice>) () -> directed(slices, reversed))
        {
            try (UnfilteredRowIterator slicedIter = partition.unfilteredIterator(cf, Slices.with(metadata.comparator, slice), reversed))
            {
                assertIteratorsEqual(streamOf(directed(slice(sortedContent, slice), reversed)).map(colFilter).iterator(),
                                     slicedIter);
            }
        }

        // Fetch all slices at once
        try (UnfilteredRowIterator slicedIter = partition.unfilteredIterator(cf, slices, reversed))
        {
            List<Iterator<? extends Clusterable>> slicelist = new ArrayList<>();
            slices.forEach(slice -> slicelist.add(directed(slice(sortedContent, slice), reversed)));
            if (reversed)
                Collections.reverse(slicelist);

            assertIteratorsEqual(Iterators.concat(slicelist.toArray(new Iterator[0])), slicedIter);
        }
    }

    private<T> Iterator<T> invert(Iterator<T> slice)
    {
        Deque<T> dest = new LinkedList<>();
        Iterators.addAll(dest, slice);
        return dest.descendingIterator();
    }

    private Iterator<Clusterable> slice(NavigableSet<Clusterable> sortedContent, Slices slices)
    {
        return Iterators.concat(streamOf(slices).map(slice -> slice(sortedContent, slice)).iterator());
    }

    private Iterator<Clusterable> slice(NavigableSet<Clusterable> sortedContent, Slice slice)
    {
        // Slice bounds are inclusive bounds, equal only to markers. Matched markers should be returned as one-sided boundaries.
        RangeTombstoneMarker prev = (RangeTombstoneMarker) sortedContent.headSet(slice.start(), true).descendingSet().stream().filter(x -> x instanceof RangeTombstoneMarker).findFirst().orElse(null);
        RangeTombstoneMarker next = (RangeTombstoneMarker) sortedContent.tailSet(slice.end(), true).stream().filter(x -> x instanceof RangeTombstoneMarker).findFirst().orElse(null);
        Iterator<Clusterable> result = sortedContent.subSet(slice.start(), false, slice.end(), false).iterator();
        if (prev != null && prev.isOpen(false))
            result = Iterators.concat(Iterators.singletonIterator(new RangeTombstoneBoundMarker(slice.start(), prev.openDeletionTime(false))), result);
        if (next != null && next.isClose(false))
            result = Iterators.concat(result, Iterators.singletonIterator(new RangeTombstoneBoundMarker(slice.end(), next.closeDeletionTime(false))));
        return result;
    }

    private Iterator<Slice> directed(Slices slices, boolean reversed)
    {
        return directed(slices.iterator(), reversed);
    }

    private <T> Iterator<T> directed(Iterator<T> iter, boolean reversed)
    {
        if (!reversed)
            return iter;
        return invert(iter);
    }

    private <T> Stream<T> streamOf(Iterator<T> iterator)
    {
        Iterable<T> iterable = () -> iterator;
        return streamOf(iterable);
    }

    <T> Stream<T> streamOf(Iterable<T> iterable)
    {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private void assertIteratorsEqual(Iterator<? extends Clusterable> it1, Iterator<? extends Clusterable> it2)
    {
        assertIteratorsEqual(it1, it2, metadata);
    }

    private void assertIteratorsEqual(Iterator<? extends Clusterable> it1, Iterator<? extends Clusterable> it2, TableMetadata table)
    {
        Clusterable[] a1 = (Clusterable[]) Iterators.toArray(it1, Clusterable.class);
        Clusterable[] a2 = (Clusterable[]) Iterators.toArray(it2, Clusterable.class);
        if (Arrays.equals(a1, a2))
            return;
        String a1s = Stream.of(a1).map(x -> "\n" + (x instanceof Unfiltered ? ((Unfiltered) x).toString(table) : x.toString())).collect(Collectors.toList()).toString();
        String a2s = Stream.of(a2).map(x -> "\n" + (x instanceof Unfiltered ? ((Unfiltered) x).toString(table) : x.toString())).collect(Collectors.toList()).toString();
        assertArrayEquals("Arrays differ. Expected " + a1s + " was " + a2s, a1, a2);
    }

    private Row getRow(NavigableSet<Clusterable> sortedContent, Clustering<?> cl)
    {
        NavigableSet<Clusterable> nexts = sortedContent.tailSet(cl, true);
        if (nexts.isEmpty())
            return null;
        Row row = nexts.first() instanceof Row && metadata.comparator.compare(cl, nexts.first()) == 0 ? (Row) nexts.first() : null;
        for (Clusterable next : nexts)
            if (next instanceof RangeTombstoneMarker)
            {
                RangeTombstoneMarker rt = (RangeTombstoneMarker) next;
                if (!rt.isClose(false))
                    return row;
                DeletionTime delTime = rt.closeDeletionTime(false);
                return row == null ? BTreeRow.emptyDeletedRow(cl, Deletion.regular(delTime)) : row.filter(ColumnFilter.all(metadata), delTime, true, metadata);
            }
        return row;
    }

    private void assertRowsEqual(Row expected, Row actual)
    {
        try
        {
            assertEquals(expected == null, actual == null);
            if (expected == null)
                return;
            assertEquals(expected.clustering(), actual.clustering());
            assertEquals(expected.deletion(), actual.deletion());
            assertArrayEquals(Iterables.toArray(expected.cells(), Cell.class), Iterables.toArray(expected.cells(), Cell.class));
        } catch (Throwable t)
        {
            throw new AssertionError(String.format("Row comparison failed, expected %s got %s", expected, actual), t);
        }
    }

    private static<T> T get(NavigableSet<T> sortedContent, Predicate<T> test)
    {
        return sortedContent.stream().filter(test).findFirst().orElse(null);
    }

    @Test
    public void testEmpty()
    {
        test(() -> Collections.<Row>emptyList(), null);
    }

    @Test
    public void testStaticOnly()
    {
        test(() -> Collections.<Row>emptyList(), makeStaticRow());
    }

    @Test
    public void testRows()
    {
        test(this::generateRows, null);
    }

    @Test
    public void testRowsWithStatic()
    {
        test(this::generateRows, makeStaticRow());
    }

    @Test
    public void testMarkersOnly()
    {
        test(this::generateMarkersOnly, null);
    }

    @Test
    public void testMarkersWithStatic()
    {
        test(this::generateMarkersOnly, makeStaticRow());
    }

    @Test
    public void testUnfiltereds()
    {
        test(this::generateUnfiltereds, makeStaticRow());
    }

}
