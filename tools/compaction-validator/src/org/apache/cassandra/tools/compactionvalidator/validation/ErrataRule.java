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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cassandra.db.rows.Cell;

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
    };

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
            return java.util.Collections.emptySet();

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
