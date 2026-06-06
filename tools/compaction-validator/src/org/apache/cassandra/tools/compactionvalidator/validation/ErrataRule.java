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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.Row;

/**
 * Catalogue of <em>known</em> cursor-vs-legacy compaction divergences whose root cause
 * is already identified and queued for a fix. Each rule names the symptom, describes
 * the underlying defect, and carries a matcher predicate the validator uses to decide
 * whether an observed partition mismatch fits the rule's pattern.
 *
 * <p>When the user passes {@code --ignore-errata=<rule-name>,<rule-name>,...} on the
 * command line, the validator runs Phase A as normal — but on any hash mismatch it
 * does a per-cell comparison and asks {@link ErrataChecker} whether every observed
 * difference is explained by one of the active rules. If yes, the partition is
 * recorded under the matching rule(s) in {@link ValidationStats#errataOccurrences}
 * and the validator continues to the next partition instead of halting the run.
 *
 * <p>This keeps long soak runs useful while a known defect awaits its fix: the run
 * still surfaces any <em>new</em> divergence (one that doesn't match any active rule)
 * loudly via the usual {@link MismatchReport}, but tolerates the known one.
 */
public enum ErrataRule
{
    /**
     * Discovered 2026-05-25 (seed {@code 0x693F6A0B09368F0C}, partition {@code wUZ}).
     *
     * <p>{@code CursorCompactor.mergeCells()}'s {@code COMPARE} branch inverts the
     * tie-breaker direction at {@code CursorCompactor.java:694} ({@code if (compare >= 0)}
     * switches to the new candidate when it should switch only when the new candidate
     * is larger, i.e. {@code compare < 0}).
     *
     * <p>Symptom: when two source SSTables contribute live cells for the same
     * (partition, column) at the same timestamp, with matching TTL and deletion
     * fields, cursor compaction picks the cell with the <em>smaller</em> unsigned
     * value bytes; legacy correctly picks the larger one per
     * {@code Cells.resolveRegular()}'s {@code compareValues(left,right) >= 0 ? left : right}.
     *
     * <p>This matcher fires when two cells have equal timestamp + equal TTL +
     * equal localDeletionTime + equal counter-cell flag but differ in value. Any
     * other field difference is treated as a real mismatch even with the rule active.
     */
    EQUAL_TIMESTAMP_TIEBREAKER("equal-ts-tiebreaker",
        "Cursor compaction picks the smaller-value-bytes cell when two cells share "
        + "a timestamp (TTL/deletion equal). Inverted compareUnsigned direction in "
        + "CursorCompactor.mergeCells() COMPARE branch.")
    {
        @Override
        boolean explainsCellValueDifference(Cell<?> legacy, Cell<?> cursor)
        {
            // The bug is value-only: timestamp, TTL, localDeletionTime, and counter-flag
            // must all match. If any of those also differs, it's a different kind of
            // divergence and shouldn't be silenced by this rule.
            return legacy.timestamp() == cursor.timestamp()
                && legacy.ttl() == cursor.ttl()
                && legacy.localDeletionTime() == cursor.localDeletionTime()
                && legacy.isCounterCell() == cursor.isCounterCell();
        }
    },

    /**
     * Cursor-compaction bug 1A — cell flag mutual exclusivity violation.
     *
     * <p>Discovered through the format auditor: cursor's {@code mergeCells} can produce
     * a cell whose on-disk flags byte has BOTH {@code IS_DELETED_MASK} and
     * {@code IS_EXPIRING_MASK} set. The deserialiser's {@code else if} chain takes
     * the IS_EXPIRING branch and reads {@code (ttl, localDeletionTime)} where the
     * second field was supposed to be the expiration time {@code writetime + ttl}
     * but on disk holds the IS_DELETED-style deletion time instead.
     *
     * <p>The {@link FormatAuditor} catches this via the expiration-formula check.
     * This rule lets soak runs suppress the divergence the broken cell causes
     * during cell-by-cell comparison while waiting on an upstream fix.
     *
     * <p>Suppresses partitions where every cell-pair difference between legacy and
     * cursor's cell shows the bug-1A signature: the cursor cell has {@code isExpiring()}
     * set but its {@code localDeletionTime} field doesn't match the formula
     * {@code (timestamp / 1_000_000) + ttl}.
     */
    TOMBSTONE_EXPIRING_FLAGS_BOTH_SET("tombstone-expiring-flags-both-set",
        "Cursor compaction sets both IS_DELETED and IS_EXPIRING in the same cell's "
        + "flags byte. Detected via the expiration-formula invariant — the on-disk "
        + "localDeletionTime is the deletion time, not writetime+ttl.")
    {
        @Override
        boolean explainsCellPairDifference(Cell<?> legacy, Cell<?> cursor)
        {
            // Bug 1A signature: cursor reads back as "expiring" with a localDeletionTime
            // that doesn't match the expiration formula. Legacy is correct (either a
            // proper expiring cell or a proper tombstone).
            if (!cursor.isExpiring())
                return false;
            long expectedLdt = (cursor.timestamp() / 1_000_000L) + cursor.ttl();
            long delta       = Math.abs(cursor.localDeletionTime() - expectedLdt);
            return delta > 2L; // 2-second tolerance, mirrors FormatAuditor
        }
    },

    /**
     * Cursor-compaction bug 1B — silent data resurrection.
     *
     * <p>When two source SSTables contribute cells for the same (partition, column) at
     * the same timestamp — one a per-cell tombstone, the other an expiring cell —
     * {@code Cells.resolveRegular()} requires the tombstone to win (legacy behaviour:
     * deleted data stays deleted under timestamp ties). Cursor compaction reverses
     * the choice and emits the expiring cell, silently resurrecting data the user
     * deleted.
     *
     * <p>Suppresses partitions where every cell-pair difference fits the signature:
     * legacy cell is a tombstone (zero TTL, non-empty localDeletionTime), cursor cell
     * is expiring (non-zero TTL), and both share the same timestamp.
     */
    TOMBSTONE_RESURRECTED_BY_EXPIRING("tombstone-resurrected-by-expiring",
        "Cursor compaction emits an expiring cell where legacy correctly emits a "
        + "tombstone, when both inputs share a timestamp. Reverses Cells.resolveRegular's "
        + "tombstone-wins rule under timestamp ties.")
    {
        @Override
        boolean explainsCellPairDifference(Cell<?> legacy, Cell<?> cursor)
        {
            // Same timestamp, legacy is tombstone, cursor is expiring. The values,
            // TTL, and localDeletionTime fields will all differ — this rule's whole
            // point is to suppress those multi-field differences.
            return legacy.timestamp() == cursor.timestamp()
                && legacy.isTombstone()
                && cursor.isExpiring();
        }

        @Override
        boolean explainsResurrectedRow(Row cursorRow)
        {
            // Static-row manifestation of bug 1B: when 16 same-TS writes to a
            // static column produce {tombstone, expiring} candidates, legacy's
            // resolveRegular picks the tombstone and Cassandra's Row.Merger
            // collapses the all-tombstone static row to {@code null} (rendered
            // as EMPTY_STATIC_ROW). Cursor's bug-1B picks the expiring cell, so
            // its static row keeps that one live expiring cell — making
            // legacy.staticRow().isEmpty()=true but cursor.staticRow().isEmpty()=false,
            // which the row-empty / not-empty check upstream bails on without
            // ever consulting the cell-pair matcher.
            //
            // Fingerprint: cursor's row contains only expiring + tombstone cells
            // (no pure-live cells) and at least one expiring. That's structurally
            // what survives when bug 1B resurrects an expiring write that should
            // have been overridden by a same-TS tombstone.
            if (cursorRow == null || cursorRow.isEmpty())
                return false;
            boolean anyExpiring = false;
            for (ColumnData cd : cursorRow)
            {
                if (!(cd instanceof Cell<?>))
                    return false;
                Cell<?> cell = (Cell<?>) cd;
                if (cell.isExpiring())
                    anyExpiring = true;
                else if (!cell.isTombstone())
                    return false; // pure live cell — not bug-1B's signature
            }
            return anyExpiring;
        }

        @Override
        boolean explainsResurrectedCell(Cell<?> cursorCell)
        {
            // Column-within-row manifestation of bug 1B: cursor has an extra
            // cell (for a column legacy doesn't carry in this row) that's
            // expiring. Bug-1B resurrects the expiring write that should have
            // been overridden by a same-TS tombstone; legacy's compaction
            // correctly drops the cell, so legacy's row simply doesn't contain
            // that column at all.
            //
            // Pure-live or pure-tombstone extra cells don't match this
            // signature — only an expiring cell carries the bug-1B fingerprint.
            return cursorCell != null && cursorCell.isExpiring();
        }
    },

    /**
     * Cursor-compaction bug 2 — {@code prevUnfilteredSize} field always written as zero.
     *
     * <p>The BIG SSTable format records the on-disk size of each unfiltered (row /
     * range tombstone marker) so reverse iteration can navigate backward without a
     * full forward re-scan. Cursor compaction's writer leaves the field at its
     * default zero value; reverse-scanning a partition then either throws or
     * silently returns the wrong sequence.
     *
     * <p>The validator's reverse-scan pass detects the divergence by hashing the
     * partition in reverse on both sides. This rule has no per-cell matcher (the
     * forward-scan validator never sees the field); it exists so soak runs can
     * suppress the reverse-scan-flagged partitions while keeping forward-scan
     * coverage intact.
     */
    PREV_UNFILTERED_SIZE_ZERO("prev-unfiltered-size-zero",
        "Cursor compaction always writes prevUnfilteredSize=0. Caught by the "
        + "reverse-scan validator — forward iteration never reads this field."),

    /**
     * Cursor-compaction bug 3 — {@code Index.db} final block width off-by-one.
     *
     * <p>Partitions large enough to span at least one Index.db block boundary
     * (≥ 64 KiB by default) carry a final-block width that doesn't match the
     * actual extent of the last block in {@code Data.db}. End-of-partition
     * indexed point reads land one byte off and return the wrong row (or none).
     *
     * <p>The validator's indexed-point-read pass flags this. Like {@link #PREV_UNFILTERED_SIZE_ZERO},
     * the rule has no per-cell matcher; it exists for {@code --ignore-errata} suppression.
     */
    INDEX_BLOCK_WIDTH_OFF_BY_ONE("index-block-width-off-by-one",
        "Cursor compaction's Index.db last-block width is off by one for partitions "
        + "≥ 64 KiB. End-of-partition point reads land in the wrong slice."),

    /**
     * Cursor-compaction bug 4 — sparse-row encoding drops the last column.
     *
     * <p>For tables with ≥ 64 regular columns whose rows omit most cells (so that
     * Cassandra's {@code Columns.serialize} takes the "subset" encoding path),
     * cursor compaction's writer drops the highest-position cell from the
     * present-columns set. The legacy iterator pipeline encodes correctly.
     *
     * <p>Suppresses partitions where the only difference between legacy and cursor
     * is exactly one extra cell on legacy's side — at the end of the row's
     * column list — and the row contains enough cells (≥ 12) that the wide-row
     * sparse path is the most likely encoding.
     */
    WIDE_COLUMN_SUBSET_DROPPED_LAST("wide-column-subset-dropped-last",
        "Cursor compaction's column-subset row encoder drops the last present column. "
        + "Fires only on rows with ≥ 12 present cells, the threshold above which "
        + "the sparse-encoding path is the likely culprit.");

    private final String cliName;
    private final String description;

    ErrataRule(String cliName, String description)
    {
        this.cliName = cliName;
        this.description = description;
    }

    /** @return the lower-case, dash-separated name the user types on the CLI */
    public String cliName()
    {
        return cliName;
    }

    /** @return one-line summary used in {@code --help} and in run-summary output */
    public String description()
    {
        return description;
    }

    /**
     * Maps a {@link FormatViolation.Kind} to the {@link ErrataRule} that suppresses
     * it (or {@code null} when no rule covers the kind — the violation always
     * causes a failure). Used by {@link org.apache.cassandra.tools.compactionvalidator.RunOrchestrator}
     * to decide whether each violation reported by the {@link FormatAuditor} is
     * a known-bug occurrence (counted, suppressed) or a new failure.
     *
     * <p>Five kinds map to {@link #TOMBSTONE_EXPIRING_FLAGS_BOTH_SET} — all of
     * them are signatures of bug 1A at progressively later points in the
     * deserialiser's misalignment cascade:
     * <ul>
     *   <li>{@link FormatViolation.Kind#FLAG_DELETE_AND_EXPIRE} — the cell the
     *       writer encoded with both flag bits set; the deserialiser reads
     *       garbage for {@code ttl} but the broken expiration formula is still
     *       computable so the auditor flags it directly.</li>
     *   <li>{@link FormatViolation.Kind#DUPLICATE_CLUSTERING} — a few cells
     *       later, the deserialiser is reading bytes that happen to parse as
     *       a row marker with the same clustering as the previous row.
     *       Structurally invalid but in practice always a downstream symptom
     *       of the misalignment, not a real row-ordering bug.</li>
     *   <li>{@link FormatViolation.Kind#TIMESTAMP_NEGATIVE} — the misaligned
     *       byte stream is read as a {@code (vint timestamp delta + minTimestamp)}
     *       that underflows the long range, surfacing as a wildly negative
     *       timestamp on the decoded cell. Same root cause; later checkpoint.</li>
     *   <li>{@link FormatViolation.Kind#TTL_NEGATIVE} — same as above for the
     *       TTL slot. Listed for completeness; we haven't observed this fire
     *       in the wild but it can only realistically arise from the same
     *       misalignment.</li>
     *   <li>{@link FormatViolation.Kind#SCANNER_FAILURE} — eventually the
     *       deserialiser hits bounds checks it can't satisfy and throws
     *       {@code CorruptSSTableException}. Tied to the same rule so a soak
     *       run with bug-1A errata active rides through the entire cascade
     *       rather than halting on the first corrupted SSTable.</li>
     * </ul>
     *
     * <p>The only remaining kind that stays unmapped — strict-reverse
     * {@link FormatViolation.Kind#ROW_OUT_OF_ORDER} ({@code prev > curr}) — would
     * indicate a real writer bug not part of the bug-1A cascade. If a new
     * cascade symptom shows up that lands in that bucket, this list will need
     * another entry; for now we err on the side of failing loudly for it.
     */
    public static ErrataRule forFormatViolation(FormatViolation.Kind kind)
    {
        if (kind == null) return null;
        switch (kind)
        {
            case FLAG_DELETE_AND_EXPIRE:
            case DUPLICATE_CLUSTERING:
            case TIMESTAMP_NEGATIVE:
            case TTL_NEGATIVE:
            case SCANNER_FAILURE:
                return TOMBSTONE_EXPIRING_FLAGS_BOTH_SET;
            case ROW_OUT_OF_ORDER:
                // Strict reverse row ordering — would indicate a real writer bug
                // outside the known bug-1A cascade. Always surface as failure.
                return null;
            default:
                return null;
        }
    }

    /**
     * Per-rule override that returns {@code true} when the {@code legacy}/{@code cursor}
     * cell pair's value-only difference is the signature of this rule's defect.
     *
     * <p>Default {@code false} — rules that don't manifest as cell-value differences
     * inherit a safe no-op and leave the partition flagged as a real mismatch.
     */
    boolean explainsCellValueDifference(Cell<?> legacy, Cell<?> cursor)
    {
        return false;
    }

    /**
     * Per-rule override that returns {@code true} when the {@code legacy}/{@code cursor}
     * cell pair's broader (multi-field) difference is the signature of this rule's
     * defect. Used by rules whose symptom isn't value-only — e.g. bug 1B's
     * tombstone-vs-expiring divergence (different ttl, ldt, AND value all at once)
     * or bug 1A's broken expiration formula.
     *
     * <p>{@link ErrataChecker} consults this method <em>before</em> falling into the
     * narrower value-only path, so a rule that overrides this can suppress
     * differences that would otherwise be flagged as "non-value field also
     * differs — disqualified".
     *
     * <p>Default {@code false} — rules whose entire signature is value-only stay
     * narrow.
     */
    boolean explainsCellPairDifference(Cell<?> legacy, Cell<?> cursor)
    {
        return false;
    }

    /**
     * Per-rule override consulted by {@link ErrataChecker#rowsExplainable} when the
     * comparison shows one side's row collapsed to empty while the other side's row
     * still has data. Some cursor-compaction bugs (notably bug 1B on static cells)
     * produce exactly this asymmetry because legacy's same-TS reconciliation lands
     * on a tombstone whose row then collapses to empty via {@code Row.Merger}, while
     * cursor's reconciliation keeps a live cell that survives to the output.
     *
     * <p>The default returns {@code false} — most rules describe cell-pair defects
     * that don't have a row-asymmetry manifestation. Rules whose signature includes
     * "the entire row disappears on one side" override this and inspect the
     * non-empty side's cells for their fingerprint.
     */
    boolean explainsResurrectedRow(Row cursorRow)
    {
        return false;
    }

    /**
     * Per-rule override consulted by {@link ErrataChecker#rowsExplainable} when
     * the per-column merge-walk finds a cell present on cursor that has no
     * counterpart on legacy. Same root cause as
     * {@link #explainsResurrectedRow}: same-TS reconciliation on the legacy side
     * dropped the cell (e.g. tombstone won and {@code Row.Merger} pruned it from
     * the row body), while cursor's bug-1B kept the expiring cell.
     *
     * <p>Default {@code false}. Rules that recognise this signature on a single
     * cell — typically expiring with no other characteristic distinguishing it
     * from a normal expiring write — override this.
     */
    boolean explainsResurrectedCell(Cell<?> cursorCell)
    {
        return false;
    }

    // ---- CLI parsing helpers --------------------------------------------

    /**
     * Parses a {@code --ignore-errata} value into the corresponding set of rules.
     *
     * @param cliList comma-separated list (whitespace tolerated); empty / {@code null}
     *                yields an empty set
     * @return the matched rule set; iteration order matches the user's input
     * @throws IllegalArgumentException if any name in the list isn't a known rule —
     *         we'd rather fail loudly than silently swallow a typo'd rule name
     */
    public static Set<ErrataRule> parseCliList(String cliList)
    {
        if (cliList == null || cliList.trim().isEmpty())
            return Collections.emptySet();

        Set<ErrataRule> result = new LinkedHashSet<>();
        for (String token : cliList.split(","))
        {
            String name = token.trim();
            if (name.isEmpty())
                continue;
            ErrataRule rule = byCliName(name);
            if (rule == null)
                throw new IllegalArgumentException("Unknown errata rule '" + name + "'. Known rules: "
                                                   + Arrays.toString(allCliNames()));
            result.add(rule);
        }
        return result;
    }

    /** @return the rule with the given CLI name, or {@code null} if not found */
    public static ErrataRule byCliName(String name)
    {
        for (ErrataRule r : values())
            if (r.cliName.equals(name))
                return r;
        return null;
    }

    /** @return all rule CLI names — handy for {@code --help} output */
    public static String[] allCliNames()
    {
        ErrataRule[] vals = values();
        String[] names = new String[vals.length];
        for (int i = 0; i < vals.length; i++)
            names[i] = vals[i].cliName;
        return names;
    }
}
