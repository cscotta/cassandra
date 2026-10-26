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

package org.apache.cassandra.db;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The single-leaf purge path must behave exactly like the generic {@code transformAndFilter} path it replaces,
 * for every combination of expired, tombstoned, shadowed and live column data.
 */
public class BTreeRowPurgeTest
{
    private static final long NOW = 1000;
    private static final long TS = 500;
    private static final int COLUMN_COUNT = 40;

    private static final TableMetadata metadata;
    private static final ColumnMetadata[] intColumns = new ColumnMetadata[COLUMN_COUNT];
    private static final ColumnMetadata mapColumn;
    private static final Clustering<?> clustering;

    static
    {
        TableMetadata.Builder builder = TableMetadata.builder("ks", "tbl")
                                                     .addPartitionKeyColumn("k", IntegerType.instance)
                                                     .addClusteringColumn("c", IntegerType.instance);
        // zero-padded names so that column order (which the sorted builder requires) matches the array order
        for (int i = 0; i < COLUMN_COUNT; i++)
            builder.addRegularColumn(String.format("v%02d", i), IntegerType.instance);
        builder.addRegularColumn("m", MapType.getInstance(IntegerType.instance, IntegerType.instance, true));
        metadata = builder.build();
        for (int i = 0; i < COLUMN_COUNT; i++)
            intColumns[i] = metadata.getColumn(new ColumnIdentifier(String.format("v%02d", i), false));
        mapColumn = metadata.getColumn(new ColumnIdentifier("m", false));
        clustering = metadata.comparator.make(BigInteger.valueOf(1));
    }

    private static final ByteBuffer VAL = ByteBufferUtil.bytes(10);
    private static final ByteBuffer KEY1 = ByteBufferUtil.bytes(1);
    private static final ByteBuffer KEY2 = ByteBufferUtil.bytes(2);

    /** The implementation {@link Row#purge} used before the single-leaf fast path. */
    private static Row purgeViaGenericTransform(Row row, DeletionPurger purger, long nowInSec, boolean enforceStrictLiveness)
    {
        if (!row.hasDeletion(nowInSec))
            return row;

        LivenessInfo newInfo = purger.shouldPurge(row.primaryKeyLivenessInfo(), nowInSec) ? LivenessInfo.EMPTY : row.primaryKeyLivenessInfo();
        Row.Deletion newDeletion = purger.shouldPurge(row.deletion().time()) ? Row.Deletion.LIVE : row.deletion();
        if (enforceStrictLiveness && newDeletion.isLive() && newInfo.isEmpty())
            return null;
        return row.transformAndFilter(newInfo, newDeletion, cd -> cd.purge(purger, nowInSec));
    }

    private static void assertSameResult(Row row, DeletionPurger purger, long nowInSec)
    {
        for (boolean enforceStrictLiveness : new boolean[]{ false, true })
        {
            Row expected = purgeViaGenericTransform(row, purger, nowInSec, enforceStrictLiveness);
            Row actual = row.purge(purger, nowInSec, enforceStrictLiveness);
            if (expected == null)
                assertNull("purge(" + enforceStrictLiveness + ") should have dropped the row", actual);
            else
                assertEquals("purge(" + enforceStrictLiveness + ") diverged from the generic transform", expected, actual);
        }
    }

    private static Row.Builder sortedBuilder(LivenessInfo info, Row.Deletion deletion)
    {
        Row.Builder builder = BTreeRow.sortedBuilder();
        builder.newRow(clustering);
        if (!info.isEmpty())
            builder.addPrimaryKeyLivenessInfo(info);
        if (!deletion.isLive())
            builder.addRowDeletion(deletion);
        return builder;
    }

    private static Row.Builder sortedBuilder()
    {
        return sortedBuilder(LivenessInfo.EMPTY, Row.Deletion.LIVE);
    }

    private static Row purgeAndAssert(Row row, DeletionPurger purger)
    {
        assertSameResult(row, purger, NOW);
        return row.purge(purger, NOW, false);
    }

    private static List<ColumnMetadata> columns(Row row)
    {
        List<ColumnMetadata> columns = new ArrayList<>();
        for (ColumnData cd : row)
            columns.add(cd.column());
        return columns;
    }

    @Test
    public void testRowWithoutDeletionsIsReturnedUnchanged()
    {
        Row.Builder builder = sortedBuilder(LivenessInfo.create(TS), Row.Deletion.LIVE);
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        builder.addCell(BufferCell.live(intColumns[1], TS, VAL));
        Row row = builder.build();

        assertSame(row, row.purge(DeletionPurger.PURGE_ALL, NOW, false));
        assertSameResult(row, DeletionPurger.PURGE_ALL, NOW);
    }

    @Test
    public void testExpiredCellsAreDroppedAndOrderPreserved()
    {
        Row.Builder builder = sortedBuilder();
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        // written with a 1 s TTL at NOW - 100, i.e. already expired
        builder.addCell(BufferCell.expiring(intColumns[1], TS, 1, NOW - 100, VAL));
        builder.addCell(BufferCell.live(intColumns[2], TS, VAL));
        builder.addCell(BufferCell.expiring(intColumns[3], TS, 1, NOW - 100, VAL));
        builder.addCell(BufferCell.live(intColumns[4], TS, VAL));
        Row row = builder.build();

        Row purged = purgeAndAssert(row, DeletionPurger.PURGE_ALL);

        assertNotNull(purged);
        assertEquals(Arrays.asList(intColumns[0], intColumns[2], intColumns[4]), columns(purged));
    }

    @Test
    public void testExpiredCellThatIsNotPurgeableBecomesATombstone()
    {
        Row.Builder builder = sortedBuilder();
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        builder.addCell(BufferCell.expiring(intColumns[1], TS, 1, NOW - 100, VAL));
        builder.addCell(BufferCell.live(intColumns[2], TS, VAL));
        Row row = builder.build();

        // nothing is old enough to purge: the expired cell must survive as a tombstone
        DeletionPurger keepAll = (timestamp, localDeletionTime) -> false;
        Row purged = purgeAndAssert(row, keepAll);

        assertNotNull(purged);
        assertEquals(Arrays.asList(intColumns[0], intColumns[1], intColumns[2]), columns(purged));
        Iterator<ColumnData> iter = purged.iterator();
        iter.next();
        Cell<?> expired = (Cell<?>) iter.next();
        assertTrue(expired.isTombstone());
        assertEquals(intColumns[1], expired.column());
    }

    @Test
    public void testAllCellsPurgedDropsTheRow()
    {
        Row.Builder builder = sortedBuilder();
        builder.addCell(BufferCell.tombstone(intColumns[0], TS, NOW - 100));
        builder.addCell(BufferCell.expiring(intColumns[1], TS, 1, NOW - 100, VAL));
        Row row = builder.build();

        assertNull(purgeAndAssert(row, DeletionPurger.PURGE_ALL));
    }

    @Test
    public void testRowDeletionIsPurgedAndLiveLivenessIsRetained()
    {
        LivenessInfo liveness = LivenessInfo.create(TS);
        Row.Builder builder = sortedBuilder(liveness, Row.Deletion.regular(DeletionTime.build(TS - 10, NOW - 100)));
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        Row row = builder.build();

        Row purged = purgeAndAssert(row, (timestamp, localDeletionTime) -> localDeletionTime < NOW);
        assertNotNull(purged);
        assertTrue(purged.deletion().isLive());
        assertEquals(liveness, purged.primaryKeyLivenessInfo());
        assertEquals(Arrays.asList(intColumns[0]), columns(purged));
    }

    @Test
    public void testLiveRowDeletionIsRetained()
    {
        Row.Builder builder = sortedBuilder(LivenessInfo.create(TS), Row.Deletion.regular(DeletionTime.build(TS, NOW)));
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        Row row = builder.build();

        Row purged = purgeAndAssert(row, (timestamp, localDeletionTime) -> false);
        assertNotNull(purged);
        assertFalse(purged.deletion().isLive());
    }

    @Test
    public void testEnforceStrictLivenessDropsTheRow()
    {
        Row.Builder builder = sortedBuilder(LivenessInfo.EMPTY, Row.Deletion.regular(DeletionTime.build(TS - 10, NOW - 100)));
        builder.addCell(BufferCell.tombstone(intColumns[0], TS, NOW));
        Row row = builder.build();

        // the tombstone cell is not purgeable, so without strict liveness the row survives its purged deletion
        DeletionPurger purgeRowDeletion = (timestamp, localDeletionTime) -> localDeletionTime < NOW;
        assertNull(row.purge(purgeRowDeletion, NOW, true));
        Row kept = row.purge(purgeRowDeletion, NOW, false);
        assertNotNull(kept);
        assertTrue(kept.deletion().isLive());
        assertEquals(Arrays.asList(intColumns[0]), columns(kept));
        assertSameResult(row, purgeRowDeletion, NOW);
    }

    @Test
    public void testComplexColumnPurge()
    {
        Row.Builder builder = sortedBuilder();
        builder.addCell(BufferCell.live(intColumns[0], TS, VAL));
        builder.addComplexDeletion(mapColumn, DeletionTime.build(TS, NOW - 100));
        builder.addCell(BufferCell.tombstone(mapColumn, TS, NOW - 100, CellPath.create(KEY1)));
        builder.addCell(BufferCell.expiring(mapColumn, TS, 1, NOW - 100, VAL, CellPath.create(KEY2)));
        builder.addCell(BufferCell.live(intColumns[1], TS, VAL));
        Row row = builder.build();

        Row purged = purgeAndAssert(row, DeletionPurger.PURGE_ALL);
        assertNotNull(purged);
        assertEquals(Arrays.asList(intColumns[0], intColumns[1]), columns(purged));
    }

    @Test
    public void testMultiNodeTreeStillMatchesGenericTransform()
    {
        // more columns than a single leaf holds, so the row's b-tree has a branch node
        Row.Builder builder = sortedBuilder(LivenessInfo.create(TS), Row.Deletion.regular(DeletionTime.build(TS - 10, NOW - 100)));
        for (int i = 0; i < COLUMN_COUNT; i++)
        {
            if (i % 7 == 3)
                builder.addCell(BufferCell.tombstone(intColumns[i], TS, NOW - 100));
            else if (i % 5 == 0)
                builder.addCell(BufferCell.expiring(intColumns[i], TS, 1, NOW - 100, VAL));
            else
                builder.addCell(BufferCell.live(intColumns[i], TS, VAL));
        }
        Row row = builder.build();

        Row purged = purgeAndAssert(row, DeletionPurger.PURGE_ALL);
        assertNotNull(purged);
        List<ColumnMetadata> remaining = columns(purged);
        assertTrue("expected the purged columns to be dropped", remaining.size() < COLUMN_COUNT);
        for (int i = 0; i < remaining.size(); i++)
            assertEquals(intColumns[expectedIndex(i)], remaining.get(i));
    }

    /** Index of the i-th column that the multi-node test expects to survive purging. */
    private static int expectedIndex(int i)
    {
        int kept = -1;
        for (int c = 0; c < COLUMN_COUNT; c++)
        {
            boolean purged = c % 7 == 3 || c % 5 == 0;
            if (!purged && ++kept == i)
                return c;
        }
        throw new AssertionError("no such surviving column: " + i);
    }

    /**
     * Differential test against the generic transform over the interesting cell shapes and leaf sizes, so the
     * fast path cannot diverge on a shape that a hand-written expectation happens to miss.
     */
    @Test
    public void testPurgeMatchesGenericTransformForManyShapes()
    {
        for (int columnCount = 1; columnCount <= COLUMN_COUNT; columnCount++)
        {
            for (int shape = 0; shape < 5; shape++)
            {
                Row.Builder builder = sortedBuilder(shape < 2 ? LivenessInfo.EMPTY : LivenessInfo.create(TS),
                                                    shape % 2 == 0 ? Row.Deletion.LIVE : Row.Deletion.regular(DeletionTime.build(TS - 10, NOW - 100)));
                for (int i = 0; i < columnCount; i++)
                {
                    switch (shape)
                    {
                        case 0:
                            builder.addCell(BufferCell.live(intColumns[i], TS, VAL));
                            break;
                        case 1:
                            builder.addCell((i % 3 == 0) ? BufferCell.tombstone(intColumns[i], TS, NOW - 100)
                                                         : BufferCell.live(intColumns[i], TS, VAL));
                            break;
                        case 2:
                            builder.addCell((i % 2 == 0) ? BufferCell.expiring(intColumns[i], TS, 1, NOW - 100, VAL)
                                                         : BufferCell.expiring(intColumns[i], TS, 100, NOW - 100, VAL));
                            break;
                        case 3:
                            builder.addCell((i % 4 == 0) ? BufferCell.expiring(intColumns[i], TS, 1, NOW - 100, VAL)
                                                         : BufferCell.tombstone(intColumns[i], TS, NOW - 100));
                            break;
                        default:
                            builder.addCell(BufferCell.tombstone(intColumns[i], TS, i % 2 == 0 ? NOW - 100 : NOW));
                            break;
                    }
                }

                Row row = builder.build();
                // purge only the old data, so some shapes keep live and expired-but-unpurgeable cells
                DeletionPurger purger = (timestamp, localDeletionTime) -> localDeletionTime < NOW;
                assertSameResult(row, purger, NOW);
                assertSameResult(row, DeletionPurger.PURGE_ALL, NOW);
            }
        }
    }
}
