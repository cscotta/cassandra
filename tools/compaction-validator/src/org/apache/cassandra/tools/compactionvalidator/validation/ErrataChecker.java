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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
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
        this.activeRules = activeRules == null ? Collections.emptySet() : activeRules;
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
        // length, the extra unfiltereds may be the bug-1B body-row variant
        // (same mechanism as the static-row case: legacy's same-TS tombstones
        // collapse the body row to {@code null} via Row.Merger, cursor's bug-1B
        // keeps an expiring cell that survives as an extra row). We consult
        // the same {@code explainsResurrectedRow} matcher on each leftover
        // cursor row; if it matches, suppress and keep walking.
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
        // Asymmetric end-of-iteration. Cursor-has-more is the bug-1B body-row
        // direction; for each leftover Row on the cursor side ask if it matches
        // the resurrected-row signature.
        while (cursor.hasNext() && !legacy.hasNext())
        {
            Unfiltered cu = cursor.next();
            if (!cu.isRow())
                return EnumSet.noneOf(ErrataRule.class);
            if (!resurrectedRowExplainable((Row) cu, fired))
                return EnumSet.noneOf(ErrataRule.class);
        }
        // Legacy-has-more (cursor lost data) has no known signature — surface as
        // real mismatch even with rules active.
        if (legacy.hasNext())
            return EnumSet.noneOf(ErrataRule.class);

        return fired;
    }

    /**
     * Returns {@code true} when {@code cursorRow} matches the
     * {@code explainsResurrectedRow} signature of at least one active rule.
     * Used by both the static-row asymmetry check inside
     * {@link #rowsExplainable} and the body-row asymmetry check at the tail of
     * {@link #matches}.
     */
    private boolean resurrectedRowExplainable(Row cursorRow, EnumSet<ErrataRule> fired)
    {
        for (ErrataRule rule : activeRules)
        {
            if (rule.explainsResurrectedRow(cursorRow))
            {
                fired.add(rule);
                return true;
            }
        }
        return false;
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
        {
            // Asymmetric: one side has the row, the other doesn't. The cursor-
            // has-more direction can match bug 1B's static-row signature:
            // legacy's same-TS tombstones collapse the row to empty via
            // Row.Merger, while cursor's bug-1B keeps an expiring cell that
            // survives. Consult per-rule matchers; if any matches, suppress.
            //
            // The cursor-has-less direction (lEmpty=false, cEmpty=true) would
            // mean cursor lost data the legacy side kept — there's no known
            // cursor-compaction bug with that signature, so it stays a real
            // failure.
            if (lEmpty && !cEmpty)
            {
                if (resurrectedRowExplainable(cursor, fired))
                    return true;
            }
            return false; // unsuppressed asymmetry — real failure
        }

        // Clustering, primary-key liveness, and row deletion must match exactly:
        // none of the known errata produces divergence at these levels.
        if (!clusteringEqual(legacy, cursor)) return false;
        if (!livenessEqual(legacy.primaryKeyLivenessInfo(), cursor.primaryKeyLivenessInfo())) return false;
        if (!rowDeletionEqual(legacy.deletion(), cursor.deletion())) return false;

        // ColumnData merge-walk. Lockstep would bail at the first column-metadata
        // mismatch — but bug 1B can produce an asymmetric row where cursor has
        // an extra column (same root cause as the row-asymmetry path: legacy's
        // same-TS tombstone correctly won and {@code Row.Merger} pruned the
        // cell from the row body, while cursor's bug-1B kept the expiring
        // cell). Walk both iterators ordered by column name so we can consult
        // the per-cell resurrected matcher on the extras.
        Iterator<ColumnData> li = legacy.iterator();
        Iterator<ColumnData> ci = cursor.iterator();
        ColumnData lcd = li.hasNext() ? li.next() : null;
        ColumnData ccd = ci.hasNext() ? ci.next() : null;
        while (lcd != null || ccd != null)
        {
            // Complex / non-Cell column data isn't covered by any rule.
            if (lcd != null && !(lcd instanceof Cell<?>))
                return false;
            if (ccd != null && !(ccd instanceof Cell<?>))
                return false;

            int cmp;
            if (lcd == null)
                cmp = 1;       // legacy exhausted → cursor's column is the "extra"
            else if (ccd == null)
                cmp = -1;      // cursor exhausted → legacy has a column cursor doesn't
            else
                cmp = compareColumnsByName(lcd, ccd);

            if (cmp == 0)
            {
                // Same column on both sides → normal pair comparison.
                if (!cellPairExplainable((Cell<?>) lcd, (Cell<?>) ccd, fired))
                    return false;
                lcd = li.hasNext() ? li.next() : null;
                ccd = ci.hasNext() ? ci.next() : null;
            }
            else if (cmp < 0)
            {
                // Legacy has a column cursor doesn't — cursor-lost-data
                // direction has no known bug-1B variant.
                return false;
            }
            else
            {
                // Cursor has a column legacy doesn't — could be the bug-1B
                // resurrected-cell signature. Consult per-cell matchers.
                if (!cellExplainsResurrection((Cell<?>) ccd, fired))
                    return false;
                ccd = ci.hasNext() ? ci.next() : null;
            }
        }

        return true;
    }

    /**
     * Position-style ordering helper: returns the comparison result for the two
     * cells' column names, keyspace-agnostic. Both sides come from independent
     * {@code TableMetadata} objects (different keyspaces), so we can't rely on
     * {@code ColumnMetadata.compareTo} which folds keyspace into the comparison.
     * Within a single row, columns iterate in name order, so name-based
     * comparison is enough to align two iterators.
     */
    private static int compareColumnsByName(ColumnData a, ColumnData b)
    {
        return a.column().name.bytes.compareTo(b.column().name.bytes);
    }

    /**
     * Consults each active rule's {@link ErrataRule#explainsResurrectedCell}
     * matcher on a single cursor-extra cell. Returns {@code true} when any
     * rule covers it (and records that rule in {@code fired}); {@code false}
     * otherwise.
     */
    private boolean cellExplainsResurrection(Cell<?> cursorCell, EnumSet<ErrataRule> fired)
    {
        for (ErrataRule rule : activeRules)
        {
            if (rule.explainsResurrectedCell(cursorCell))
            {
                fired.add(rule);
                return true;
            }
        }
        return false;
    }

    /**
     * For one cell pair, determine whether the difference (if any) is covered by at
     * least one active rule. Returns true when the pair matches exactly OR when the
     * difference matches a rule (whose counter we then add to {@code fired}).
     *
     * <p>The matcher checks rules in two passes:
     * <ol>
     *   <li>First, ask each rule whether it explains the broader cell-pair difference
     *       (any combination of value, ttl, ldt, etc). This catches multi-field
     *       signatures like bug 1B's tombstone-vs-expiring divergence.</li>
     *   <li>Falling through to the narrower value-only path: if the cells differ
     *       only in their value bytes (timestamp, ttl, ldt all equal), ask each
     *       rule whether the value-only difference is its signature.</li>
     * </ol>
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

        // Path differences are never explained by any current rule.
        if (!pathEqual)
            return false;

        // First pass: broad multi-field matchers. Rules that override
        // explainsCellPairDifference() can suppress differences across multiple
        // fields at once (bug 1B's tombstone-vs-expiring is the canonical case —
        // it differs in ttl AND ldt AND value all simultaneously).
        for (ErrataRule rule : activeRules)
        {
            if (rule.explainsCellPairDifference(legacy, cursor))
            {
                fired.add(rule);
                return true;
            }
        }

        // Second pass: narrow value-only matchers. Any non-value difference
        // disqualifies these rules — they're for cases where the bug only
        // affects which value-bytes win the tie-break.
        if (!tsEqual || !ttlEqual || !ldtEqual || !counterFlagEqual)
            return false;

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
        return Objects.equals(a, b);
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
