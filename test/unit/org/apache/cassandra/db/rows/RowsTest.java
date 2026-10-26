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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Assert;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.partitions.PartitionStatisticsCollector;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.db.CellTest.assertCellsEqual;

public class RowsTest
{
    private static final String KEYSPACE = "rows_test";
    private static final String KCVM_TABLE = "kcvm";
    private static final TableMetadata kcvm;
    private static final ColumnMetadata v;
    private static final ColumnMetadata m;
    private static final Clustering<?> c1;

    static
    {
        DatabaseDescriptor.daemonInitialization();
        kcvm =
            TableMetadata.builder(KEYSPACE, KCVM_TABLE)
                         .addPartitionKeyColumn("k", IntegerType.instance)
                         .addClusteringColumn("c", IntegerType.instance)
                         .addRegularColumn("v", IntegerType.instance)
                         .addRegularColumn("m", MapType.getInstance(IntegerType.instance, IntegerType.instance, true))
                         .build();

        v = kcvm.getColumn(new ColumnIdentifier("v", false));
        m = kcvm.getColumn(new ColumnIdentifier("m", false));
        c1 = kcvm.comparator.make(BigInteger.valueOf(1));
    }

    private static final ByteBuffer BB1 = ByteBufferUtil.bytes(1);
    private static final ByteBuffer BB2 = ByteBufferUtil.bytes(2);
    private static final ByteBuffer BB3 = ByteBufferUtil.bytes(3);
    private static final ByteBuffer BB4 = ByteBufferUtil.bytes(4);

    // More regular columns than Row.Merger can merge versions of at once in a single sorted section, so the merge
    // tests below cover many sources, interleaved column order and grouping versions held by non-adjacent sources.
    private static final TableMetadata wideMerge =
        TableMetadata.builder(KEYSPACE, "wide_merge")
                     .addPartitionKeyColumn("k", IntegerType.instance)
                     .addClusteringColumn("ck", IntegerType.instance)
                     .addRegularColumn("a", Int32Type.instance)
                     .addRegularColumn("b", Int32Type.instance)
                     .addRegularColumn("d", Int32Type.instance)
                     .addRegularColumn("e", Int32Type.instance)
                     .addRegularColumn("f", Int32Type.instance)
                     .addRegularColumn("g", Int32Type.instance)
                     .build();

    private static class MergedPair<T>
    {
        public final int idx;
        public final T merged;
        public final T original;

        private MergedPair(int idx, T merged, T original)
        {
            this.idx = idx;
            this.merged = merged;
            this.original = original;
        }

        static <T> MergedPair<T> create(int i, T m, T o)
        {
            return new MergedPair<>(i, m, o);
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MergedPair<?> that = (MergedPair<?>) o;

            if (idx != that.idx) return false;
            if (merged != null ? !merged.equals(that.merged) : that.merged != null) return false;
            return !(original != null ? !original.equals(that.original) : that.original != null);
        }

        public int hashCode()
        {
            int result = idx;
            result = 31 * result + (merged != null ? merged.hashCode() : 0);
            result = 31 * result + (original != null ? original.hashCode() : 0);
            return result;
        }

        public String toString()
        {
            return "MergedPair{" +
                   "idx=" + idx +
                   ", merged=" + merged +
                   ", original=" + original +
                   '}';
        }
    }

    private static class DiffListener implements RowDiffListener
    {
        int updates = 0;
        Clustering<?> clustering = null;

        private void updateClustering(Clustering<?> c)
        {
            assert clustering == null || clustering == c;
            clustering = c;
        }

        List<MergedPair<Cell<?>>> cells = new LinkedList<>();
        public void onCell(int i, Clustering<?> clustering, Cell<?> merged, Cell<?> original)
        {
            updateClustering(clustering);
            cells.add(MergedPair.create(i, merged, original));
            updates++;
        }

        List<MergedPair<LivenessInfo>> liveness = new LinkedList<>();
        public void onPrimaryKeyLivenessInfo(int i, Clustering<?> clustering, LivenessInfo merged, LivenessInfo original)
        {
            updateClustering(clustering);
            liveness.add(MergedPair.create(i, merged, original));
            updates++;
        }

        List<MergedPair<Row.Deletion>> deletions = new LinkedList<>();
        public void onDeletion(int i, Clustering<?> clustering, Row.Deletion merged, Row.Deletion original)
        {
            updateClustering(clustering);
            deletions.add(MergedPair.create(i, merged, original));
            updates++;
        }

        Map<ColumnMetadata, List<MergedPair<DeletionTime>>> complexDeletions = new HashMap<>();
        public void onComplexDeletion(int i, Clustering<?> clustering, ColumnMetadata column, DeletionTime merged, DeletionTime original)
        {
            updateClustering(clustering);
            if (!complexDeletions.containsKey(column)) complexDeletions.put(column, new LinkedList<>());
            complexDeletions.get(column).add(MergedPair.create(i, merged, original));
            updates++;
        }
    }

    public static class StatsCollector implements PartitionStatisticsCollector
    {
        List<Cell<?>> cells = new LinkedList<>();
        public void update(Cell<?> cell)
        {
            cells.add(cell);
        }

        List<LivenessInfo> liveness = new LinkedList<>();
        public void update(LivenessInfo info)
        {
            liveness.add(info);
        }

        List<DeletionTime> deletions = new LinkedList<>();
        public void update(DeletionTime deletion)
        {
            deletions.add(deletion);
        }

        long columnCount = -1;
        public void updateColumnSetPerRow(long columnSetInRow)
        {
            assert columnCount < 0;
            this.columnCount = columnSetInRow;
        }

        boolean hasLegacyCounterShards = false;
        public void updateHasLegacyCounterShards(boolean hasLegacyCounterShards)
        {
            this.hasLegacyCounterShards |= hasLegacyCounterShards;
        }

        @Override
        public void updatePartitionDeletion(DeletionTime dt)
        {
            update(dt);
        }
    }

    private static long secondToTs(long now)
    {
        return now * 1000000L;
    }

    private static Row.Builder createBuilder(Clustering<?> c)
    {
        Row.Builder builder = BTreeRow.unsortedBuilder();
        builder.newRow(c);
        return builder;
    }

    private static Row.Builder createBuilder(Clustering<?> c, long now, ByteBuffer vVal, ByteBuffer mKey, ByteBuffer mVal)
    {
        long ts = secondToTs(now);
        Row.Builder builder = createBuilder(c);
        builder.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        if (vVal != null)
        {
            builder.addCell(BufferCell.live(v, ts, vVal));
        }
        if (mKey != null && mVal != null)
        {
            builder.addComplexDeletion(m, DeletionTime.build(ts - 1, now));
            builder.addCell(BufferCell.live(m, ts, mVal, CellPath.create(mKey)));
        }

        return builder;
    }

    @Test
    public void collectStats()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);
        Row.Builder builder = BTreeRow.unsortedBuilder();
        builder.newRow(c1);
        LivenessInfo liveness = LivenessInfo.create(ts);
        builder.addPrimaryKeyLivenessInfo(liveness);
        DeletionTime complexDeletion = DeletionTime.build(ts-1, now);
        builder.addComplexDeletion(m, complexDeletion);
        List<Cell<?>> expectedCells = Lists.newArrayList(BufferCell.live(v, ts, BB1),
                                                      BufferCell.live(m, ts, BB1, CellPath.create(BB1)),
                                                      BufferCell.live(m, ts, BB2, CellPath.create(BB2)));
        expectedCells.forEach(builder::addCell);
        // We need to use ts-1 so the deletion doesn't shadow what we've created
        Row.Deletion rowDeletion = new Row.Deletion(DeletionTime.build(ts-1, now), false);
        builder.addRowDeletion(rowDeletion);

        StatsCollector collector = new StatsCollector();
        Rows.collectStats(builder.build(), collector);

        Assert.assertEquals(Lists.newArrayList(liveness), collector.liveness);
        Assert.assertEquals(Sets.newHashSet(rowDeletion.time(), complexDeletion), Sets.newHashSet(collector.deletions));
        Assert.assertEquals(Sets.newHashSet(expectedCells), Sets.newHashSet(collector.cells));
        Assert.assertEquals(2, collector.columnCount);
        Assert.assertFalse(collector.hasLegacyCounterShards);
    }


    public static void addExpectedCells(Set<MergedPair<Cell<?>>> dst, Cell<?> merged, Cell<?>... inputs)
    {
        for (int i=0; i<inputs.length; i++)
        {
            dst.add(MergedPair.create(i, merged, inputs[i]));
        }
    }

    private static Row liveRow(Clustering<?> c, long ts, ByteBuffer vVal)
    {
        return rowWithCell(c, ts, BufferCell.live(v, ts, vVal));
    }

    private static Row rowWithCell(Clustering<?> c, long ts, Cell<?> cell)
    {
        Row.Builder builder = createBuilder(c);
        builder.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        builder.addCell(cell);
        return builder.build();
    }

    private static Row rowWithComplexCells(Clustering<?> c, long ts, Cell<?>... cells)
    {
        Row.Builder builder = BTreeRow.unsortedBuilder();
        builder.newRow(c);
        builder.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        for (Cell<?> cell : cells)
            builder.addCell(cell);
        return builder.build();
    }

    private static Row mergeRows(Row... rows)
    {
        boolean hasComplex = false;
        for (Row row : rows)
            hasComplex |= row.hasComplex();
        Row.Merger merger = new Row.Merger(rows.length, hasComplex);
        for (int i = 0; i < rows.length; i++)
            merger.add(i, rows[i]);
        return merger.merge(DeletionTime.LIVE);
    }

    @Test
    public void testMergerMinLocalDeletionTime()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // All inputs are free of deletions and expiring data, so the merged row must be too. This is the fast path in
        // Row.Merger#merge that reuses Cell.MAX_DELETION_TIME instead of recomputing it by scanning the merged btree.
        Row mergedLive = mergeRows(liveRow(c1, ts, BB1), liveRow(c1, ts + 1, BB2));
        Assert.assertEquals(Cell.MAX_DELETION_TIME, mergedLive.minLocalDeletionTime());
        Assert.assertFalse(mergedLive.hasDeletion(now));

        // One input carries an expiring cell that wins reconciliation (higher timestamp): the merged row must keep
        // its expiration time rather than being reported as deletion-free.
        Cell<?> expiringCell = BufferCell.expiring(v, ts + 2, 100, now, BB3);
        Row mergedExpiring = mergeRows(liveRow(c1, ts, BB1), rowWithCell(c1, ts + 2, expiringCell));
        Assert.assertEquals(expiringCell.localDeletionTime(), mergedExpiring.minLocalDeletionTime());
        Assert.assertFalse(mergedExpiring.hasDeletion(now));
        Assert.assertTrue(mergedExpiring.hasDeletion(expiringCell.localDeletionTime()));

        // Same for a tombstone cell winning reconciliation: the merged row must report a deletion.
        Cell<?> tombstoneCell = BufferCell.tombstone(v, ts + 2, now);
        Row mergedTombstone = mergeRows(liveRow(c1, ts, BB1), rowWithCell(c1, ts + 2, tombstoneCell));
        Assert.assertEquals(Long.MIN_VALUE, mergedTombstone.minLocalDeletionTime());
        Assert.assertTrue(mergedTombstone.hasDeletion(now));
    }

    private static ColumnMetadata wideColumn(String name)
    {
        return wideMerge.getColumn(new ColumnIdentifier(name, false));
    }

    private static Row wideRow(long ts, Cell<?>... cells)
    {
        Row.Builder builder = BTreeRow.unsortedBuilder();
        builder.newRow(wideMerge.comparator.make(BigInteger.valueOf(1)));
        builder.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        for (Cell<?> cell : cells)
            builder.addCell(cell);
        return builder.build();
    }

    private static Row mergeWide(Row... rows)
    {
        Row.Merger merger = new Row.Merger(rows.length, false);
        for (int i = 0; i < rows.length; i++)
            merger.add(i, rows[i]);
        return merger.merge(DeletionTime.LIVE);
    }

    private static List<ColumnMetadata> mergedColumns(Row row)
    {
        List<ColumnMetadata> columns = new ArrayList<>();
        for (ColumnData data : row)
            columns.add(data.column());
        return columns;
    }

    private static long mergedTimestamp(Row row, ColumnMetadata column)
    {
        for (ColumnData data : row)
        {
            if (data.column().equals(column))
                return ((Cell<?>) data).timestamp();
        }
        return Long.MIN_VALUE;
    }

    private static ByteBuffer mergedValue(Row row, ColumnMetadata column)
    {
        for (ColumnData data : row)
        {
            if (data.column().equals(column))
                return (ByteBuffer) ((Cell<?>) data).value();
        }
        return null;
    }

    @Test
    public void testMergerManySources()
    {
        ColumnMetadata a = wideColumn("a"), b = wideColumn("b"), d = wideColumn("d"),
                       e = wideColumn("e"), f = wideColumn("f"), g = wideColumn("g");

        // Six sources: disjoint columns interleaved across sources, one source without columns, and one column
        // whose three versions are held by the first, third and last source.
        Row r0 = wideRow(1000, BufferCell.live(a, 1000, BB1), BufferCell.live(e, 1000, BB1), BufferCell.live(f, 1000, BB1));
        Row r1 = wideRow(1001, BufferCell.live(b, 1001, BB2));
        Row r2 = wideRow(1002, BufferCell.live(d, 1002, BB2), BufferCell.live(f, 1002, BB2));
        Row r3 = wideRow(1003);
        Row r4 = wideRow(1004, BufferCell.live(g, 1004, BB4));
        Row r5 = wideRow(1005, BufferCell.live(f, 1005, BB3));

        Row merged = mergeWide(r0, r1, r2, r3, r4, r5);

        Assert.assertEquals(Lists.newArrayList(a, b, d, e, f, g), mergedColumns(merged));
        Assert.assertEquals(1000, mergedTimestamp(merged, a));
        Assert.assertEquals(1001, mergedTimestamp(merged, b));
        Assert.assertEquals(1002, mergedTimestamp(merged, d));
        Assert.assertEquals(1000, mergedTimestamp(merged, e));
        // newest version of f wins, whichever source holds it
        Assert.assertEquals(1005, mergedTimestamp(merged, f));
        Assert.assertEquals(BB3, mergedValue(merged, f));
        Assert.assertEquals(1004, mergedTimestamp(merged, g));
    }

    @Test
    public void testMergerReusedAcrossRows()
    {
        ColumnMetadata a = wideColumn("a"), b = wideColumn("b"), d = wideColumn("d");
        Row.Merger merger = new Row.Merger(3, false);

        merger.add(0, wideRow(1000, BufferCell.live(a, 1000, BB1)));
        merger.add(1, wideRow(1001, BufferCell.live(b, 1001, BB1)));
        merger.add(2, wideRow(1002, BufferCell.live(d, 1002, BB1)));
        Assert.assertEquals(Lists.newArrayList(a, b, d), mergedColumns(merger.merge(DeletionTime.LIVE)));

        // fewer sources than the previous row, and the columns of the previous row must not leak into this one
        merger.clear();
        merger.add(0, wideRow(1010, BufferCell.live(a, 1010, BB2)));
        merger.add(2, wideRow(1011, BufferCell.live(d, 1011, BB2)));
        Row second = merger.merge(DeletionTime.LIVE);
        Assert.assertEquals(Lists.newArrayList(a, d), mergedColumns(second));
        Assert.assertEquals(BB2, mergedValue(second, a));

        // a single source; the live active-deletion case is short-circuited by merge(), so use a deletion that
        // keeps this on the merging path (one source is the OneToOne case of the merge iterator)
        merger.clear();
        merger.add(1, wideRow(1020, BufferCell.live(d, 1020, BB3)));
        Row third = merger.merge(DeletionTime.build(1000, FBUtilities.nowInSeconds()));
        Assert.assertEquals(Lists.newArrayList(d), mergedColumns(third));
        Assert.assertEquals(BB3, mergedValue(third, d));
    }

    @Test
    public void testMergerManySourcesComplexColumns()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // Ten sources (past the in-place merge's source threshold, and past the merge iterator's sorted section):
        // every source carries a version of the same complex column plus a simple column, so the merged row must
        // reconcile all of them instead of dropping or double-reducing any version.
        Row[] rows = new Row[10];
        for (int i = 0; i < rows.length; i++)
        {
            Row.Builder builder = BTreeRow.unsortedBuilder();
            builder.newRow(c1);
            builder.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts + i));
            builder.addCell(BufferCell.live(v, ts + i, ByteBufferUtil.bytes(i)));
            builder.addCell(BufferCell.live(m, ts + i, ByteBufferUtil.bytes(i), CellPath.create(BB1)));
            rows[i] = builder.build();
        }

        Row merged = mergeRows(rows);

        Assert.assertEquals(Lists.newArrayList(v, m), mergedColumns(merged));
        Assert.assertEquals(ts + 9, mergedTimestamp(merged, v));
        ComplexColumnData complex = merged.getComplexColumnData(m);
        Assert.assertNotNull(complex);
        Assert.assertEquals(1, complex.cellsCount());
        Assert.assertEquals(ts + 9, complex.iterator().next().timestamp());
    }

    @Test
    public void testMergerDropsColumnsShadowedByActiveDeletion()
    {
        long now = FBUtilities.nowInSeconds();
        ColumnMetadata a = wideColumn("a"), b = wideColumn("b");

        Row r0 = wideRow(3000, BufferCell.live(a, 3000, BB1));
        Row r1 = wideRow(2950, BufferCell.live(b, 2950, BB1));

        Row merged = mergeWide(r0, r1);
        Assert.assertEquals(Lists.newArrayList(a, b), mergedColumns(merged));

        // the deletion only shadows b, so the merge must emit a alone and skip the fully shadowed column
        Row.Merger merger = new Row.Merger(2, false);
        merger.add(0, r0);
        merger.add(1, r1);
        Row shadowed = merger.merge(DeletionTime.build(2990, now));
        Assert.assertEquals(Lists.newArrayList(a), mergedColumns(shadowed));
        Assert.assertEquals(3000, mergedTimestamp(shadowed, a));
    }

    @Test
    public void testMergerGroupsComplexColumnVersions()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // two versions of the same complex column, plus a simple column present in both sources
        Row.Builder b1 = BTreeRow.unsortedBuilder();
        b1.newRow(c1);
        b1.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        b1.addCell(BufferCell.live(v, ts, BB1));
        b1.addCell(BufferCell.live(m, ts, BB1, CellPath.create(BB1)));
        Row.Builder b2 = BTreeRow.unsortedBuilder();
        b2.newRow(c1);
        b2.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts + 1));
        b2.addCell(BufferCell.live(v, ts + 1, BB2));
        b2.addCell(BufferCell.live(m, ts + 1, BB2, CellPath.create(BB2)));

        Row merged = mergeRows(b1.build(), b2.build());

        Assert.assertEquals(Lists.newArrayList(v, m), mergedColumns(merged));
        Assert.assertEquals(ts + 1, mergedTimestamp(merged, v));
        ComplexColumnData complex = merged.getComplexColumnData(m);
        Assert.assertNotNull(complex);
        Assert.assertEquals(2, complex.cellsCount());
    }

    @Test
    public void testMergerGroupsSameComplexCellPathVersions()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // Both sources hold a version of the same map entry (same cell path): the in-place cell merge must reduce them
        // to a single cell and keep the newer version, while entries held by one source only are emitted once.
        Row r0 = rowWithComplexCells(c1, ts, BufferCell.live(m, ts, BB1, CellPath.create(BB1)));
        Row r1 = rowWithComplexCells(c1, ts + 1,
                                     BufferCell.live(m, ts + 1, BB2, CellPath.create(BB1)),
                                     BufferCell.live(m, ts + 1, BB3, CellPath.create(BB2)));

        Row merged = mergeRows(r0, r1);

        Assert.assertEquals(Lists.newArrayList(m), mergedColumns(merged));
        ComplexColumnData complex = merged.getComplexColumnData(m);
        Assert.assertNotNull(complex);
        Assert.assertEquals(2, complex.cellsCount());

        Cell<?> firstPath = null, secondPath = null;
        for (Cell<?> cell : complex)
        {
            if (cell.path().equals(CellPath.create(BB1)))
                firstPath = cell;
            else if (cell.path().equals(CellPath.create(BB2)))
                secondPath = cell;
        }
        Assert.assertNotNull(firstPath);
        Assert.assertEquals(ts + 1, firstPath.timestamp());
        Assert.assertEquals(BB2, firstPath.value());
        Assert.assertNotNull(secondPath);
        Assert.assertEquals(ts + 1, secondPath.timestamp());
    }

    @Test
    public void testMergerReusedAcrossComplexColumns()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);
        Row.Merger merger = new Row.Merger(3, true);

        merger.add(0, rowWithComplexCells(c1, ts, BufferCell.live(m, ts, BB1, CellPath.create(BB1))));
        merger.add(1, rowWithComplexCells(c1, ts + 1, BufferCell.live(m, ts + 1, BB2, CellPath.create(BB1))));
        merger.add(2, rowWithComplexCells(c1, ts + 2, BufferCell.live(m, ts + 2, BB3, CellPath.create(BB2))));
        ComplexColumnData first = merger.merge(DeletionTime.LIVE).getComplexColumnData(m);
        Assert.assertNotNull(first);
        Assert.assertEquals(2, first.cellsCount());

        // fewer sources than the previous row: the cells of the previous row must not leak into this one. A non-live
        // deletion is required here, a single row with a live deletion is returned without merging.
        merger.clear();
        merger.add(0, rowWithComplexCells(c1, ts + 10, BufferCell.live(m, ts + 10, BB4, CellPath.create(BB1))));
        ComplexColumnData second = merger.merge(DeletionTime.build(ts + 5, now)).getComplexColumnData(m);
        Assert.assertNotNull(second);
        Assert.assertEquals(1, second.cellsCount());
        Assert.assertEquals(ts + 10, second.iterator().next().timestamp());

        // a single source, which the merge iterator served through its one-to-one case
        merger.clear();
        merger.add(1, rowWithComplexCells(c1, ts + 20, BufferCell.live(m, ts + 20, BB2, CellPath.create(BB2))));
        ComplexColumnData third = merger.merge(DeletionTime.build(ts + 15, now)).getComplexColumnData(m);
        Assert.assertNotNull(third);
        Assert.assertEquals(1, third.cellsCount());
        Assert.assertEquals(ts + 20, third.iterator().next().timestamp());
    }

    @Test
    public void testMergerDropsComplexCellsShadowedByActiveDeletion()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        Row r0 = rowWithComplexCells(c1, ts, BufferCell.live(m, ts, BB1, CellPath.create(BB1)));
        Row r1 = rowWithComplexCells(c1, ts - 100, BufferCell.live(m, ts - 100, BB2, CellPath.create(BB2)));

        Row.Merger merger = new Row.Merger(2, true);
        merger.add(0, r0);
        merger.add(1, r1);
        // only the older entry is shadowed by the active deletion
        ComplexColumnData complex = merger.merge(DeletionTime.build(ts - 99, now)).getComplexColumnData(m);
        Assert.assertNotNull(complex);
        Assert.assertEquals(1, complex.cellsCount());
        Assert.assertEquals(ts, complex.iterator().next().timestamp());
    }

    @Test
    public void testMergerKeepsComplexDeletion()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        Row.Builder r0 = BTreeRow.unsortedBuilder();
        r0.newRow(c1);
        r0.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts));
        r0.addComplexDeletion(m, DeletionTime.build(ts - 5, now));
        r0.addCell(BufferCell.live(m, ts, BB1, CellPath.create(BB1)));

        Row.Builder r1 = BTreeRow.unsortedBuilder();
        r1.newRow(c1);
        r1.addPrimaryKeyLivenessInfo(LivenessInfo.create(ts + 1));
        r1.addCell(BufferCell.live(m, ts + 1, BB2, CellPath.create(BB2)));

        Row merged = mergeRows(r0.build(), r1.build());

        ComplexColumnData complex = merged.getComplexColumnData(m);
        Assert.assertNotNull(complex);
        Assert.assertEquals(DeletionTime.build(ts - 5, now), complex.complexDeletion());
        Assert.assertEquals(2, complex.cellsCount());
    }

    @Test
    public void testMergerGroupsSameComplexCellPathVersionsManySources()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // Both sides of the in-place merge's source threshold: 8 sources take the in-place path, 9 and 10 the merge
        // iterator slow path. Each must reconcile the versions of the one map entry to its newest value.
        for (int sources : new int[]{ 8, 9, 10 })
        {
            Row[] rows = new Row[sources];
            for (int i = 0; i < rows.length; i++)
                rows[i] = rowWithComplexCells(c1, ts + i, BufferCell.live(m, ts + i, ByteBufferUtil.bytes(i), CellPath.create(BB1)));

            Row merged = mergeRows(rows);

            ComplexColumnData complex = merged.getComplexColumnData(m);
            Assert.assertNotNull(complex);
            Assert.assertEquals(sources + " sources", 1, complex.cellsCount());
            Cell<?> cell = complex.iterator().next();
            Assert.assertEquals(sources + " sources", ts + sources - 1, cell.timestamp());
            Assert.assertEquals(sources + " sources", ByteBufferUtil.bytes(sources - 1), cell.value());
        }
    }

    @Test
    public void testMergerGroupsMixedComplexPathsAcrossSources()
    {
        long now = FBUtilities.nowInSeconds();
        long ts = secondToTs(now);

        // Two shared paths and one path held by a single source, with the shared versions arriving in different
        // timestamps per source, so the grouping (not the source order) decides the emitted cells.
        Row r0 = rowWithComplexCells(c1, ts,
                                     BufferCell.live(m, ts, BB1, CellPath.create(BB1)),
                                     BufferCell.live(m, ts, BB2, CellPath.create(BB2)),
                                     BufferCell.live(m, ts, BB3, CellPath.create(BB3)));
        Row r1 = rowWithComplexCells(c1, ts + 1,
                                     BufferCell.live(m, ts + 1, BB2, CellPath.create(BB2)),
                                     BufferCell.live(m, ts + 1, BB4, CellPath.create(BB1)));
        Row r2 = rowWithComplexCells(c1, ts + 2,
                                     BufferCell.live(m, ts + 2, BB4, CellPath.create(BB3)));

        Row merged = mergeRows(r0, r1, r2);

        Map<ByteBuffer, Cell<?>> byPath = new HashMap<>();
        for (Cell<?> cell : merged.getComplexColumnData(m))
            Assert.assertNull("duplicate path " + ByteBufferUtil.bytesToHex(cell.path().get(0)), byPath.put(cell.path().get(0), cell));

        Assert.assertEquals(3, byPath.size());
        Assert.assertEquals(ts + 1, byPath.get(BB1).timestamp());
        Assert.assertEquals(BB4, byPath.get(BB1).value());
        Assert.assertEquals(ts + 1, byPath.get(BB2).timestamp());
        Assert.assertEquals(BB2, byPath.get(BB2).value());
        Assert.assertEquals(ts + 2, byPath.get(BB3).timestamp());
        Assert.assertEquals(BB4, byPath.get(BB3).value());
    }

    @Test
    public void diff()
    {
        long now1 = FBUtilities.nowInSeconds();
        long ts1 = secondToTs(now1);
        Row.Builder r1Builder = BTreeRow.unsortedBuilder();
        r1Builder.newRow(c1);
        LivenessInfo r1Liveness = LivenessInfo.create(ts1);
        r1Builder.addPrimaryKeyLivenessInfo(r1Liveness);
        DeletionTime r1ComplexDeletion = DeletionTime.build(ts1-1, now1);
        r1Builder.addComplexDeletion(m, r1ComplexDeletion);

        Cell<?> r1v = BufferCell.live(v, ts1, BB1);
        Cell<?> r1m1 = BufferCell.live(m, ts1, BB1, CellPath.create(BB1));
        Cell<?> r1m2 = BufferCell.live(m, ts1, BB2, CellPath.create(BB2));
        List<Cell<?>> r1ExpectedCells = Lists.newArrayList(r1v, r1m1, r1m2);

        r1ExpectedCells.forEach(r1Builder::addCell);

        long now2 = now1 + 1;
        long ts2 = secondToTs(now2);
        Row.Builder r2Builder = BTreeRow.unsortedBuilder();
        r2Builder.newRow(c1);
        LivenessInfo r2Liveness = LivenessInfo.create(ts2);
        r2Builder.addPrimaryKeyLivenessInfo(r2Liveness);
        Cell<?> r2v = BufferCell.live(v, ts2, BB2);
        Cell<?> r2m2 = BufferCell.live(m, ts2, BB1, CellPath.create(BB2));
        Cell<?> r2m3 = BufferCell.live(m, ts2, BB2, CellPath.create(BB3));
        Cell<?> r2m4 = BufferCell.live(m, ts2, BB3, CellPath.create(BB4));
        List<Cell<?>> r2ExpectedCells = Lists.newArrayList(r2v, r2m2, r2m3, r2m4);

        r2ExpectedCells.forEach(r2Builder::addCell);
        Row.Deletion r2RowDeletion = new Row.Deletion(DeletionTime.build(ts1 - 2, now2), false);
        r2Builder.addRowDeletion(r2RowDeletion);

        Row r1 = r1Builder.build();
        Row r2 = r2Builder.build();
        Row merged = Rows.merge(r1, r2);

        Assert.assertEquals(r1ComplexDeletion, merged.getComplexColumnData(m).complexDeletion());

        DiffListener listener = new DiffListener();
        Rows.diff(listener, merged, r1, r2);

        Assert.assertEquals(c1, listener.clustering);

        // check cells
        Set<MergedPair<Cell<?>>> expectedCells = Sets.newHashSet();
        addExpectedCells(expectedCells, r2v,  r1v,  r2v);     // v
        addExpectedCells(expectedCells, r1m1, r1m1, null);   // m[1]
        addExpectedCells(expectedCells, r2m2, r1m2, r2m2);   // m[2]
        addExpectedCells(expectedCells, r2m3, null, r2m3);   // m[3]
        addExpectedCells(expectedCells, r2m4, null, r2m4);   // m[4]

        Assert.assertEquals(expectedCells.size(), listener.cells.size());
        Assert.assertEquals(expectedCells, Sets.newHashSet(listener.cells));

        // liveness
        List<MergedPair<LivenessInfo>> expectedLiveness = Lists.newArrayList(MergedPair.create(0, r2Liveness, r1Liveness),
                                                                             MergedPair.create(1, r2Liveness, r2Liveness));
        Assert.assertEquals(expectedLiveness, listener.liveness);

        // deletions
        List<MergedPair<Row.Deletion>> expectedDeletions = Lists.newArrayList(MergedPair.create(0, r2RowDeletion, null),
                                                                              MergedPair.create(1, r2RowDeletion, r2RowDeletion));
        Assert.assertEquals(expectedDeletions, listener.deletions);

        // complex deletions
        List<MergedPair<DeletionTime>> expectedCmplxDeletions = Lists.newArrayList(MergedPair.create(0, r1ComplexDeletion, r1ComplexDeletion),
                                                                                   MergedPair.create(1, r1ComplexDeletion, DeletionTime.LIVE));
        Assert.assertEquals(ImmutableMap.builder().put(m, expectedCmplxDeletions).build(), listener.complexDeletions);
    }

    /**
     * merged row has no column data
     */
    @Test
    public void diffEmptyMerged()
    {
        long now1 = FBUtilities.nowInSeconds();
        long ts1 = secondToTs(now1);
        Row.Builder r1Builder = BTreeRow.unsortedBuilder();
        r1Builder.newRow(c1);
        LivenessInfo r1Liveness = LivenessInfo.create(ts1);
        r1Builder.addPrimaryKeyLivenessInfo(r1Liveness);

        // mergedData == null
        long now2 = now1 + 1L;
        long ts2 = secondToTs(now2);
        Row.Builder r2Builder = BTreeRow.unsortedBuilder();
        r2Builder.newRow(c1);
        LivenessInfo r2Liveness = LivenessInfo.create(ts2);
        r2Builder.addPrimaryKeyLivenessInfo(r2Liveness);
        DeletionTime r2ComplexDeletion = DeletionTime.build(ts2-1, now2);
        r2Builder.addComplexDeletion(m, r2ComplexDeletion);
        Cell<?> r2v = BufferCell.live(v, ts2, BB2);
        Cell<?> r2m2 = BufferCell.live(m, ts2, BB1, CellPath.create(BB2));
        Cell<?> r2m3 = BufferCell.live(m, ts2, BB2, CellPath.create(BB3));
        Cell<?> r2m4 = BufferCell.live(m, ts2, BB3, CellPath.create(BB4));
        List<Cell<?>> r2ExpectedCells = Lists.newArrayList(r2v, r2m2, r2m3, r2m4);

        r2ExpectedCells.forEach(r2Builder::addCell);
        Row.Deletion r2RowDeletion = new Row.Deletion(DeletionTime.build(ts1 - 1, now2), false);
        r2Builder.addRowDeletion(r2RowDeletion);

        Row r1 = r1Builder.build();
        Row r2 = r2Builder.build();

        DiffListener listener = new DiffListener();
        Rows.diff(listener, r1, r2);

        Assert.assertEquals(c1, listener.clustering);

        // check cells
        Set<MergedPair<Cell<?>>> expectedCells = Sets.newHashSet(MergedPair.create(0, null, r2v),   // v
                                                              MergedPair.create(0, null, r2m2),  // m[2]
                                                              MergedPair.create(0, null, r2m3),  // m[3]
                                                              MergedPair.create(0, null, r2m4)); // m[4]

        Assert.assertEquals(expectedCells.size(), listener.cells.size());
        Assert.assertEquals(expectedCells, Sets.newHashSet(listener.cells));

        // complex deletions
        List<MergedPair<DeletionTime>> expectedCmplxDeletions = Lists.newArrayList(MergedPair.create(0, null, r2ComplexDeletion));
        Assert.assertEquals(ImmutableMap.builder().put(m, expectedCmplxDeletions).build(), listener.complexDeletions);
    }

    /**
     * input row has no column data
     */
    @Test
    public void diffEmptyInput()
    {
        long now1 = FBUtilities.nowInSeconds();
        long ts1 = secondToTs(now1);
        Row.Builder r1Builder = BTreeRow.unsortedBuilder();
        r1Builder.newRow(c1);
        LivenessInfo r1Liveness = LivenessInfo.create(ts1);
        r1Builder.addPrimaryKeyLivenessInfo(r1Liveness);

        // mergedData == null
        long now2 = now1 + 1L;
        long ts2 = secondToTs(now2);
        Row.Builder r2Builder = BTreeRow.unsortedBuilder();
        r2Builder.newRow(c1);
        LivenessInfo r2Liveness = LivenessInfo.create(ts2);
        r2Builder.addPrimaryKeyLivenessInfo(r2Liveness);
        DeletionTime r2ComplexDeletion = DeletionTime.build(ts2-1, now2);
        r2Builder.addComplexDeletion(m, r2ComplexDeletion);
        Cell<?> r2v = BufferCell.live(v, ts2, BB2);
        Cell<?> r2m2 = BufferCell.live(m, ts2, BB1, CellPath.create(BB2));
        Cell<?> r2m3 = BufferCell.live(m, ts2, BB2, CellPath.create(BB3));
        Cell<?> r2m4 = BufferCell.live(m, ts2, BB3, CellPath.create(BB4));
        List<Cell<?>> r2ExpectedCells = Lists.newArrayList(r2v, r2m2, r2m3, r2m4);

        r2ExpectedCells.forEach(r2Builder::addCell);
        Row.Deletion r2RowDeletion = new Row.Deletion(DeletionTime.build(ts1 - 1, now2), false);
        r2Builder.addRowDeletion(r2RowDeletion);

        Row r1 = r1Builder.build();
        Row r2 = r2Builder.build();

        DiffListener listener = new DiffListener();
        Rows.diff(listener, r2, r1);

        Assert.assertEquals(c1, listener.clustering);

        // check cells
        Set<MergedPair<Cell<?>>> expectedCells = Sets.newHashSet(MergedPair.create(0, r2v, null),   // v
                                                              MergedPair.create(0, r2m2, null),  // m[2]
                                                              MergedPair.create(0, r2m3, null),  // m[3]
                                                              MergedPair.create(0, r2m4, null)); // m[4]

        Assert.assertEquals(expectedCells.size(), listener.cells.size());
        Assert.assertEquals(expectedCells, Sets.newHashSet(listener.cells));

        // complex deletions
        List<MergedPair<DeletionTime>> expectedCmplxDeletions = Lists.newArrayList(MergedPair.create(0, r2ComplexDeletion, null));
        Assert.assertEquals(ImmutableMap.builder().put(m, expectedCmplxDeletions).build(), listener.complexDeletions);
    }

    @Test
    public void merge()
    {
        long now1 = FBUtilities.nowInSeconds();
        Row.Builder existingBuilder = createBuilder(c1, now1, BB1, BB1, BB1);

        long now2 = now1 + 1L;
        long ts2 = secondToTs(now2);

        Cell<?> expectedVCell = BufferCell.live(v, ts2, BB2);
        Cell<?> expectedMCell = BufferCell.live(m, ts2, BB2, CellPath.create(BB1));
        DeletionTime expectedComplexDeletionTime = DeletionTime.build(ts2 - 1, now2);

        Row.Builder updateBuilder = createBuilder(c1, now2, null, null, null);
        updateBuilder.addCell(expectedVCell);
        updateBuilder.addComplexDeletion(m, expectedComplexDeletionTime);
        updateBuilder.addCell(expectedMCell);

        Row merged = Rows.merge(existingBuilder.build(), updateBuilder.build());

        Assert.assertEquals(c1, merged.clustering());
        Assert.assertEquals(LivenessInfo.create(ts2), merged.primaryKeyLivenessInfo());

        Iterator<Cell<?>> iter = merged.cells().iterator();
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(expectedVCell, iter.next());
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(expectedMCell, iter.next());
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void mergeComplexDeletionSupersededByRowDeletion()
    {
        long now1 = FBUtilities.nowInSeconds();
        Row.Builder existingBuilder = createBuilder(c1, now1, null, BB2, BB2);

        long now2 = now1 + 1L;
        Row.Builder updateBuilder = createBuilder(c1);
        long now3 = now2 + 1L;
        Row.Deletion expectedDeletion = new Row.Deletion(DeletionTime.build(secondToTs(now3), now3), false);
        updateBuilder.addRowDeletion(expectedDeletion);

        Row merged = Rows.merge(existingBuilder.build(), updateBuilder.build());

        Assert.assertEquals(expectedDeletion, merged.deletion());
        Assert.assertFalse(merged.hasComplexDeletion());
        Assert.assertFalse(merged.cells().iterator().hasNext());
    }

    @Test
    public void mergeRowDeletionSupercedesLiveness()
    {
        long now1 = FBUtilities.nowInSeconds();
        Row.Builder existingBuilder = createBuilder(c1, now1, BB1, BB1, BB1);

        long now2 = now1 + 1L;
        Row.Builder updateBuilder = createBuilder(c1);
        long now3 = now2 + 1L;
        Row.Deletion expectedDeletion = new Row.Deletion(DeletionTime.build(secondToTs(now3), now3), false);
        updateBuilder.addRowDeletion(expectedDeletion);

        Row merged = Rows.merge(existingBuilder.build(), updateBuilder.build());

        Assert.assertEquals(expectedDeletion, merged.deletion());
        Assert.assertEquals(LivenessInfo.EMPTY, merged.primaryKeyLivenessInfo());
        Assert.assertEquals(0, merged.columns().size());
    }


    public static BufferCell expiringWithExpirationTime(ColumnMetadata column, long timestamp, int ttl, long localDeletionTime, ByteBuffer value)
    {
        return expiringWithExpirationTime(column, timestamp, ttl, localDeletionTime, value, null);
    }

    public static BufferCell expiringWithExpirationTime(ColumnMetadata column, long timestamp, int ttl, long localDeletionTime, ByteBuffer value, CellPath path)
    {
        assert ttl != Cell.NO_TTL;
        return new BufferCell(column, timestamp, ttl, localDeletionTime, value, path);
    }

    @Test
    public void mergeRowsWithSameExpiryDifferentTTLCommutesLiveness()
    {
        long now1 = FBUtilities.nowInSeconds();
        long ts1 = secondToTs(now1);
        long ldt = now1 + 1000;

        Row.Builder r1Builder = BTreeRow.unsortedBuilder();
        r1Builder.newRow(c1);
        LivenessInfo originalLiveness = LivenessInfo.withExpirationTime(ts1, 100, ldt);
        r1Builder.addPrimaryKeyLivenessInfo(originalLiveness);

        Row.Builder r2Builder = BTreeRow.unsortedBuilder();
        r2Builder.newRow(c1);
        LivenessInfo loweredTTL = LivenessInfo.withExpirationTime(ts1, 50, ldt);
        r2Builder.addPrimaryKeyLivenessInfo(loweredTTL);

        Cell<?> r2v = expiringWithExpirationTime(v, ts1, 75, ldt, BB1);
        Cell<?> r2m2 = expiringWithExpirationTime(m, ts1, 50, ldt, BB1, CellPath.create(BB2));
        Cell<?> r2m3 = expiringWithExpirationTime(m, ts1, 75, ldt, BB2, CellPath.create(BB3));
        Cell<?> r2m4 = expiringWithExpirationTime(m, ts1, 100, ldt, BB3, CellPath.create(BB4));
        List<Cell<?>> expectedCells = Lists.newArrayList(r2v, r2m2, r2m3, r2m4);

        expectedCells.forEach(r1Builder::addCell);
        expectedCells.forEach(r2Builder::addCell);

        Row r1 = r1Builder.build();
        Row r2 = r2Builder.build();

        Row r1r2 = Rows.merge(r1, r2);
        Row r2r1 = Rows.merge(r2, r1);

        DiffListener mergedListener = new DiffListener();
        Rows.diff(mergedListener, r1r2, r2r1);

        mergedListener.liveness.forEach(pair -> Assert.assertEquals(pair.merged, pair.original));
        mergedListener.cells.forEach(pair -> assertCellsEqual(pair.merged, pair.original));
    }


    // Creates a dummy cell for a (regular) column for the provided name and without a cellPath.
    private static Cell<?> liveCell(ColumnMetadata name)
    {
        return liveCell(name, -1);
    }

    // Creates a dummy cell for a (regular) column for the provided name.
    // If path >= 0, the cell will have a CellPath containing path as an Int32Type.
    private static Cell<?> liveCell(ColumnMetadata name, int path)
    {
        CellPath cp = path < 0 ? null : CellPath.create(ByteBufferUtil.bytes(path));
        return new BufferCell(name, 0L, Cell.NO_TTL, Cell.NO_DELETION_TIME, ByteBuffer.allocate(1), cp);
    }

    // Assert that the cells generated by iterating iterable are the cell of cells (in the same order
    // and with neither more nor less cells).
    private static void assertCellOrder(Iterable<Cell<?>> iterable, Cell<?>... cells)
    {
        int i = 0;
        for (Cell<?> actual : iterable)
        {
            Assert.assertFalse(String.format("Got more rows than expected (expecting %d). First unexpected cell is %s", cells.length, actual), i >= cells.length);
            Assert.assertEquals(cells[i++], actual);
        }
        Assert.assertFalse(String.format("Got less rows than expected (got %d while expecting %d).", i, cells.length), i < cells.length);
    }

    // Make a dummy row (empty clustering) with the provided cells, that are assumed to be in order
    private static Row makeDummyRow(Cell<?> ... cells)
    {
        Row.Builder builder = BTreeRow.sortedBuilder();
        builder.newRow(Clustering.EMPTY);
        for (Cell<?> cell : cells)
            builder.addCell(cell);

        return builder.build();
    }

    @Test
    public void testLegacyCellIterator()
    {
        // Creates a table with
        //   - 3 Simple columns: a, c and e
        //   - 2 Complex columns: b and d
        TableMetadata metadata =
            TableMetadata.builder("dummy_ks", "dummy_tbl")
                         .addPartitionKeyColumn("k", BytesType.instance)
                         .addRegularColumn("a", BytesType.instance)
                         .addRegularColumn("b", MapType.getInstance(Int32Type.instance, BytesType.instance, true))
                         .addRegularColumn("c", BytesType.instance)
                         .addRegularColumn("d", MapType.getInstance(Int32Type.instance, BytesType.instance, true))
                         .addRegularColumn("e", BytesType.instance)
                         .build();

        ColumnMetadata a = metadata.getColumn(new ColumnIdentifier("a", false));
        ColumnMetadata b = metadata.getColumn(new ColumnIdentifier("b", false));
        ColumnMetadata c = metadata.getColumn(new ColumnIdentifier("c", false));
        ColumnMetadata d = metadata.getColumn(new ColumnIdentifier("d", false));
        ColumnMetadata e = metadata.getColumn(new ColumnIdentifier("e", false));

        Row row;

        // Row with only simple columns

        row = makeDummyRow(liveCell(a),
                           liveCell(c),
                           liveCell(e));


        assertCellOrder(row.cellsInLegacyOrder(metadata, false),
                        liveCell(a),
                        liveCell(c),
                        liveCell(e));

        assertCellOrder(row.cellsInLegacyOrder(metadata, true),
                        liveCell(e),
                        liveCell(c),
                        liveCell(a));

        // Row with only complex columns

        row = makeDummyRow(liveCell(b, 1),
                           liveCell(b, 2),
                           liveCell(d, 3),
                           liveCell(d, 4));


        assertCellOrder(row.cellsInLegacyOrder(metadata, false),
                        liveCell(b, 1),
                        liveCell(b, 2),
                        liveCell(d, 3),
                        liveCell(d, 4));

        assertCellOrder(row.cellsInLegacyOrder(metadata, true),
                        liveCell(d, 4),
                        liveCell(d, 3),
                        liveCell(b, 2),
                        liveCell(b, 1));

        // Row with mixed simple and complex columns

        row = makeDummyRow(liveCell(a),
                           liveCell(c),
                           liveCell(e),
                           liveCell(b, 1),
                           liveCell(b, 2),
                           liveCell(d, 3),
                           liveCell(d, 4));


        assertCellOrder(row.cellsInLegacyOrder(metadata, false),
                        liveCell(a),
                        liveCell(b, 1),
                        liveCell(b, 2),
                        liveCell(c),
                        liveCell(d, 3),
                        liveCell(d, 4),
                        liveCell(e));

        assertCellOrder(row.cellsInLegacyOrder(metadata, true),
                        liveCell(e),
                        liveCell(d, 4),
                        liveCell(d, 3),
                        liveCell(c),
                        liveCell(b, 2),
                        liveCell(b, 1),
                        liveCell(a));
    }
}