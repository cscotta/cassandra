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

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Walks two {@link UnfilteredRowIterator}s in lockstep and produces a detailed
 * {@link MismatchReport} the first time they diverge.
 *
 * <p>This is the Phase B comparator: it is invoked only when {@link PartitionHasher}
 * has flagged a partition as differing between the legacy and cursor sides.  Because
 * it walks each iterator only once and bails out at the first difference, it is
 * still cheap on small mismatches.
 *
 * <p>The comparison covers, in order:
 * <ol>
 *   <li>partition key bytes (sanity check — both sides should have already agreed)</li>
 *   <li>partition-level deletion</li>
 *   <li>static row</li>
 *   <li>each {@link Unfiltered} (rows or range tombstone markers) in clustering order</li>
 *   <li>iterator length (one side ends before the other)</li>
 * </ol>
 *
 * <p>For rows, comparison drills down into clustering, primary-key liveness info,
 * row deletion, and finally each cell (column, value, timestamp, ttl, localDeletionTime,
 * counter flag, and complex path).  The first difference at any level produces the
 * {@link MismatchReport} and ends the walk.
 */
public final class PartitionComparator
{
    private PartitionComparator() {}

    /**
     * Compares two partition iterators in lockstep, returning a populated
     * {@link MismatchReport} as soon as a difference is found.
     *
     * <p>This consumes both iterators up to (and including) the divergence point.
     * Callers should not use either iterator after this call.
     *
     * @param legacy                          the legacy-side partition iterator
     * @param cursor                          the cursor-side partition iterator
     * @param partitionsCheckedBeforeFailure  carry-through from {@link Validator} for
     *                                        reporting how far we got before failing
     * @return a {@link MismatchReport} describing the first observed difference;
     *         never null (the contract is that this method is only called when a
     *         difference is known to exist)
     */
    public static MismatchReport compare(UnfilteredRowIterator legacy,
                                         UnfilteredRowIterator cursor,
                                         long partitionsCheckedBeforeFailure)
    {
        DecoratedKey legacyKey = legacy.partitionKey();
        DecoratedKey cursorKey = cursor.partitionKey();

        if (!legacyKey.equals(cursorKey))
        {
            return build(legacyKey,
                         "partition keys differ: control=" + bytesAsHex(legacyKey.getKey())
                         + " experiment=" + bytesAsHex(cursorKey.getKey()),
                         legacy, cursor,
                         partitionsCheckedBeforeFailure);
        }

        // 1. partition-level deletion
        DeletionTime legacyDel = legacy.partitionLevelDeletion();
        DeletionTime cursorDel = cursor.partitionLevelDeletion();
        if (!deletionEquals(legacyDel, cursorDel))
        {
            return build(legacyKey,
                         "partition-level deletion differs: control=" + describe(legacyDel)
                         + " experiment=" + describe(cursorDel),
                         legacy, cursor,
                         partitionsCheckedBeforeFailure);
        }

        // 2. static row
        Row legacyStatic = legacy.staticRow();
        Row cursorStatic = cursor.staticRow();
        String staticDiff = compareRows(legacyStatic, cursorStatic, "static row");
        if (staticDiff != null)
        {
            return build(legacyKey, staticDiff, legacy, cursor, partitionsCheckedBeforeFailure);
        }

        // 3. body unfiltereds
        long index = 0;
        while (legacy.hasNext() && cursor.hasNext())
        {
            Unfiltered lu = legacy.next();
            Unfiltered cu = cursor.next();
            String d = compareUnfiltered(lu, cu, index);
            if (d != null)
            {
                return build(legacyKey, d, legacy, cursor, partitionsCheckedBeforeFailure);
            }
            index++;
        }

        // 4. length mismatch
        if (legacy.hasNext() || cursor.hasNext())
        {
            String which = legacy.hasNext() ? "control has more unfiltereds" : "experiment has more unfiltereds";
            return build(legacyKey,
                         "iterator length differs after " + index + " unfiltereds: " + which,
                         legacy, cursor,
                         partitionsCheckedBeforeFailure);
        }

        // Both iterators exhausted with no diff. Caller said there was a hash mismatch,
        // so this represents either a hash collision (unlikely but possible) or a real
        // detection bug. Report it explicitly so we never silently lose a divergence.
        return build(legacyKey,
                     "PartitionHasher reported a hash mismatch but cell-by-cell comparison found none "
                     + "(possible hash collision or comparator gap)",
                     legacy, cursor,
                     partitionsCheckedBeforeFailure);
    }

    // ---- Per-element comparators ----------------------------------------

    /**
     * Package-visible bridge so {@link ErrataChecker} can reuse the same per-pair
     * comparison logic when deciding whether a body unfiltered (typically a range
     * tombstone marker) is bit-for-bit equal. Returns {@code null} when equal, or
     * a short description of the difference otherwise. The {@code index} reported
     * inside the description is irrelevant to errata matching — only the null/not-null
     * distinction matters there.
     */
    static String compareUnfilteredForErrata(Unfiltered legacy, Unfiltered cursor)
    {
        return compareUnfiltered(legacy, cursor, 0L);
    }

    private static String compareUnfiltered(Unfiltered legacy, Unfiltered cursor, long index)
    {
        if (legacy.kind() != cursor.kind())
        {
            return "unfiltered kind differs at index " + index + ": control=" + legacy.kind()
                   + " experiment=" + cursor.kind();
        }

        if (legacy.isRow())
            return compareRows((Row) legacy, (Row) cursor, "row at index " + index);

        if (legacy.isRangeTombstoneMarker())
            return compareMarkers((RangeTombstoneMarker) legacy, (RangeTombstoneMarker) cursor, index);

        return null;
    }

    private static String compareRows(Row legacy, Row cursor, String context)
    {
        boolean lEmpty = legacy == null || legacy.isEmpty();
        boolean cEmpty = cursor == null || cursor.isEmpty();
        if (lEmpty && cEmpty)
            return null;
        if (lEmpty != cEmpty)
            return context + ": one side is empty, the other is not (controlEmpty=" + lEmpty + " experimentEmpty=" + cEmpty + ")";

        // Both non-empty here.
        String clusteringDiff = compareClustering(legacy.clustering(), cursor.clustering(), context);
        if (clusteringDiff != null)
            return clusteringDiff;

        // Primary-key liveness
        if (!livenessEquals(legacy.primaryKeyLivenessInfo(), cursor.primaryKeyLivenessInfo()))
        {
            return context + ": primary-key liveness differs: control=" + describe(legacy.primaryKeyLivenessInfo())
                   + " experiment=" + describe(cursor.primaryKeyLivenessInfo());
        }

        // Row deletion
        Row.Deletion ld = legacy.deletion();
        Row.Deletion cd = cursor.deletion();
        if (!rowDeletionEquals(ld, cd))
        {
            return context + ": row deletion differs: control=" + describe(ld) + " experiment=" + describe(cd);
        }

        // ColumnData (cells / complex column data)
        Iterator<ColumnData> li = legacy.iterator();
        Iterator<ColumnData> ci = cursor.iterator();
        int colIndex = 0;
        while (li.hasNext() && ci.hasNext())
        {
            ColumnData lcd = li.next();
            ColumnData ccd = ci.next();
            String d = compareColumnData(lcd, ccd, context + " col[" + colIndex + ']');
            if (d != null)
                return d;
            colIndex++;
        }
        if (li.hasNext() || ci.hasNext())
        {
            String which = li.hasNext() ? "control has extra column" : "experiment has extra column";
            return context + ": " + which + " after " + colIndex + " columns";
        }

        return null;
    }

    private static String compareColumnData(ColumnData legacy, ColumnData cursor, String context)
    {
        // ColumnMetadata.equals folds in keyspace + table name, but our two
        // compaction outputs live in independent keyspaces — so the strict
        // equals would always return false for cross-side cells even when the
        // logical column is identical. Compare on the bits that actually have
        // to match for a comparison to be meaningful (name + kind + position +
        // type). Mirrors ErrataChecker.sameColumn's workaround; both pieces
        // need the same treatment because they run on the same cross-keyspace
        // cell pairs.
        if (!columnsEquivalentAcrossKeyspaces(legacy.column(), cursor.column()))
            return context + ": column metadata differs: control=" + legacy.column().ksName + '.'
                   + legacy.column().cfName + '.' + legacy.column().name
                   + " experiment=" + cursor.column().ksName + '.' + cursor.column().cfName + '.'
                   + cursor.column().name;

        if (legacy instanceof Cell<?> && cursor instanceof Cell<?>)
            return compareCells((Cell<?>) legacy, (Cell<?>) cursor, context);

        if (legacy instanceof ComplexColumnData && cursor instanceof ComplexColumnData)
            return compareComplex((ComplexColumnData) legacy, (ComplexColumnData) cursor, context);

        return context + ": ColumnData subtype differs: control=" + legacy.getClass().getSimpleName()
               + " experiment=" + cursor.getClass().getSimpleName();
    }

    /**
     * Keyspace-agnostic column equivalence. We can't use
     * {@code ColumnMetadata.equals} because it includes keyspace + table, and
     * our two sides live in independent keyspaces (so equals always returns
     * false). Check the bits that actually have to match for the column to be
     * the same column in the schema sense: name, kind, position, and the
     * (in-keyspace-agnostic) type representation.
     */
    private static boolean columnsEquivalentAcrossKeyspaces(ColumnMetadata a,
                                                            ColumnMetadata b)
    {
        return a.name.equals(b.name)
            && a.kind == b.kind
            && a.position() == b.position()
            && a.type.asCQL3Type().toString().equals(b.type.asCQL3Type().toString());
    }

    private static String compareComplex(ComplexColumnData legacy, ComplexColumnData cursor, String context)
    {
        if (!deletionEquals(legacy.complexDeletion(), cursor.complexDeletion()))
            return context + ": complex deletion differs: control=" + describe(legacy.complexDeletion())
                   + " experiment=" + describe(cursor.complexDeletion());

        Iterator<Cell<?>> li = legacy.iterator();
        Iterator<Cell<?>> ci = cursor.iterator();
        int i = 0;
        while (li.hasNext() && ci.hasNext())
        {
            String d = compareCells(li.next(), ci.next(), context + ".cell[" + i + ']');
            if (d != null)
                return d;
            i++;
        }
        if (li.hasNext() || ci.hasNext())
        {
            return context + ": complex cell count differs: control.hasMore=" + li.hasNext()
                   + " experiment.hasMore=" + ci.hasNext() + " after " + i + " cells";
        }
        return null;
    }

    private static String compareCells(Cell<?> legacy, Cell<?> cursor, String context)
    {
        ByteBuffer lv = legacy.buffer();
        ByteBuffer cv = cursor.buffer();
        if (!byteBufferEquals(lv, cv))
            return context + ": cell value differs: control=" + bytesAsHex(lv) + " experiment=" + bytesAsHex(cv);

        if (legacy.timestamp() != cursor.timestamp())
            return context + ": cell timestamp differs: control=" + legacy.timestamp() + " experiment=" + cursor.timestamp();

        if (legacy.ttl() != cursor.ttl())
            return context + ": cell ttl differs: control=" + legacy.ttl() + " experiment=" + cursor.ttl();

        if (legacy.localDeletionTime() != cursor.localDeletionTime())
            return context + ": cell localDeletionTime differs: control=" + legacy.localDeletionTime()
                   + " experiment=" + cursor.localDeletionTime();

        if (legacy.isCounterCell() != cursor.isCounterCell())
            return context + ": isCounterCell differs: control=" + legacy.isCounterCell() + " experiment=" + cursor.isCounterCell();

        if (!cellPathEquals(legacy, cursor))
            return context + ": cell path differs: control=" + describePath(legacy) + " experiment=" + describePath(cursor);

        return null;
    }

    private static String compareMarkers(RangeTombstoneMarker legacy, RangeTombstoneMarker cursor, long index)
    {
        if (legacy.isBoundary() != cursor.isBoundary())
            return "RTM at index " + index + ": isBoundary differs: control=" + legacy.isBoundary()
                   + " experiment=" + cursor.isBoundary();

        String clusteringDiff = compareClustering(legacy.clustering(), cursor.clustering(), "RTM at index " + index);
        if (clusteringDiff != null)
            return clusteringDiff;

        boolean reversed = false; // outputs are forward in compaction
        if (legacy.isBoundary())
        {
            if (!deletionEquals(legacy.openDeletionTime(reversed), cursor.openDeletionTime(reversed)))
                return "RTM at index " + index + ": openDeletionTime differs";
            if (!deletionEquals(legacy.closeDeletionTime(reversed), cursor.closeDeletionTime(reversed)))
                return "RTM at index " + index + ": closeDeletionTime differs";
            if (legacy.openIsInclusive(reversed) != cursor.openIsInclusive(reversed))
                return "RTM at index " + index + ": openIsInclusive differs";
            if (legacy.closeIsInclusive(reversed) != cursor.closeIsInclusive(reversed))
                return "RTM at index " + index + ": closeIsInclusive differs";
        }
        else
        {
            // Bound marker: only one of open/close is meaningful.
            boolean lOpen = legacy.isOpen(reversed);
            boolean cOpen = cursor.isOpen(reversed);
            if (lOpen != cOpen)
                return "RTM at index " + index + ": open/close direction differs: controlOpen=" + lOpen
                       + " experimentOpen=" + cOpen;

            if (lOpen)
            {
                if (!deletionEquals(legacy.openDeletionTime(reversed), cursor.openDeletionTime(reversed)))
                    return "RTM at index " + index + ": openDeletionTime differs";
                if (legacy.openIsInclusive(reversed) != cursor.openIsInclusive(reversed))
                    return "RTM at index " + index + ": openIsInclusive differs";
            }
            else
            {
                if (!deletionEquals(legacy.closeDeletionTime(reversed), cursor.closeDeletionTime(reversed)))
                    return "RTM at index " + index + ": closeDeletionTime differs";
                if (legacy.closeIsInclusive(reversed) != cursor.closeIsInclusive(reversed))
                    return "RTM at index " + index + ": closeIsInclusive differs";
            }
        }
        return null;
    }

    private static String compareClustering(ClusteringPrefix<?> legacy, ClusteringPrefix<?> cursor, String context)
    {
        if (legacy == null && cursor == null) return null;
        if (legacy == null || cursor == null)
            return context + ": one clustering is null (control=" + legacy + " experiment=" + cursor + ')';

        if (legacy.kind() != cursor.kind())
            return context + ": clustering kind differs: control=" + legacy.kind() + " experiment=" + cursor.kind();
        if (legacy.size() != cursor.size())
            return context + ": clustering size differs: control=" + legacy.size() + " experiment=" + cursor.size();

        Object[] lv = legacy.getRawValues();
        Object[] cv = cursor.getRawValues();
        for (int i = 0; i < legacy.size(); i++)
        {
            Object a = lv[i];
            Object b = cv[i];
            if (!rawValueEquals(a, b))
                return context + ": clustering[" + i + "] differs: control=" + describeRaw(a) + " experiment=" + describeRaw(b);
        }
        return null;
    }

    // ---- Equality helpers ------------------------------------------------

    private static boolean deletionEquals(DeletionTime a, DeletionTime b)
    {
        if (a == null && b == null) return true;
        if (a == null) return b.isLive();
        if (b == null) return a.isLive();
        if (a.isLive() && b.isLive()) return true;
        return a.markedForDeleteAt() == b.markedForDeleteAt()
               && a.localDeletionTime() == b.localDeletionTime();
    }

    private static boolean livenessEquals(LivenessInfo a, LivenessInfo b)
    {
        boolean ae = a == null || a.isEmpty();
        boolean be = b == null || b.isEmpty();
        if (ae && be) return true;
        if (ae != be) return false;
        return a.timestamp() == b.timestamp()
               && a.ttl() == b.ttl()
               && a.localExpirationTime() == b.localExpirationTime();
    }

    private static boolean rowDeletionEquals(Row.Deletion a, Row.Deletion b)
    {
        boolean al = a == null || a.isLive();
        boolean bl = b == null || b.isLive();
        if (al && bl) return true;
        if (al != bl) return false;
        return a.isShadowable() == b.isShadowable() && deletionEquals(a.time(), b.time());
    }

    private static boolean cellPathEquals(Cell<?> a, Cell<?> b)
    {
        var pa = a.path();
        var pb = b.path();
        if (pa == null && pb == null) return true;
        if (pa == null || pb == null) return false;
        if (pa.size() != pb.size()) return false;
        for (int i = 0; i < pa.size(); i++)
            if (!byteBufferEquals(pa.get(i), pb.get(i)))
                return false;
        return true;
    }

    private static boolean byteBufferEquals(ByteBuffer a, ByteBuffer b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static boolean rawValueEquals(Object a, Object b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof ByteBuffer && b instanceof ByteBuffer)
            return ((ByteBuffer) a).equals(b);
        if (a instanceof byte[] && b instanceof byte[])
        {
            byte[] aa = (byte[]) a;
            byte[] bb = (byte[]) b;
            if (aa.length != bb.length) return false;
            for (int i = 0; i < aa.length; i++)
                if (aa[i] != bb[i]) return false;
            return true;
        }
        // Cross-type: normalize through ByteBuffer
        ByteBuffer aBuf = toBuf(a);
        ByteBuffer bBuf = toBuf(b);
        if (aBuf == null || bBuf == null) return a.equals(b);
        return aBuf.equals(bBuf);
    }

    private static ByteBuffer toBuf(Object o)
    {
        if (o == null) return null;
        if (o instanceof ByteBuffer) return (ByteBuffer) o;
        if (o instanceof byte[]) return ByteBuffer.wrap((byte[]) o);
        return null;
    }

    // ---- Pretty-printing ------------------------------------------------

    private static MismatchReport build(DecoratedKey key,
                                        String description,
                                        UnfilteredRowIterator legacy,
                                        UnfilteredRowIterator cursor,
                                        long partitionsCheckedBeforeFailure)
    {
        return new MismatchReport(bytesAsHex(key.getKey()),
                                  description,
                                  partitionDump("legacy", legacy),
                                  partitionDump("cursor", cursor),
                                  partitionsCheckedBeforeFailure);
    }

    private static String partitionDump(String label, UnfilteredRowIterator iter)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append('[').append(label).append("] partitionKey=").append(bytesAsHex(iter.partitionKey().getKey())).append('\n');
        sb.append('[').append(label).append("] partitionLevelDeletion=").append(describe(iter.partitionLevelDeletion())).append('\n');
        Row staticRow = iter.staticRow();
        if (staticRow != null && !staticRow.isEmpty())
            sb.append('[').append(label).append("] staticRow=").append(staticRow.toString(iter.metadata(), true)).append('\n');
        // The iterator may be partially consumed at this point (we hit a divergence
        // mid-walk), so only render up to a small remaining tail to avoid runaway output.
        int remaining = 0;
        while (iter.hasNext() && remaining < 64)
        {
            Unfiltered u = iter.next();
            sb.append('[').append(label).append("] ").append(u.toString(iter.metadata(), true)).append('\n');
            remaining++;
        }
        if (iter.hasNext())
            sb.append('[').append(label).append("] (truncated remaining unfiltereds)\n");
        return sb.toString();
    }

    private static String describe(DeletionTime dt)
    {
        if (dt == null) return "null";
        return dt.isLive() ? "LIVE" : ("deletedAt=" + dt.markedForDeleteAt() + ",localDeletion=" + dt.localDeletionTime());
    }

    private static String describe(LivenessInfo li)
    {
        if (li == null || li.isEmpty()) return "EMPTY";
        return "ts=" + li.timestamp() + ",ttl=" + li.ttl() + ",localExpiration=" + li.localExpirationTime();
    }

    private static String describe(Row.Deletion d)
    {
        if (d == null || d.isLive()) return "LIVE";
        return "shadowable=" + d.isShadowable() + ",time=" + describe(d.time());
    }

    private static String describePath(Cell<?> cell)
    {
        if (cell.path() == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cell.path().size(); i++)
        {
            if (i > 0) sb.append(',');
            sb.append(bytesAsHex(cell.path().get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String describeRaw(Object o)
    {
        if (o == null) return "null";
        if (o instanceof ByteBuffer) return bytesAsHex((ByteBuffer) o);
        if (o instanceof byte[]) return bytesAsHex(ByteBuffer.wrap((byte[]) o));
        return String.valueOf(o);
    }

    private static String bytesAsHex(ByteBuffer buf)
    {
        if (buf == null) return "null";
        return ByteBufferUtil.bytesToHex(buf);
    }
}
