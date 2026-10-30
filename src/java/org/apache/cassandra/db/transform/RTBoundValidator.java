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

import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.WrappingUnfilteredRowIterator;

import static org.apache.cassandra.utils.Throwables.maybeFail;
import static org.apache.cassandra.utils.Throwables.merge;

/**
 * A validating transformation that sanity-checks the sequence of Range Tombstone bounds and boundaries in every
 * partition.
 *
 * What we validate, specifically:
 * - that open markers are only followed by close markers
 * - that open markers and close markers have equal deletion times
 * - optionally, that the iterator closes its last RT marker
 */
public final class RTBoundValidator extends Transformation<UnfilteredRowIterator>
{
    public enum Stage { MEMTABLE, SSTABLE, MERGED, PURGED, PROCESSED }

    private final Stage stage;
    private final boolean enforceIsClosed;

    private RTBoundValidator(Stage stage, boolean enforceIsClosed)
    {
        this.stage = stage;
        this.enforceIsClosed = enforceIsClosed;
    }

    public static UnfilteredPartitionIterator validate(UnfilteredPartitionIterator partitions, Stage stage, boolean enforceIsClosed)
    {
        return Transformation.apply(partitions, new RTBoundValidator(stage, enforceIsClosed));
    }

    public static UnfilteredRowIterator validate(UnfilteredRowIterator partition, Stage stage, boolean enforceIsClosed)
    {
        return validateRows(partition, stage, enforceIsClosed);
    }

    /**
     * A partition that already carries a transformation stack only costs a slot in that stack, while a plain iterator
     * would have to pay for the wrapping {@code UnfilteredRows}, its {@code Stack} and the {@code Transformation[]}
     * backing it just to hold this single validator. That is what a read pays per source iterator (one per memtable and
     * per sstable), which is where the wrapper below is used. Partitions that are about to be transformed further
     * (the stages applied by {@code ReadCommand.executeLocally}) keep the stack, so the transformation that follows
     * still finds one to extend instead of allocating it.
     */
    private static UnfilteredRowIterator validateRows(UnfilteredRowIterator partition, Stage stage, boolean enforceIsClosed)
    {
        if (partition instanceof BaseRows)
            return Transformation.apply(partition, new RowsTransformation(stage, partition, enforceIsClosed));

        return new ValidatingIterator(partition, stage, enforceIsClosed);
    }

    @Override
    public UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition)
    {
        return Transformation.apply(partition, new RowsTransformation(stage, partition, enforceIsClosed));
    }

    /**
     * Validates one marker of the stream against the markers seen so far in the same partition, returning the
     * deletion time of the marker that is left open ({@code null} if none is).
     */
    private static DeletionTime validateMarker(DeletionTime openMarkerDeletionTime,
                                                RangeTombstoneMarker marker,
                                                Stage stage,
                                                UnfilteredRowIterator partition)
    {
        if (null == openMarkerDeletionTime)
        {
             // there is no open RT in the stream - we are expecting a *_START_BOUND
            if (marker.isClose(partition.isReverseOrder()))
                throw ise(stage, partition, openMarkerDeletionTime, "unexpected end bound or boundary " + marker.toString(partition.metadata()));
        }
        else
        {
            // there is an open RT in the stream - we are expecting a *_BOUNDARY or an *_END_BOUND
            if (!marker.isClose(partition.isReverseOrder()))
                throw ise(stage, partition, openMarkerDeletionTime, "start bound followed by another start bound " + marker.toString(partition.metadata()));

            // deletion times of open/close markers must match
            DeletionTime deletionTime = marker.closeDeletionTime(partition.isReverseOrder());
            if (!deletionTime.equals(openMarkerDeletionTime))
                throw ise(stage, partition, openMarkerDeletionTime, "open marker and close marker have different deletion times, close=" + deletionTime);

            openMarkerDeletionTime = null;
        }

        if (marker.isOpen(partition.isReverseOrder()))
            openMarkerDeletionTime = marker.openDeletionTime(partition.isReverseOrder());

        return openMarkerDeletionTime;
    }

    private static IllegalStateException ise(Stage stage, UnfilteredRowIterator partition, DeletionTime openMarkerDeletionTime, String why)
    {
        return new IllegalStateException(message(stage, partition, openMarkerDeletionTime, why));
    }

    private static String message(Stage stage, UnfilteredRowIterator partition, DeletionTime openMarkerDeletionTime, String why)
    {
        return String.format("%s UnfilteredRowIterator for %s (key: %s omdt: [%s]) has an illegal Range Tombstone bounds sequence: %s",
                             stage,
                             partition.metadata(),
                             partition.metadata().partitionKeyType.getString(partition.partitionKey().getKey()),
                             Objects.toString(openMarkerDeletionTime, "not present"),
                             why);
    }

    /**
     * Validates a partition without going through the transformation stack; used for the plain iterators the read
     * path produces, one per memtable and sstable source.
     */
    private static final class ValidatingIterator implements WrappingUnfilteredRowIterator
    {
        private final UnfilteredRowIterator input;
        private final Stage stage;
        private final boolean enforceIsClosed;

        private DeletionTime openMarkerDeletionTime;
        private Unfiltered next;
        private boolean nextComputed;
        private boolean closed;

        private ValidatingIterator(UnfilteredRowIterator input, Stage stage, boolean enforceIsClosed)
        {
            this.input = input;
            this.stage = stage;
            this.enforceIsClosed = enforceIsClosed;
        }

        public UnfilteredRowIterator wrapped()
        {
            return input;
        }

        // markers are checked when they are pulled, like the transformation it replaces
        public boolean hasNext()
        {
            if (!nextComputed)
            {
                next = null;
                if (input.hasNext())
                {
                    Unfiltered unfiltered = input.next();
                    if (unfiltered.kind() == Unfiltered.Kind.RANGE_TOMBSTONE_MARKER)
                        openMarkerDeletionTime = validateMarker(openMarkerDeletionTime, (RangeTombstoneMarker) unfiltered, stage, input);
                    next = unfiltered;
                }
                // latched only on success, so a caller that retries after a validation failure sees the input's own
                // failure again instead of a silently truncated stream
                nextComputed = true;
            }
            return next != null;
        }

        public Unfiltered next()
        {
            if (!hasNext())
                throw new NoSuchElementException();

            Unfiltered ret = next;
            next = null;
            nextComputed = false;
            return ret;
        }

        public void close()
        {
            // like the transformation stack this replaces, ignore repeated closes
            if (closed)
                return;

            closed = true;
            Throwable fail = null;
            if (enforceIsClosed && null != openMarkerDeletionTime)
                fail = ise(stage, input, openMarkerDeletionTime, "expected all Range Tombstones to be closed, but the last one is open");

            try
            {
                input.close();
            }
            catch (Throwable t)
            {
                fail = merge(fail, t);
            }
            maybeFail(fail);
        }
    }

    private final static class RowsTransformation extends Transformation
    {
        private final Stage stage;
        private final boolean enforceIsClosed;
        private final UnfilteredRowIterator partition;

        private DeletionTime openMarkerDeletionTime;

        private RowsTransformation(Stage stage, UnfilteredRowIterator partition, boolean enforceIsClosed)
        {
            this.stage = stage;
            this.partition = partition;
            this.enforceIsClosed = enforceIsClosed;
        }

        @Override
        public RangeTombstoneMarker applyToMarker(RangeTombstoneMarker marker)
        {
            openMarkerDeletionTime = validateMarker(openMarkerDeletionTime, marker, stage, partition);
            return marker;
        }

        @Override
        public void onPartitionClose()
        {
            if (enforceIsClosed && null != openMarkerDeletionTime)
                throw ise(stage, partition, openMarkerDeletionTime, "expected all Range Tombstones to be closed, but the last one is open");
        }
    }
}
