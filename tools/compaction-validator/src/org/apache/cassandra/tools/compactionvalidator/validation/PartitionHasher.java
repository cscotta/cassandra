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
package org.apache.cassandra.tools.compactionvalidator.validation;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Computes a 64-bit content hash of a single partition for fast equality comparison
 * between two compaction outputs.
 *
 * <p>Phase A of validation walks both sides in lockstep, hashes each partition with
 * this class, and only falls back to the (much slower) {@link PartitionComparator}
 * when the hashes differ.  Hash collisions therefore weaken detection but never
 * cause false positives, since Phase B re-runs the comparison cell-by-cell.
 *
 * <p>The hash is computed by serialising every observable property of the partition
 * — partition key, partition-level deletion, static row, ordered list of unfiltereds —
 * into a single byte array using a fixed format and then running xxhash64 over it.
 * The format is internal to this validator: it does <em>not</em> need to match
 * Cassandra's on-wire encoding, only to produce different bytes for any two
 * partitions that differ in any observable way.
 *
 * <p>The seed {@link #SEED} is fixed so that the same partition iterator always
 * yields the same 64-bit hash regardless of run.
 */
public final class PartitionHasher
{
    /** Fixed xxhash64 seed; deterministic across runs. */
    public static final long SEED = 0xC0FFEEC0FFEEL;

    private static final XXHash64 HASH = XXHashFactory.fastestInstance().hash64();

    // ---- Section markers --------------------------------------------------
    // We prefix each logical section with a single distinguishing byte so that
    // adjacent sections cannot accidentally collide if their contents pun.

    private static final byte TAG_PARTITION_KEY        = 0x01;
    private static final byte TAG_PARTITION_DELETION   = 0x02;
    private static final byte TAG_STATIC_ROW           = 0x03;
    private static final byte TAG_ROW                  = 0x04;
    private static final byte TAG_RTM_BOUND            = 0x05;
    private static final byte TAG_RTM_BOUNDARY         = 0x06;
    private static final byte TAG_ROW_LIVENESS         = 0x07;
    private static final byte TAG_ROW_DELETION         = 0x08;
    private static final byte TAG_CELL                 = 0x09;
    private static final byte TAG_CLUSTERING           = 0x0A;
    private static final byte TAG_END_PARTITION        = 0x0B;

    private PartitionHasher() {}

    /**
     * Computes the xxhash64 of {@code partition}.
     *
     * <p>This consumes the iterator: callers that need to inspect the partition
     * after hashing must obtain a fresh iterator (e.g. by re-opening a scanner).
     *
     * @param partition the partition to hash; must not be null
     * @return the 64-bit content hash
     */
    public static long hashPartition(UnfilteredRowIterator partition)
    {
        return hashPartitionWithRowCount(partition).hash;
    }

    /**
     * Same as {@link #hashPartition(UnfilteredRowIterator)} but also returns the count of
     * rows observed in the partition (excludes range tombstone markers; includes the static row).
     */
    public static HashAndRowCount hashPartitionWithRowCount(UnfilteredRowIterator partition)
    {
        if (partition == null)
            throw new IllegalArgumentException("partition must not be null");

        long rowCount = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        try (DataOutputStream out = new DataOutputStream(baos))
        {
            // Partition key bytes
            out.writeByte(TAG_PARTITION_KEY);
            writeByteBuffer(out, partition.partitionKey().getKey());

            // Partition-level deletion
            out.writeByte(TAG_PARTITION_DELETION);
            writeDeletionTime(out, partition.partitionLevelDeletion());

            // Static row (may be empty)
            Row staticRow = partition.staticRow();
            if (staticRow != null && !staticRow.isEmpty())
            {
                out.writeByte(TAG_STATIC_ROW);
                writeRow(out, staticRow);
                rowCount++;
            }

            // Body unfiltereds
            while (partition.hasNext())
            {
                Unfiltered u = partition.next();
                if (u.isRow())
                {
                    out.writeByte(TAG_ROW);
                    writeRow(out, (Row) u);
                    rowCount++;
                }
                else if (u.isRangeTombstoneMarker())
                {
                    writeRangeTombstoneMarker(out, (RangeTombstoneMarker) u);
                }
                else
                {
                    // Future-proof: tag and serialize via clustering only.
                    out.writeByte(TAG_ROW);
                    writeClustering(out, u.clustering());
                    rowCount++;
                }
            }

            out.writeByte(TAG_END_PARTITION);
        }
        catch (IOException e)
        {
            // ByteArrayOutputStream cannot throw IOException; this is unreachable.
            throw new UncheckedIOException(e);
        }

        byte[] bytes = baos.toByteArray();
        long hash = HASH.hash(bytes, 0, bytes.length, SEED);
        return new HashAndRowCount(hash, rowCount);
    }

    /** Hash + row-count pair returned by {@link #hashPartitionWithRowCount}. */
    public static final class HashAndRowCount
    {
        public final long hash;
        public final long rowCount;

        public HashAndRowCount(long hash, long rowCount)
        {
            this.hash = hash;
            this.rowCount = rowCount;
        }
    }

    // ---- Writers ----------------------------------------------------------

    private static void writeByteBuffer(DataOutputStream out, ByteBuffer buf) throws IOException
    {
        if (buf == null)
        {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = ByteBufferUtil.getArray(buf);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static void writeUtf8(DataOutputStream out, String s) throws IOException
    {
        if (s == null)
        {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static void writeDeletionTime(DataOutputStream out, DeletionTime dt) throws IOException
    {
        if (dt == null || dt.isLive())
        {
            out.writeBoolean(false); // not present
            return;
        }
        out.writeBoolean(true);
        out.writeLong(dt.markedForDeleteAt());
        out.writeLong(dt.localDeletionTime());
    }

    private static void writeLivenessInfo(DataOutputStream out, LivenessInfo li) throws IOException
    {
        if (li == null || li.isEmpty())
        {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeLong(li.timestamp());
        out.writeInt(li.ttl());
        out.writeLong(li.localExpirationTime());
    }

    private static void writeClustering(DataOutputStream out, ClusteringPrefix<?> cp) throws IOException
    {
        out.writeByte(TAG_CLUSTERING);
        if (cp == null)
        {
            out.writeInt(-1);
            return;
        }
        out.writeUTF(cp.kind().name());
        out.writeInt(cp.size());
        Object[] raw = cp.getRawValues();
        for (int i = 0; i < cp.size(); i++)
        {
            Object v = raw[i];
            if (v == null)
            {
                out.writeInt(-1);
            }
            else if (v instanceof ByteBuffer)
            {
                writeByteBuffer(out, (ByteBuffer) v);
            }
            else if (v instanceof byte[])
            {
                byte[] arr = (byte[]) v;
                out.writeInt(arr.length);
                out.write(arr);
            }
            else
            {
                // Fallback: stringify
                writeUtf8(out, v.toString());
            }
        }
    }

    private static void writeRow(DataOutputStream out, Row row) throws IOException
    {
        writeClustering(out, row.clustering());

        // Primary-key liveness
        out.writeByte(TAG_ROW_LIVENESS);
        writeLivenessInfo(out, row.primaryKeyLivenessInfo());

        // Row deletion
        out.writeByte(TAG_ROW_DELETION);
        Row.Deletion rowDel = row.deletion();
        if (rowDel == null || rowDel.isLive())
        {
            out.writeBoolean(false);
        }
        else
        {
            out.writeBoolean(true);
            out.writeBoolean(rowDel.isShadowable());
            writeDeletionTime(out, rowDel.time());
        }

        // Cells (and complex deletions, conservatively just hash the cell stream).
        // We iterate ColumnData so we can include complex deletions.
        for (ColumnData cd : row)
        {
            writeColumnData(out, cd);
        }
    }

    private static void writeColumnData(DataOutputStream out, ColumnData cd) throws IOException
    {
        out.writeByte(TAG_CELL);
        // Column identity
        writeUtf8(out, cd.column().name.toString());
        writeUtf8(out, cd.column().type.asCQL3Type().toString());

        if (cd instanceof Cell<?>)
        {
            writeCell(out, (Cell<?>) cd);
        }
        else if (cd instanceof ComplexColumnData)
        {
            ComplexColumnData ccd = (ComplexColumnData) cd;
            // Hash the complex deletion, then walk inner cells.
            out.writeByte(TAG_ROW_DELETION);
            writeDeletionTime(out, ccd.complexDeletion());
            for (Cell<?> cell : ccd)
                writeCell(out, cell);
        }
        else
        {
            // Unknown ColumnData subclass; tag the type name only so the hash differs
            // from a recognised case while remaining deterministic.
            writeUtf8(out, cd.getClass().getName());
        }
    }

    private static void writeCell(DataOutputStream out, Cell<?> cell) throws IOException
    {
        out.writeByte(TAG_CELL);
        writeUtf8(out, cell.column().name.toString());
        writeByteBuffer(out, cell.buffer());
        out.writeLong(cell.timestamp());
        out.writeInt(cell.ttl());
        out.writeLong(cell.localDeletionTime());
        out.writeBoolean(cell.isCounterCell());
        // Path (for complex columns)
        if (cell.path() != null)
        {
            out.writeBoolean(true);
            int sz = cell.path().size();
            out.writeInt(sz);
            for (int i = 0; i < sz; i++)
                writeByteBuffer(out, cell.path().get(i));
        }
        else
        {
            out.writeBoolean(false);
        }
    }

    private static void writeRangeTombstoneMarker(DataOutputStream out, RangeTombstoneMarker marker) throws IOException
    {
        if (marker.isBoundary())
        {
            out.writeByte(TAG_RTM_BOUNDARY);
            writeClustering(out, marker.clustering());
            // Both directions for forward-iterating boundary
            writeDeletionTime(out, marker.openDeletionTime(false));
            writeDeletionTime(out, marker.closeDeletionTime(false));
            out.writeBoolean(marker.openIsInclusive(false));
            out.writeBoolean(marker.closeIsInclusive(false));
        }
        else
        {
            out.writeByte(TAG_RTM_BOUND);
            writeClustering(out, marker.clustering());
            boolean isOpen = marker.isOpen(false);
            out.writeBoolean(isOpen);
            if (isOpen)
            {
                writeDeletionTime(out, marker.openDeletionTime(false));
                out.writeBoolean(marker.openIsInclusive(false));
            }
            else
            {
                writeDeletionTime(out, marker.closeDeletionTime(false));
                out.writeBoolean(marker.closeIsInclusive(false));
            }
        }
    }
}
