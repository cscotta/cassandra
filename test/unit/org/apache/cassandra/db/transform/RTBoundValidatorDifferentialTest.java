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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Iterators;

import org.junit.Before;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.BufferClusteringBoundary;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.rows.AbstractUnfilteredRowIterator;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.RangeTombstoneBoundMarker;
import org.apache.cassandra.db.rows.RangeTombstoneBoundaryMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.RTBoundValidator.Stage;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.db.transform.RTBoundValidator.validate;
import static org.apache.cassandra.utils.ByteBufferUtil.bytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that validating a plain partition with the dedicated wrapper behaves exactly like validating the same partition
 * through the transformation stack, which is the path it replaces: same values, same failures at the same point, same
 * close behaviour. Random (but seeded, so reproducible) marker streams include illegal sequences, so both the accept and
 * the reject paths are compared.
 */
public final class RTBoundValidatorDifferentialTest
{
    private static final String KEYSPACE = "RTBoundValidatorDifferentialTest";
    private static final String TABLE = "table";
    private static final int SEQUENCES = 3000;
    private static final ClusteringPrefix.Kind[] BOUND_KINDS = { ClusteringPrefix.Kind.INCL_START_BOUND,
                                                                 ClusteringPrefix.Kind.EXCL_START_BOUND,
                                                                 ClusteringPrefix.Kind.INCL_END_BOUND,
                                                                 ClusteringPrefix.Kind.EXCL_END_BOUND };
    private static final ClusteringPrefix.Kind[] BOUNDARY_KINDS = { ClusteringPrefix.Kind.EXCL_END_INCL_START_BOUNDARY,
                                                                    ClusteringPrefix.Kind.INCL_END_EXCL_START_BOUNDARY };

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
                         .partitioner(Murmur3Partitioner.instance)
                         .build();
        key = Murmur3Partitioner.instance.decorateKey(bytes("key"));
    }

    @Test
    public void testWrapperMatchesTransformationStack()
    {
        Random rnd = new Random(20261030L);
        int accepted = 0, rejectedWhileIterating = 0, rejectedOnClose = 0;
        for (int i = 0; i < SEQUENCES; i++)
        {
            List<Unfiltered> content = randomContent(rnd);
            boolean reversed = rnd.nextBoolean();
            boolean enforceIsClosed = rnd.nextBoolean();
            Stage stage = Stage.values()[rnd.nextInt(Stage.values().length)];

            Trace stacked = trace(true, content, reversed, enforceIsClosed, stage);
            Trace wrapped = trace(false, content, reversed, enforceIsClosed, stage);

            assertEquals("yielded values differ on " + describe(content, reversed, enforceIsClosed, stage),
                         stacked.values, wrapped.values);
            assertEquals("iteration failure differs on " + describe(content, reversed, enforceIsClosed, stage),
                         stacked.iterationFailure, wrapped.iterationFailure);
            assertEquals("close failure differs on " + describe(content, reversed, enforceIsClosed, stage),
                         stacked.closeFailure, wrapped.closeFailure);

            if (stacked.iterationFailure == null && stacked.closeFailure == null)
                accepted++;
            else if (stacked.iterationFailure != null)
                rejectedWhileIterating++;
            else
                rejectedOnClose++;
        }

        // the generated streams must cover all three outcomes, or the comparison proves nothing
        assertTrue("no legal sequence was generated", accepted > 0);
        assertTrue("no sequence was rejected while iterating", rejectedWhileIterating > 0);
        assertTrue("no sequence was rejected on close", rejectedOnClose > 0);
    }

    private static String describe(List<Unfiltered> content, boolean reversed, boolean enforceIsClosed, Stage stage)
    {
        return "stage " + stage + ", reversed " + reversed + ", enforceIsClosed " + enforceIsClosed + ", " + content;
    }

    /**
     * Drains one partition, recording what it yielded and how iteration and close failed (class and message, so a
     * different failure point or a different message is caught).
     */
    private Trace trace(boolean stacked, List<Unfiltered> content, boolean reversed, boolean enforceIsClosed, Stage stage)
    {
        UnfilteredRowIterator input = rows(reversed, content.toArray(new Unfiltered[0]));
        UnfilteredRowIterator validated = stacked
                                          ? validate(Transformation.apply(input, new NoOpTransformation()), stage, enforceIsClosed)
                                          : validate(input, stage, enforceIsClosed);

        List<Unfiltered> values = new ArrayList<>();
        String iterationFailure = null;
        try
        {
            while (validated.hasNext())
                values.add(validated.next());
        }
        catch (Throwable t)
        {
            iterationFailure = failure(t);
        }

        String closeFailure = null;
        try
        {
            validated.close();
        }
        catch (Throwable t)
        {
            closeFailure = failure(t);
        }
        return new Trace(values, iterationFailure, closeFailure);
    }

    private static String failure(Throwable t)
    {
        return t.getClass().getName() + ": " + t.getMessage();
    }

    private static final class Trace
    {
        private final List<Unfiltered> values;
        private final String iterationFailure;
        private final String closeFailure;

        private Trace(List<Unfiltered> values, String iterationFailure, String closeFailure)
        {
            this.values = values;
            this.iterationFailure = iterationFailure;
            this.closeFailure = closeFailure;
        }
    }

    private static final class NoOpTransformation extends Transformation<UnfilteredRowIterator>
    {
    }

    private List<Unfiltered> randomContent(Random rnd)
    {
        int size = rnd.nextInt(6);
        List<Unfiltered> content = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            long timestamp = rnd.nextInt(3);
            int columns = 1 + rnd.nextInt(2);
            Object[] clustering = new Object[columns];
            for (int c = 0; c < columns; c++)
                clustering[c] = String.valueOf((char) ('a' + rnd.nextInt(3)));
            if (rnd.nextInt(4) == 0)
            {
                content.add(rnd.nextBoolean()
                           ? boundary(BOUNDARY_KINDS[rnd.nextInt(BOUNDARY_KINDS.length)], timestamp, rnd.nextInt(3), clustering)
                           : bound(BOUND_KINDS[rnd.nextInt(BOUND_KINDS.length)], timestamp, clustering));
            }
            else
            {
                content.add(row(timestamp, clustering));
            }
        }
        content.sort((a, b) -> metadata.comparator.compare(a.clustering(), b.clustering()));
        return Collections.unmodifiableList(content);
    }

    private RangeTombstoneBoundMarker bound(ClusteringPrefix.Kind kind, long timestamp, Object... clusteringValues)
    {
        return new RangeTombstoneBoundMarker(BufferClusteringBound.create(kind, clusteringBuffer(clusteringValues)),
                                             DeletionTime.build(timestamp, nowInSec));
    }

    private RangeTombstoneBoundaryMarker boundary(ClusteringPrefix.Kind kind, long closeTimestamp, long openTimestamp, Object... clusteringValues)
    {
        return new RangeTombstoneBoundaryMarker(BufferClusteringBoundary.create(kind, clusteringBuffer(clusteringValues)),
                                                DeletionTime.build(closeTimestamp, nowInSec),
                                                DeletionTime.build(openTimestamp, nowInSec));
    }

    private Row row(long timestamp, Object... clusteringValues)
    {
        return BTreeRow.noCellLiveRow(Clustering.make(clusteringBuffer(clusteringValues)), LivenessInfo.create(timestamp));
    }

    private ByteBuffer[] clusteringBuffer(Object[] clusteringValues)
    {
        ByteBuffer[] clusteringByteBuffers = new ByteBuffer[clusteringValues.length];
        for (int i = 0; i < clusteringValues.length; i++)
            clusteringByteBuffers[i] = decompose(metadata.clusteringColumns().get(i).type, clusteringValues[i]);
        return clusteringByteBuffers;
    }

    @SuppressWarnings("unchecked")
    private static <T> ByteBuffer decompose(AbstractType<?> type, T value)
    {
        return ((AbstractType<T>) type).decompose(value);
    }

    private UnfilteredRowIterator rows(boolean isReversedOrder, Unfiltered... unfiltereds)
    {
        Iterator<Unfiltered> iterator = Iterators.forArray(unfiltereds);
        return new AbstractUnfilteredRowIterator(metadata,
                                                 key,
                                                 DeletionTime.LIVE,
                                                 metadata.regularAndStaticColumns(),
                                                 Rows.EMPTY_STATIC_ROW,
                                                 isReversedOrder,
                                                 EncodingStats.NO_STATS)
        {
            protected Unfiltered computeNext()
            {
                return iterator.hasNext() ? iterator.next() : endOfData();
            }
        };
    }
}
