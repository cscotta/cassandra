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

package org.apache.cassandra.db.filter;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NavigableSet;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.btree.BTreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ClusteringIndexNamesFilterTest
{
    private static final String KEYSPACE = "ks";
    private static final String TABLE = "names_filter_slices";

    private static TableMetadata metadata;
    private static ClusteringComparator comparator;

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        DatabaseDescriptor.daemonInitialization();
        metadata = tableMetadata();
        comparator = metadata.comparator;
    }

    /**
     * Every source of a read (each sstable's lower bound iterator and each memtable iterator) asks the filter for
     * its slices, so they are built once and shared; {@link Slices} is immutable.
     */
    @Test
    public void testSlicesAreBuiltOncePerFilter()
    {
        ClusteringIndexNamesFilter filter = filter(1, 2, 3);

        Slices slices = filter.getSlices(metadata);
        assertSlices(slices, 1, 2, 3);
        assertSame(slices, filter.getSlices(metadata));
        assertSame("a second read of the same filter must not rebuild them", slices, filter.getSlices(metadata));
    }

    /**
     * The cache must not serve slices built for another table comparator, e.g. after a schema change.
     */
    @Test
    public void testSlicesAreRebuiltForDifferentTableMetadata()
    {
        ClusteringIndexNamesFilter filter = filter(1, 2, 3);
        Slices first = filter.getSlices(metadata);

        TableMetadata otherTable = tableMetadata();
        assertNotSame(metadata.comparator, otherTable.comparator);

        Slices second = filter.getSlices(otherTable);
        assertNotSame(first, second);
        assertSlices(second, 1, 2, 3);
        assertSame(second, filter.getSlices(otherTable));

        // the cache is keyed on the comparator, so the original table rebuilds instead of serving the other's
        Slices third = filter.getSlices(metadata);
        assertNotSame(second, third);
        assertSlices(third, 1, 2, 3);
    }

    @Test
    public void testCachedSlicesAreNotSharedWithPagingFilter()
    {
        ClusteringIndexNamesFilter filter = filter(1, 2, 3);
        Slices all = filter.getSlices(metadata);

        ClusteringIndexNamesFilter paged = filter.forPaging(comparator, clustering(2), true);
        Slices pagedSlices = paged.getSlices(metadata);
        assertNotSame(all, pagedSlices);
        // a non-reversed filter pages forward: the remaining rows start at the last returned one
        assertSlices(pagedSlices, 2, 3);
        assertSlices(all, 1, 2, 3);
        assertSame(pagedSlices, paged.getSlices(metadata));
    }

    @Test
    public void testReversedFilterCachesTheSlicesOfItsQueryOrder()
    {
        ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(names(1, 2, 3), true);
        Slices slices = filter.getSlices(metadata);
        // reversed names are normalized to clustering order by Slices.Builder
        assertSlices(slices, 1, 2, 3);
        assertSame(slices, filter.getSlices(metadata));
    }

    @Test
    public void testEmptyNamesHaveNoSlices()
    {
        ClusteringIndexNamesFilter empty = new ClusteringIndexNamesFilter(BTreeSet.<Clustering<?>>builder(comparator).build(), false);
        assertEquals(0, empty.getSlices(metadata).size());
        assertSame(Slices.NONE, empty.getSlices(metadata));

        // a names filter never serves the empty cache another filter built
        assertSlices(filter(1, 2, 3).getSlices(metadata), 1, 2, 3);
    }

    @Test
    public void testReversedPagingFilterHasItsOwnSlices()
    {
        ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(names(1, 2, 3), true);
        Slices all = filter.getSlices(metadata);

        ClusteringIndexNamesFilter paged = filter.forPaging(comparator, clustering(2), true);
        Slices pagedSlices = paged.getSlices(metadata);
        assertNotSame(all, pagedSlices);
        // paging a reversed filter keeps the rows up to and including the last returned one
        assertSlices(pagedSlices, 1, 2);
        assertSlices(all, 1, 2, 3);
        assertSame(pagedSlices, paged.getSlices(metadata));
    }

    /**
     * The filter intersects the covered clustering of an sstable when one of its names is in the slice. The
     * implementation looks up the first name the slice's start bound admits instead of walking the set, so it must
     * agree with the definition for every membership, for exclusive bounds, for prefix bounds and for empty sets.
     */
    @Test
    public void testIntersectsAgreesWithItsDefinition()
    {
        int[][] nameSets = { {}, { 2 }, { 1, 2 }, { 1, 3, 5 }, { 0, 1, 2, 3, 4, 5, 6 } };
        for (int[] values : nameSets)
        {
            ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(names(values), false);
            for (ClusteringPrefix.Kind startKind : new ClusteringPrefix.Kind[]{
                    ClusteringPrefix.Kind.INCL_START_BOUND, ClusteringPrefix.Kind.EXCL_START_BOUND })
            {
                for (ClusteringBound<?> start : bounds(startKind))
                {
                    for (ClusteringBound<?> end : bounds(ClusteringPrefix.Kind.INCL_END_BOUND))
                        assertIntersects(values, filter, Slice.make(start, end));
                    for (ClusteringBound<?> end : bounds(ClusteringPrefix.Kind.EXCL_END_BOUND))
                        assertIntersects(values, filter, Slice.make(start, end));
                }
            }
            // point slices are the shape a point read of the names filter uses
            for (int value = -1; value <= 7; value++)
                assertIntersects(values, filter, Slice.make(clustering(value)));
        }
    }

    /**
     * A row clustering that is a prefix of the set's members compares as equal to them, so the prefix bounds of a
     * wider comparator have to be handled by the same rule.
     */
    @Test
    public void testIntersectsWithPrefixBoundsAndPrefixNames()
    {
        TableMetadata wider = TableMetadata.builder(KEYSPACE, TABLE + "_prefix")
                                            .addPartitionKeyColumn("k", UTF8Type.instance)
                                            .addClusteringColumn("c", Int32Type.instance)
                                            .addClusteringColumn("d", Int32Type.instance)
                                            .addRegularColumn("v", UTF8Type.instance)
                                            .build();
        ClusteringComparator widerComparator = wider.comparator;

        int[][] nameSets = { {}, { 1 }, { 1, 2 }, { 0, 3, 6 } };
        for (int[] values : nameSets)
        {
            for (int components = 1; components <= 2; components++)
            {
                BTreeSet.Builder<Clustering<?>> builder = BTreeSet.builder(widerComparator);
                for (int value : values)
                    builder.add(components == 1 ? Clustering.make(ByteBufferUtil.bytes(value))
                                                : Clustering.make(ByteBufferUtil.bytes(value), ByteBufferUtil.bytes(value + 1)));
                ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(builder.build(), false);

                for (int startValue = -1; startValue <= 7; startValue++)
                {
                    for (ClusteringPrefix.Kind startKind : new ClusteringPrefix.Kind[]{
                            ClusteringPrefix.Kind.INCL_START_BOUND, ClusteringPrefix.Kind.EXCL_START_BOUND })
                    {
                        for (int boundComponents = 1; boundComponents <= 2; boundComponents++)
                        {
                            for (int endValue = startValue; endValue <= 7; endValue++)
                            {
                                Slice slice = Slice.make(bound(startKind, boundComponents, startValue),
                                                         bound(ClusteringPrefix.Kind.INCL_END_BOUND, boundComponents, endValue));
                                assertIntersects(widerComparator, values, components, filter, slice);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A set that is not a {@link BTreeSet} (and so is not a list in clustering order) takes the general path, which
     * has to keep testing every member.
     */
    @Test
    public void testIntersectsTestsEveryMemberOfASetThatIsNotABTreeSet()
    {
        // two comparators that sort the same way but are not the same instance also miss the fast path
        BTreeSet.Builder<Clustering<?>> builder = BTreeSet.builder(new ClusteringComparator(Int32Type.instance));
        for (int value : new int[]{ 0, 1, 2, 4 })
            builder.add(Clustering.make(ByteBufferUtil.bytes(value)));
        ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(builder.build(), false);

        int[] values = { 0, 1, 2, 4 };
        for (int startValue = -1; startValue <= 5; startValue++)
        {
            Slice slice = Slice.make(bound(ClusteringPrefix.Kind.INCL_START_BOUND, startValue),
                                     bound(ClusteringPrefix.Kind.INCL_END_BOUND, startValue + 1));
            assertIntersects(values, filter, slice);
        }

        // a descending view of the same names: its first member is its largest, so the answer still has to be true
        NavigableSet<Clustering<?>> descending = names(values).descendingSet();
        ClusteringIndexNamesFilter reversedOrder = new ClusteringIndexNamesFilter(descending, false);
        assertEquals(true, reversedOrder.intersects(comparator, Slice.make(bound(ClusteringPrefix.Kind.INCL_START_BOUND, -1),
                                                                          bound(ClusteringPrefix.Kind.INCL_END_BOUND, 0))));
        for (int value = -1; value <= 5; value++)
            assertIntersects(values, reversedOrder, Slice.make(bound(ClusteringPrefix.Kind.INCL_START_BOUND, value),
                                                               bound(ClusteringPrefix.Kind.INCL_END_BOUND, value)));
    }

    private static void assertIntersects(int[] values, ClusteringIndexNamesFilter filter, Slice slice)
    {
        boolean expected = false;
        for (int value : values)
            expected |= slice.includes(comparator, clustering(value));

        assertEquals("names " + Arrays.toString(values) + " against " + slice,
                     expected, filter.intersects(comparator, slice));
    }

    private static void assertIntersects(ClusteringComparator comparator,
                                         int[] values,
                                         int components,
                                         ClusteringIndexNamesFilter filter,
                                         Slice slice)
    {
        boolean expected = false;
        for (int value : values)
        {
            Clustering<?> clustering = components == 1
                                       ? Clustering.make(ByteBufferUtil.bytes(value))
                                       : Clustering.make(ByteBufferUtil.bytes(value), ByteBufferUtil.bytes(value + 1));
            expected |= slice.includes(comparator, clustering);
        }

        assertEquals("names " + Arrays.toString(values) + " (" + components + " components) against " + slice.toString(comparator),
                     expected, filter.intersects(comparator, slice));
    }

    private static ClusteringBound<?> bound(ClusteringPrefix.Kind kind, int value)
    {
        return BufferClusteringBound.create(kind, new ByteBuffer[]{ ByteBufferUtil.bytes(value) });
    }

    private static ClusteringBound<?> bound(ClusteringPrefix.Kind kind,
                                            int components,
                                            int value)
    {
        ByteBuffer[] values = new ByteBuffer[components];
        for (int i = 0; i < components; i++)
            values[i] = ByteBufferUtil.bytes(value);
        return BufferClusteringBound.create(kind, values);
    }

    /**
     * Bounds before and after every name of the sets used by the test, so both ends of the range are exercised
     * for each kind of bound.
     */
    private static ClusteringBound<?>[] bounds(ClusteringPrefix.Kind kind)
    {
        ClusteringBound<?>[] bounds = new ClusteringBound<?>[9];
        for (int i = 0; i < bounds.length; i++)
            bounds[i] = BufferClusteringBound.create(kind, new ByteBuffer[]{ ByteBufferUtil.bytes(i - 1) });
        return bounds;
    }

    private static TableMetadata tableMetadata()
    {
        return TableMetadata.builder(KEYSPACE, TABLE)
                            .addPartitionKeyColumn("k", UTF8Type.instance)
                            .addClusteringColumn("c", Int32Type.instance)
                            .addRegularColumn("v", UTF8Type.instance)
                            .build();
    }

    private static ClusteringIndexNamesFilter filter(int... values)
    {
        return new ClusteringIndexNamesFilter(names(values), false);
    }

    private static NavigableSet<Clustering<?>> names(int... values)
    {
        BTreeSet.Builder<Clustering<?>> builder = BTreeSet.builder(comparator);
        for (int value : values)
            builder.add(clustering(value));
        return builder.build();
    }

    private static Clustering<?> clustering(int value)
    {
        return Clustering.make(ByteBufferUtil.bytes(value));
    }

    private static void assertSlices(Slices actual, int... values)
    {
        assertEquals(values.length, actual.size());
        for (int i = 0; i < values.length; i++)
            assertEquals("slice " + i, Slice.make(clustering(values[i])), actual.get(i));
    }
}
