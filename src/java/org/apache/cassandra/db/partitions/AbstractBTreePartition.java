/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.cassandra.db.partitions;

import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;

import com.google.common.collect.Iterators;

import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.MutableDeletionInfo;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.rows.AbstractUnfilteredRowIterator;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowAndDeletionMergeIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.RowsOnlyIterator;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.SearchIterator;
import org.apache.cassandra.utils.btree.BTree;
import org.apache.cassandra.utils.btree.BTree.Dir;

import static org.apache.cassandra.db.rows.Rows.EMPTY_STATIC_ROW;
import static org.apache.cassandra.utils.btree.BTree.Dir.desc;

public abstract class AbstractBTreePartition implements Partition, Iterable<Row>
{
    protected final DecoratedKey partitionKey;

    protected abstract BTreePartitionData holder();
    protected abstract boolean canHaveShadowedData();

    protected AbstractBTreePartition(DecoratedKey partitionKey)
    {
        this.partitionKey = partitionKey;
    }

    public DeletionInfo deletionInfo()
    {
        return holder().deletionInfo;
    }

    public Row staticRow()
    {
        return holder().staticRow;
    }

    public boolean isEmpty()
    {
        BTreePartitionData holder = holder();
        return holder.deletionInfo.isLive() && BTree.isEmpty(holder.tree) && holder.staticRow.isEmpty();
    }

    public boolean hasRows()
    {
        BTreePartitionData holder = holder();
        return !BTree.isEmpty(holder.tree);
    }

    public abstract TableMetadata metadata();

    public DecoratedKey partitionKey()
    {
        return partitionKey;
    }

    public DeletionTime partitionLevelDeletion()
    {
        return deletionInfo().getPartitionDeletion();
    }

    public RegularAndStaticColumns columns()
    {
        return holder().columns;
    }

    public EncodingStats stats()
    {
        return holder().stats;
    }

    public Row getRow(Clustering<?> clustering)
    {
        ColumnFilter columns = ColumnFilter.selection(columns());
        BTreePartitionData holder = holder();

        if (clustering == Clustering.STATIC_CLUSTERING)
        {
            // Note that for statics, this will never return null, this will return an empty row. However,
            // it's more consistent for this method to return null if we don't really have a static row.
            Row staticRow = staticRow(holder, columns, true);
            return staticRow.isEmpty() ? null : staticRow;
        }

        final Row row = (Row) BTree.find(holder.tree, metadata().comparator, clustering);
        DeletionTime activeDeletion = holder.deletionInfo.getPartitionDeletion();
        RangeTombstone rt = holder.deletionInfo.rangeCovering(clustering);

        if (rt != null && rt.deletionTime().supersedes(activeDeletion))
            activeDeletion = rt.deletionTime();

        if (row == null)
        {
            // this means our partition level deletion supersedes all other deletions and we don't have to keep the row deletions
            if (activeDeletion == holder.deletionInfo.getPartitionDeletion())
                return null;
            // no need to check activeDeletion.isLive here - if anything superseedes the partitionDeletion
            // it must be non-live
            return BTreeRow.emptyDeletedRow(clustering, Row.Deletion.regular(activeDeletion));
        }
        return row.filter(columns, activeDeletion, true, metadata());
    }

    private Row staticRow(BTreePartitionData current, ColumnFilter columns, boolean setActiveDeletionToRow)
    {
        DeletionTime partitionDeletion = current.deletionInfo.getPartitionDeletion();
        if (columns.fetchedColumns().statics.isEmpty() || (current.staticRow.isEmpty() && partitionDeletion.isLive()))
            return Rows.EMPTY_STATIC_ROW;

        Row row = current.staticRow.filter(columns, partitionDeletion, setActiveDeletionToRow, metadata());
        return row == null ? Rows.EMPTY_STATIC_ROW : row;
    }

    @Override
    public UnfilteredRowIterator unfilteredIterator(ColumnFilter selection, NavigableSet<Clustering<?>> clusteringsInQueryOrder, boolean reversed)
    {
        BTreePartitionData current = holder();
        Row staticRow = staticRow(current, selection, false);
        if (clusteringsInQueryOrder.isEmpty())
        {
            DeletionTime partitionDeletion = current.deletionInfo.getPartitionDeletion();
            return UnfilteredRowIterators.noRowsIterator(metadata(), partitionKey(), staticRow, partitionDeletion, reversed);
        }

        // A query that names a single row only needs one lookup in the partition's tree; the general path also
        // allocates a search iterator over that tree and an iterator over the requested clusterings to look one
        // key up, on every source of the read.
        if (clusteringsInQueryOrder.size() == 1)
            return singleClusteringIterator(selection, clusteringsInQueryOrder.first(), reversed, current, staticRow);

        return new ClusteringsIterator(selection, clusteringsInQueryOrder, reversed, current, staticRow);
    }

    /**
     * The unmerged content of {@code clustering} in {@code current}, which is what a names query for a single
     * clustering asks for. Absent rows are handled the same way as {@link ClusteringsIterator} handles them: with
     * no range tombstone to merge there is nothing to iterate, and with one the merge iterator has to see both.
     */
    private UnfilteredRowIterator singleClusteringIterator(ColumnFilter selection, Clustering<?> clustering, boolean reversed, BTreePartitionData current, Row staticRow)
    {
        Row row = (Row) BTree.find(current.tree, metadata().comparator, clustering);
        Iterator<Row> rowIter = row == null ? Collections.emptyIterator() : Iterators.singletonIterator(row);
        if (!current.deletionInfo.hasRanges())
            return rowsOnlyIterator(rowIter, selection, reversed, current, staticRow);

        Iterator<RangeTombstone> deleteIter = current.deletionInfo.rangeIterator(Slice.make(clustering), reversed);
        if (row == null && !deleteIter.hasNext())
            return rowsOnlyIterator(rowIter, selection, reversed, current, staticRow);

        return merge(rowIter, deleteIter, selection, reversed, current, staticRow);
    }

    public UnfilteredRowIterator unfilteredIterator()
    {
        return unfilteredIterator(ColumnFilter.all(columns()), Slices.ALL, false);
    }

    public UnfilteredRowIterator unfilteredIterator(ColumnFilter selection, Slices slices, boolean reversed)
    {
        return unfilteredIterator(holder(), selection, slices, reversed);
    }

    public UnfilteredRowIterator unfilteredIterator(BTreePartitionData current, ColumnFilter selection, Slices slices, boolean reversed)
    {
        Row staticRow = staticRow(current, selection, false);
        if (slices.size() == 0)
        {
            DeletionTime partitionDeletion = current.deletionInfo.getPartitionDeletion();
            return UnfilteredRowIterators.noRowsIterator(metadata(), partitionKey(), staticRow, partitionDeletion, reversed);
        }

        return slices.size() == 1
               ? sliceIterator(selection, slices.get(0), reversed, current, staticRow)
               : new SlicesIterator(selection, slices, reversed, current, staticRow);
    }

    private UnfilteredRowIterator sliceIterator(ColumnFilter selection, Slice slice, boolean reversed, BTreePartitionData current, Row staticRow)
    {
        Iterator<Row> rowIter = sliceRows(current.tree, slice, reversed);
        if (!current.deletionInfo.hasRanges())
            return rowsOnlyIterator(rowIter, selection, reversed, current, staticRow);

        Iterator<RangeTombstone> deleteIter = current.deletionInfo.rangeIterator(slice, reversed);
        return merge(rowIter, deleteIter, selection, reversed, current, staticRow);
    }

    /**
     * A partition without range tombstones has nothing to merge into its rows, so the rows only need the column
     * selection and the partition deletion applied. This is what {@link RowAndDeletionMergeIterator} does when its
     * range source is empty, but without allocating and walking its merge state for every row of the read.
     */
    private UnfilteredRowIterator rowsOnlyIterator(Iterator<Row> rowIter, ColumnFilter selection, boolean reversed, BTreePartitionData current, Row staticRow)
    {
        return new RowsOnlyIterator(metadata(), partitionKey(), current.deletionInfo.getPartitionDeletion(),
                                    selection, staticRow, reversed, current.stats, rowIter, canHaveShadowedData());
    }

    /**
     * The rows of {@code tree} that fall in {@code slice}. When the slice holds a single clustering, which is what
     * a names query asks for, both of its bounds are that clustering: one bound search then locates the only
     * candidate row, so the second search for the other bound is not needed.
     */
    private Iterator<Row> sliceRows(Object[] tree, Slice slice, boolean reversed)
    {
        ClusteringComparator comparator = metadata().comparator;
        ClusteringBound<?> sliceStart = slice.start();
        ClusteringBound<?> sliceEnd = slice.end();

        if (isSingleClustering(comparator, sliceStart, sliceEnd))
        {
            int index = BTree.ceilIndex(tree, comparator, sliceStart);
            if (index < BTree.size(tree))
            {
                Row row = BTree.findByIndex(tree, index);
                int cmp = comparator.compare(row, sliceEnd);
                if (cmp < 0)
                    return Iterators.singletonIterator(row);
                // A row whose clustering is a strict prefix of the slice bounds compares as equal to both of
                // them, so it is not proof of an empty slice: let the general path decide.
                if (cmp > 0)
                    return Collections.emptyIterator();
            }
            else
            {
                return Collections.emptyIterator();
            }
        }

        ClusteringBound<?> start = sliceStart.isBottom() ? null : sliceStart;
        ClusteringBound<?> end = sliceEnd.isTop() ? null : sliceEnd;
        return BTree.slice(tree, comparator, start, true, end, true, desc(reversed));
    }

    /**
     * Whether the slice holds at most one row, i.e. whether both of its bounds are full inclusive clusterings
     * with the same values. This is what a names query asks for, while {@link Slices#ALL} (whose bounds hold no
     * value) and a slice on a prefix of the clustering columns both span more than one row.
     */
    private static boolean isSingleClustering(ClusteringComparator comparator, ClusteringBound<?> start, ClusteringBound<?> end)
    {
        int size = comparator.size();
        if (size == 0 || start.size() != size || end.size() != size || !start.isInclusive() || !end.isInclusive())
            return false;

        for (int i = 0; i < size; i++)
        {
            if (comparator.compareComponent(i, start, end) != 0)
                return false;
        }
        return true;
    }

    private RowAndDeletionMergeIterator merge(Iterator<Row> rowIter, Iterator<RangeTombstone> deleteIter,
                                              ColumnFilter selection, boolean reversed, BTreePartitionData current, Row staticRow)
    {
        return new RowAndDeletionMergeIterator(metadata(), partitionKey(), current.deletionInfo.getPartitionDeletion(),
                                               selection, staticRow, reversed, current.stats,
                                               rowIter, deleteIter,
                                               canHaveShadowedData());
    }

    private abstract class AbstractIterator extends AbstractUnfilteredRowIterator
    {
        final BTreePartitionData current;
        final ColumnFilter selection;

        private AbstractIterator(BTreePartitionData current, Row staticRow, ColumnFilter selection, boolean isReversed)
        {
            super(AbstractBTreePartition.this.metadata(),
                  AbstractBTreePartition.this.partitionKey(),
                  current.deletionInfo.getPartitionDeletion(),
                  selection.fetchedColumns(), // non-selected columns will be filtered in subclasses by RowAndDeletionMergeIterator
                  // it would also be more precise to return the intersection of the selection and current.columns,
                  // but its probably not worth spending time on computing that.
                  staticRow,
                  isReversed,
                  current.stats);
            this.current = current;
            this.selection = selection;
        }
    }

    private class SlicesIterator extends AbstractIterator
    {
        private final Slices slices;

        private int idx;
        private Iterator<Unfiltered> currentSlice;

        private SlicesIterator(ColumnFilter selection, Slices slices, boolean isReversed, BTreePartitionData current, Row staticRow)
        {
            super(current, staticRow, selection, isReversed);
            this.slices = slices;
        }

        protected Unfiltered computeNext()
        {
            while (true)
            {
                if (currentSlice == null)
                {
                    if (idx >= slices.size())
                        return endOfData();

                    int sliceIdx = isReverseOrder ? slices.size() - idx - 1 : idx;
                    currentSlice = sliceIterator(selection, slices.get(sliceIdx), isReverseOrder, current, Rows.EMPTY_STATIC_ROW);
                    idx++;
                }

                if (currentSlice.hasNext())
                    return currentSlice.next();

                currentSlice = null;
            }
        }
    }

    private class ClusteringsIterator extends AbstractIterator
    {
        private final Iterator<Clustering<?>> clusteringsInQueryOrder;
        private final SearchIterator<Clustering<?>, Row> rowSearcher;

        private Iterator<Unfiltered> currentIterator;

        private ClusteringsIterator(ColumnFilter selection,
                                    NavigableSet<Clustering<?>> clusteringsInQueryOrder,
                                    boolean isReversed,
                                    BTreePartitionData current,
                                    Row staticRow)
        {
            super(current, staticRow, selection, isReversed);

            this.clusteringsInQueryOrder = clusteringsInQueryOrder.iterator();
            this.rowSearcher = BTree.slice(current.tree, metadata().comparator, desc(isReversed));
        }

        protected Unfiltered computeNext()
        {
            while (true)
            {
                if (currentIterator == null)
                {
                    if (!clusteringsInQueryOrder.hasNext())
                        return endOfData();

                    currentIterator = nextIterator(clusteringsInQueryOrder.next());
                }

                if (currentIterator != null && currentIterator.hasNext())
                    return currentIterator.next();

                currentIterator = null;
            }
        }

        private Iterator<Unfiltered> nextIterator(Clustering<?> next)
        {
            Row nextRow = rowSearcher.next(next);
            Iterator<Row> rowIterator = nextRow == null ? Collections.emptyIterator() : Iterators.singletonIterator(nextRow);

            if (!current.deletionInfo.hasRanges())
                return nextRow == null ? null : rowsOnlyIterator(rowIterator, selection, isReverseOrder, current, staticRow);

            // rangeCovering() will return original RT covering clustering key, but we want to generate fake RT with
            // given clustering bound to be consistent with fake RT generated from sstable read.
            Iterator<RangeTombstone> deleteIter = current.deletionInfo.rangeIterator(Slice.make(next), isReverseOrder());

            if (nextRow == null && !deleteIter.hasNext())
                return null;

            return merge(rowIterator, deleteIter, selection, isReverseOrder, current, staticRow);
        }
    }

    protected static BTreePartitionData build(UnfilteredRowIterator iterator, int initialRowCapacity)
    {
        return build(iterator, initialRowCapacity, true);
    }

    protected static BTreePartitionData build(UnfilteredRowIterator iterator, int initialRowCapacity, boolean ordered)
    {
        TableMetadata metadata = iterator.metadata();
        RegularAndStaticColumns columns = iterator.columns();
        boolean reversed = iterator.isReverseOrder();

        BTree.Builder<Row> builder = BTree.builder(metadata.comparator, initialRowCapacity);
        builder.auto(!ordered);
        MutableDeletionInfo.Builder deletionBuilder = MutableDeletionInfo.builder(iterator.partitionLevelDeletion(), metadata.comparator, reversed);

        while (iterator.hasNext())
        {
            Unfiltered unfiltered = iterator.next();
            if (unfiltered.kind() == Unfiltered.Kind.ROW)
                builder.add((Row)unfiltered);
            else
                deletionBuilder.add((RangeTombstoneMarker)unfiltered);
        }

        if (reversed)
            builder.reverse();

        return new BTreePartitionData(columns, builder.build(), deletionBuilder.build(), iterator.staticRow(), iterator.stats());
    }

    // Note that when building with a RowIterator, deletion will generally be LIVE, but we allow to pass it nonetheless because PartitionUpdate
    // passes a MutableDeletionInfo that it mutates later.
    protected static BTreePartitionData build(RowIterator rows, DeletionInfo deletion, boolean buildEncodingStats)
    {
        RegularAndStaticColumns columns = rows.columns();
        boolean reversed = rows.isReverseOrder();

        try (BTree.FastBuilder<Row> builder = BTree.fastBuilder())
        {
            while (rows.hasNext())
                builder.add(rows.next());


            Object[] tree = reversed ? builder.buildReverse()
                                     : builder.build();

            Row staticRow = rows.staticRow();
            EncodingStats stats = buildEncodingStats ? EncodingStats.Collector.collect(staticRow, BTree.iterator(tree), deletion)
                                                     : EncodingStats.NO_STATS;
            return new BTreePartitionData(columns, tree, deletion, staticRow, stats);
        }
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    public String toString(boolean includeFullDetails)
    {
        StringBuilder sb = new StringBuilder();
        if (includeFullDetails)
        {
            sb.append(String.format("[%s.%s] key=%s partition_deletion=%s columns=%s",
                                    metadata().keyspace,
                                    metadata().name,
                                    metadata().partitionKeyType.getString(partitionKey().getKey()),
                                    partitionLevelDeletion(),
                                    columns()));
        }
        else
        {
            sb.append("key=").append(metadata().partitionKeyType.getString(partitionKey().getKey()));
        }

        if (staticRow() != Rows.EMPTY_STATIC_ROW)
            sb.append("\n    ").append(staticRow().toString(metadata(), includeFullDetails));

        try (UnfilteredRowIterator iter = unfilteredIterator())
        {
            while (iter.hasNext())
                sb.append("\n    ").append(iter.next().toString(metadata(), includeFullDetails));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AbstractBTreePartition))
            return false;

        AbstractBTreePartition that = (AbstractBTreePartition) obj;
        BTreePartitionData a = this.holder(), b = that.holder();
        return partitionKey.equals(that.partitionKey)
               && metadata().id.equals(that.metadata().id)
               && a.deletionInfo.equals(b.deletionInfo)
               && a.staticRow.equals(b.staticRow)
               && Iterators.elementsEqual(iterator(), that.iterator());
    }

    public int rowCount()
    {
        return BTree.size(holder().tree);
    }

    @Override
    public Iterator<Row> iterator()
    {
        return iterator(false);
    }

    public Iterator<Row> iterator(boolean reverse)
    {
        return BTree.<Row>iterator(holder().tree, reverse ? Dir.DESC : Dir.ASC);
    }

    public Row lastRow()
    {
        Object[] tree = holder().tree;
        if (BTree.isEmpty(tree))
            return null;

        return BTree.findByIndex(tree, BTree.size(tree) - 1);
    }

    /**
     * The maximum timestamp used in this partition.
     *
     * @return the maximum timestamp used in this update.
     */
    public long maxTimestamp()
    {
        return maxTimestamp(0);
    }

    protected long maxTimestamp(long atLeast)
    {
        BTreePartitionData holder = holder();
        long maxTimestamp = BTree.<Row>accumulate(holder.tree, (row, maxRowTs) -> {
            return row.accumulate((cd, maxCdTs) -> {
                return Math.max(cd.maxTimestamp(), maxCdTs);
            }, Math.max(row.primaryKeyLivenessInfo().timestamp(), maxRowTs));
        }, Math.max(atLeast, holder.deletionInfo.maxTimestamp()));

        Row staticRow = holder.staticRow;
        if (staticRow != EMPTY_STATIC_ROW)
        {
            maxTimestamp = Math.max(maxTimestamp, staticRow.primaryKeyLivenessInfo().timestamp());
            maxTimestamp = staticRow.accumulate((cd, maxCdTs) -> Math.max(cd.maxTimestamp(), maxCdTs), maxTimestamp);
        }
        return maxTimestamp;
    }
}
