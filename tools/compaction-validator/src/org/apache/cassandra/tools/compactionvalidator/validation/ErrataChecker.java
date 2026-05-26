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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;

/**
 * Walks a pair of partition iterators (legacy vs cursor side, both restricted to the
 * same partition) and decides whether <em>every</em> difference between them is
 * explainable by one or more of the active {@link ErrataRule}s.
 *
 * <p>If yes, the caller treats the partition as a suppressed-but-recorded errata
 * occurrence and continues validation. If no, the partition is a real mismatch and
 * Phase B's {@link PartitionComparator} runs to produce the full report.
 *
 * <p>The matcher is intentionally strict about "covered by a rule": anything that
 * doesn't look <em>exactly</em> like the rule's known signature fails the check, so
 * a fresh divergence with similar-but-different structure still surfaces as a bug.
 */
public final class ErrataChecker
{
    private final Set<ErrataRule> activeRules;

    public ErrataChecker(Set<ErrataRule> activeRules)
    {
        this.activeRules = activeRules == null ? java.util.Collections.emptySet() : activeRules;
    }

    /** @return {@code true} if at least one errata rule is active (worth invoking the matcher) */
    public boolean isActive()
    {
        return !activeRules.isEmpty();
    }

    /**
     * Walks both partition iterators in lockstep and returns the set of rules that
     * fired across all observed differences. An <em>empty</em> result means either
     * (a) the partitions are byte-identical (no work needed) or (b) at least one
     * difference is NOT covered by any active rule — caller must treat this as a
     * real mismatch.
     *
     * <p>Returning a non-empty set means every difference was covered, and the
     * returned rules indicate which one(s) explained the differences. The caller
     * uses this to increment per-rule counters in {@link ValidationStats}.
     *
     * @return rules that fired, or empty when caller should treat as real mismatch
     */
    public Set<ErrataRule> matches(UnfilteredRowIterator legacy, UnfilteredRowIterator cursor)
    {
        if (!isActive())
            return EnumSet.noneOf(ErrataRule.class);

        // Partition-level deletion must match exactly. No rule currently explains a
        // divergence at this level — if it differs, it's a real (and serious) bug.
        if (!deletionEquals(legacy.partitionLevelDeletion(), cursor.partitionLevelDeletion()))
            return EnumSet.noneOf(ErrataRule.class);

        EnumSet<ErrataRule> fired = EnumSet.noneOf(ErrataRule.class);

        // Static row.
        if (!rowsExplainable(legacy.staticRow(), cursor.staticRow(), fired))
            return EnumSet.noneOf(ErrataRule.class);

        // Body unfiltereds. Walk in lockstep; if either side has a different
        // length, no rule currently covers that — caller treats it as real.
        while (legacy.hasNext() && cursor.hasNext())
        {
            Unfiltered lu = legacy.next();
            Unfiltered cu = cursor.next();

            if (lu.kind() != cu.kind())
                return EnumSet.noneOf(ErrataRule.class);

            if (lu.isRow())
            {
                if (!rowsExplainable((Row) lu, (Row) cu, fired))
                    return EnumSet.noneOf(ErrataRule.class);
            }
            else
            {
                // Range tombstone markers etc. — no rule covers these yet. If they
                // match bit-for-bit we're fine; otherwise we can't suppress.
                // PartitionComparator's equality logic is the reference; if it
                // returns non-null we have a non-errata divergence here.
                String diff = PartitionComparator.compareUnfilteredForErrata(lu, cu);
                if (diff != null)
                    return EnumSet.noneOf(ErrataRule.class);
            }
        }
        if (legacy.hasNext() || cursor.hasNext())
            return EnumSet.noneOf(ErrataRule.class);

        return fired;
    }

    // -------------------------------------------------------------------------
    // Row-level
    // -------------------------------------------------------------------------

    private boolean rowsExplainable(Row legacy, Row cursor, EnumSet<ErrataRule> fired)
    {
        boolean lEmpty = legacy == null || legacy.isEmpty();
        boolean cEmpty = cursor == null || cursor.isEmpty();
        if (lEmpty && cEmpty)
            return true;
        if (lEmpty != cEmpty)
            return false; // missing-row mismatch — no rule covers this

        // Clustering, primary-key liveness, and row deletion must match exactly:
        // none of the known errata produces divergence at these levels.
        if (!clusteringEqual(legacy, cursor)) return false;
        if (!livenessEqual(legacy.primaryKeyLivenessInfo(), cursor.primaryKeyLivenessInfo())) return false;
        if (!rowDeletionEqual(legacy.deletion(), cursor.deletion())) return false;

        // ColumnData lockstep. The known cell-value errata only fires when both
        // sides have the same column in the same position — anything else is real.
        Iterator<ColumnData> li = legacy.iterator();
        Iterator<ColumnData> ci = cursor.iterator();
        while (li.hasNext() && ci.hasNext())
        {
            ColumnData lcd = li.next();
            ColumnData ccd = ci.next();
            if (!sameColumn(lcd, ccd))
                return false;
            if (!(lcd instanceof Cell<?>) || !(ccd instanceof Cell<?>))
                return false; // complex / non-Cell column data — no rule covers this
            if (!cellPairExplainable((Cell<?>) lcd, (Cell<?>) ccd, fired))
                return false;
        }
        if (li.hasNext() || ci.hasNext())
            return false; // extra column on one side — no rule covers this

        return true;
    }

    /**
     * For one cell pair, determine whether the difference (if any) is value-only
     * AND covered by at least one active rule. Returns true when the pair matches
     * exactly OR when the value-only difference matches a rule (whose counter we
     * then add to {@code fired}).
     */
    private boolean cellPairExplainable(Cell<?> legacy, Cell<?> cursor, EnumSet<ErrataRule> fired)
    {
        ByteBuffer lv = legacy.buffer();
        ByteBuffer cv = cursor.buffer();
        boolean valuesEqual = byteBufferEquals(lv, cv);
        boolean tsEqual = legacy.timestamp() == cursor.timestamp();
        boolean ttlEqual = legacy.ttl() == cursor.ttl();
        boolean ldtEqual = legacy.localDeletionTime() == cursor.localDeletionTime();
        boolean counterFlagEqual = legacy.isCounterCell() == cursor.isCounterCell();
        boolean pathEqual = cellPathEqual(legacy, cursor);

        // Bit-for-bit equal — no rule fires.
        if (valuesEqual && tsEqual && ttlEqual && ldtEqual && counterFlagEqual && pathEqual)
            return true;

        // Path differences are never explained by a value-only rule.
        if (!pathEqual)
            return false;
        // Any non-value difference disqualifies all current rules — they're all
        // value-only. Add per-rule branches here when future rules cover other
        // kinds of differences.
        if (!tsEqual || !ttlEqual || !ldtEqual || !counterFlagEqual)
            return false;

        // We have a value-only difference. Ask each active rule whether its
        // explainsCellValueDifference covers this signature; the first match wins.
        // (Multiple rules COULD match the same difference, but in practice each
        // rule's predicate is specific enough that only one will fire.)
        for (ErrataRule rule : activeRules)
        {
            if (rule.explainsCellValueDifference(legacy, cursor))
            {
                fired.add(rule);
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Equality helpers (mirror PartitionComparator's so behaviour is consistent)
    // -------------------------------------------------------------------------

    private static boolean deletionEquals(DeletionTime a, DeletionTime b)
    {
        if (a == null && b == null) return true;
        if (a == null) return b.isLive();
        if (b == null) return a.isLive();
        if (a.isLive() && b.isLive()) return true;
        return a.markedForDeleteAt() == b.markedForDeleteAt()
               && a.localDeletionTime() == b.localDeletionTime();
    }

    private static boolean livenessEqual(LivenessInfo a, LivenessInfo b)
    {
        boolean ae = a == null || a.isEmpty();
        boolean be = b == null || b.isEmpty();
        if (ae && be) return true;
        if (ae != be) return false;
        return a.timestamp() == b.timestamp()
               && a.ttl() == b.ttl()
               && a.localExpirationTime() == b.localExpirationTime();
    }

    private static boolean rowDeletionEqual(Row.Deletion a, Row.Deletion b)
    {
        boolean al = a == null || a.isLive();
        boolean bl = b == null || b.isLive();
        if (al && bl) return true;
        if (al != bl) return false;
        return a.isShadowable() == b.isShadowable() && deletionEquals(a.time(), b.time());
    }

    private static boolean clusteringEqual(Row a, Row b)
    {
        if (a.clustering() == null && b.clustering() == null) return true;
        if (a.clustering() == null || b.clustering() == null) return false;
        if (a.clustering().kind() != b.clustering().kind()) return false;
        if (a.clustering().size() != b.clustering().size()) return false;
        Object[] av = a.clustering().getRawValues();
        Object[] bv = b.clustering().getRawValues();
        for (int i = 0; i < a.clustering().size(); i++)
            if (!rawEquals(av[i], bv[i]))
                return false;
        return true;
    }

    private static boolean sameColumn(ColumnData a, ColumnData b)
    {
        // Equality on the columns' metadata. Both sides come from independent
        // TableMetadata objects (different keyspaces), so this checks names + kind
        // + position via ColumnMetadata.equals — which folds in ksName/cfName too.
        // For our cross-keyspace setup they'll differ on ks; fall back to a
        // name+kind+position check that's keyspace-agnostic.
        return a.column().name.equals(b.column().name)
               && a.column().kind == b.column().kind
               && a.column().position() == b.column().position();
    }

    private static boolean byteBufferEquals(ByteBuffer a, ByteBuffer b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static boolean rawEquals(Object a, Object b)
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
        return java.util.Objects.equals(a, b);
    }

    private static boolean cellPathEqual(Cell<?> a, Cell<?> b)
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
}
