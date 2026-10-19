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

import java.util.NavigableSet;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
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
