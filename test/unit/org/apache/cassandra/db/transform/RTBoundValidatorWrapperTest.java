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
package org.apache.cassandra.db.transform;

import java.nio.ByteBuffer;
import java.util.Iterator;

import com.google.common.collect.Iterators;

import org.junit.Before;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.ClusteringPrefix.Kind;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.partitions.SingletonUnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.AbstractUnfilteredRowIterator;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.RangeTombstoneBoundMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.WrappingUnfilteredRowIterator;
import org.apache.cassandra.db.transform.RTBoundValidator.Stage;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.db.transform.RTBoundValidator.validate;
import static org.apache.cassandra.utils.ByteBufferUtil.bytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Covers the lightweight validating iterator {@link RTBoundValidator} uses for partitions that do not already carry a
 * transformation stack, and the fallback to the stack for the ones that do.
 */
public final class RTBoundValidatorWrapperTest
{
    private static final String KEYSPACE = "RTBoundValidatorWrapperTest";
    private static final String TABLE = "table";

    private final long nowInSec = FBUtilities.nowInSeconds();

    private TableMetadata metadata;
    private DecoratedKey key;

    @Before
    public void setUp()
    {
        DatabaseDescriptor.daemonInitialization();
        metadata =
            TableMetadata.builder(KEYSPACE, TABLE)
                         .addPartitionKeyColumn("pk", UTF8Type.instance)
                         .addClusteringColumn("ck0", UTF8Type.instance)
                         .addClusteringColumn("ck1", UTF8Type.instance)
                         .addClusteringColumn("ck2", UTF8Type.instance)
                         .addStaticColumn("scol", UTF8Type.instance)
                         .addRegularColumn("rcol", UTF8Type.instance)
                         .partitioner(Murmur3Partitioner.instance)
                         .build();
        key = Murmur3Partitioner.instance.decorateKey(bytes("key"));
    }

    @Test
    public void testPlainPartitionIsNotPushedOnAStack()
    {
        UnfilteredRowIterator input = rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 0, "a", "1")
        );
        UnfilteredRowIterator validated = validate(input, Stage.SSTABLE, false);

        // the whole point of the change: no wrapping UnfilteredRows, Stack and Transformation[] per source
        assertFalse(validated instanceof BaseRows);
        assertTrue(validated instanceof WrappingUnfilteredRowIterator);
        assertSame(input, ((WrappingUnfilteredRowIterator) validated).wrapped());

        assertIteratorsEqual(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 0, "a", "1")
        ), validated);
    }

    @Test
    public void testTransformedPartitionKeepsReusingItsStack()
    {
        // a partition that already carries a transformation must be extended in place, not re-wrapped
        UnfilteredRowIterator transformed = Transformation.apply(rows(false, row(1, "a", "1", "")), new NoOpTransformation());
        assertTrue(transformed instanceof BaseRows);

        assertSame(transformed, validate(transformed, Stage.MERGED, false));
    }

    @Test
    public void testPartitionsIteratorKeepsTheTransformationStack()
    {
        // the stages applied by ReadCommand.executeLocally hand the partition to further transformations, which reuse
        // an existing stack: wrapping it here would make them allocate one instead
        UnfilteredPartitionIterator partitions = validate(partitions(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 0, "a", "1")
        ), Stage.MERGED, false);

        try (UnfilteredRowIterator partition = partitions.next())
        {
            assertTrue(partition instanceof BaseRows);
            drain(partition);
        }
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPartitionsIteratorValidatesEachPartition()
    {
        UnfilteredPartitionIterator partitions = validate(partitions(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 1, "a", "1")
        ), Stage.MERGED, false);

        try (UnfilteredRowIterator partition = partitions.next())
        {
            assertIterationFails(partition);
        }
    }

    @Test
    public void testPartitionsIteratorClosesUnclosedMarkerWhenEnforced()
    {
        UnfilteredPartitionIterator partitions = validate(partitions(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        ), Stage.PROCESSED, true);

        UnfilteredRowIterator partition = partitions.next();
        drain(partition);
        assertCloseFails(partition);
    }

    @Test
    public void testDelegatesPartitionMetadata()
    {
        // non-live deletion, a non-empty static row and a narrowed column set, so returning unset defaults would fail
        ColumnMetadata staticColumn = metadata.getColumn(new ColumnIdentifier("scol", false));
        Row.Builder staticBuilder = BTreeRow.sortedBuilder();
        staticBuilder.newRow(Clustering.STATIC_CLUSTERING);
        staticBuilder.addCell(BufferCell.live(staticColumn, 3, ByteBufferUtil.bytes("s")));
        Row staticRow = staticBuilder.build();
        RegularAndStaticColumns narrowed = new RegularAndStaticColumns.Builder().add(staticColumn).build();
        UnfilteredRowIterator input = rows(true, DeletionTime.build(7, nowInSec), staticRow, narrowed, row(1, "a", "1", ""));
        UnfilteredRowIterator validated = validate(input, Stage.SSTABLE, false);

        assertEquals(input.metadata(), validated.metadata());
        assertEquals(input.partitionKey(), validated.partitionKey());
        assertEquals(input.partitionLevelDeletion(), validated.partitionLevelDeletion());
        assertSame(narrowed, validated.columns());
        assertSame(staticRow, validated.staticRow());
        assertEquals(input.stats(), validated.stats());
        assertEquals(input.isReverseOrder(), validated.isReverseOrder());
    }

    @Test
    public void testLookaheadNeitherSkipsNorRepeats()
    {
        UnfilteredRowIterator validated = validate(rows(false
        , row(1, "a", "1", "")
        , row(2, "a", "2", "")
        ), Stage.SSTABLE, false);

        assertTrue(validated.hasNext());
        assertTrue(validated.hasNext());
        assertEquals(row(1, "a", "1", ""), validated.next());
        assertTrue(validated.hasNext());
        assertEquals(row(2, "a", "2", ""), validated.next());
        assertFalse(validated.hasNext());
        validated.close();
    }

    @Test
    public void testValidatesMismatchedDeletionTimes()
    {
        UnfilteredRowIterator validated = validate(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 1, "a", "1")
        ), Stage.SSTABLE, false);

        assertIterationFails(validated);
    }

    @Test
    public void testValidatesStartBoundFollowedByStartBound()
    {
        UnfilteredRowIterator validated = validate(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        ), Stage.SSTABLE, false);

        assertIterationFails(validated);
    }

    @Test
    public void testValidatesAbsentOpenBound()
    {
        UnfilteredRowIterator validated = validate(rows(false
        , row(1, "a", "1", "")
        , bound(Kind.INCL_END_BOUND, 0, "a", "1")
        ), Stage.SSTABLE, false);

        assertIterationFails(validated);
    }

    @Test
    public void testCloseComplainsAboutUnclosedMarkerWhenEnforced()
    {
        CountingIterator input = new CountingIterator(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        ));
        UnfilteredRowIterator validated = validate(input, Stage.PROCESSED, true);
        drain(validated);

        assertCloseFails(validated);
        // the underlying iterator is still closed, and a repeated close is a no-op rather than a second failure
        assertEquals(1, input.closes);
        validated.close();
        assertEquals(1, input.closes);
    }

    @Test
    public void testCloseWithoutIteratingWhenEnforced()
    {
        CountingIterator input = new CountingIterator(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        ));
        // an unread partition holds no open marker yet, so closing it must not fail but must close the input
        UnfilteredRowIterator validated = validate(input, Stage.PROCESSED, true);
        validated.close();
        assertEquals(1, input.closes);
    }

    @Test
    public void testCloseAcceptsUnclosedMarkerWhenNotEnforced()
    {
        CountingIterator input = new CountingIterator(rows(false
        , bound(Kind.INCL_START_BOUND, 0, "a", "1")
        , row(1, "a", "1", "")
        ));
        UnfilteredRowIterator validated = validate(input, Stage.SSTABLE, false);
        drain(validated);

        validated.close();
        assertEquals(1, input.closes);
    }

    private static void assertIterationFails(UnfilteredRowIterator iter)
    {
        try
        {
            drain(iter);
        }
        catch (IllegalStateException e)
        {
            return;
        }
        fail("expected an IllegalStateException");
    }

    private static void assertCloseFails(UnfilteredRowIterator iter)
    {
        try
        {
            iter.close();
        }
        catch (IllegalStateException e)
        {
            return;
        }
        fail("expected an IllegalStateException");
    }

    private static final class NoOpTransformation extends Transformation<UnfilteredRowIterator>
    {
    }

    /** Counts closes, to check the validator still closes its input when it fails. */
    private static final class CountingIterator extends AbstractUnfilteredRowIterator
    {
        private final UnfilteredRowIterator delegate;
        private int closes;

        private CountingIterator(UnfilteredRowIterator delegate)
        {
            super(delegate.metadata(),
                  delegate.partitionKey(),
                  delegate.partitionLevelDeletion(),
                  delegate.columns(),
                  delegate.staticRow(),
                  delegate.isReverseOrder(),
                  delegate.stats());
            this.delegate = delegate;
        }

        protected Unfiltered computeNext()
        {
            return delegate.hasNext() ? delegate.next() : endOfData();
        }

        @Override
        public void close()
        {
            closes++;
            super.close();
        }
    }

    private RangeTombstoneBoundMarker bound(ClusteringPrefix.Kind kind, long timestamp, Object... clusteringValues)
    {
        ByteBuffer[] clusteringByteBuffers = new ByteBuffer[clusteringValues.length];
        for (int i = 0; i < clusteringValues.length; i++)
            clusteringByteBuffers[i] = decompose(metadata.clusteringColumns().get(i).type, clusteringValues[i]);

        return new RangeTombstoneBoundMarker(BufferClusteringBound.create(kind, clusteringByteBuffers), DeletionTime.build(timestamp, nowInSec));
    }

    private Row row(long timestamp, Object... clusteringValues)
    {
        ByteBuffer[] clusteringByteBuffers = new ByteBuffer[clusteringValues.length];
        for (int i = 0; i < clusteringValues.length; i++)
            clusteringByteBuffers[i] = decompose(metadata.clusteringColumns().get(i).type, clusteringValues[i]);

        return BTreeRow.noCellLiveRow(Clustering.make(clusteringByteBuffers), LivenessInfo.create(timestamp));
    }

    @SuppressWarnings("unchecked")
    private static <T> ByteBuffer decompose(AbstractType<?> type, T value)
    {
        return ((AbstractType<T>) type).decompose(value);
    }

    private UnfilteredRowIterator rows(boolean isReversedOrder, Unfiltered... unfiltereds)
    {
        return rows(isReversedOrder, DeletionTime.LIVE, Rows.EMPTY_STATIC_ROW, metadata.regularAndStaticColumns(), unfiltereds);
    }

    private UnfilteredRowIterator rows(boolean isReversedOrder,
                                       DeletionTime partitionLevelDeletion,
                                       Row staticRow,
                                       RegularAndStaticColumns columns,
                                       Unfiltered... unfiltereds)
    {
        Iterator<Unfiltered> iterator = Iterators.forArray(unfiltereds);
        return new AbstractUnfilteredRowIterator(metadata,
                                                 key,
                                                 partitionLevelDeletion,
                                                 columns,
                                                 staticRow,
                                                 isReversedOrder,
                                                 EncodingStats.NO_STATS)
        {
            protected Unfiltered computeNext()
            {
                return iterator.hasNext() ? iterator.next() : endOfData();
            }
        };
    }

    private UnfilteredPartitionIterator partitions(boolean isReversedOrder, Unfiltered... unfiltereds)
    {
        return new SingletonUnfilteredPartitionIterator(rows(isReversedOrder, unfiltereds));
    }

    private void assertIteratorsEqual(UnfilteredRowIterator expected, UnfilteredRowIterator actual)
    {
        while (expected.hasNext())
        {
            assertTrue(actual.hasNext());
            assertEquals(expected.next(), actual.next());
        }
        assertFalse(actual.hasNext());
        actual.close();
    }

    private static void drain(UnfilteredRowIterator iter)
    {
        while (iter.hasNext())
            iter.next();
    }
}
