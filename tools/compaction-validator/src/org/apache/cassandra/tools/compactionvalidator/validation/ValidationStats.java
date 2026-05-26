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

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable statistics accumulated during the validation phase of a run.
 *
 * <p>Counter fields are {@link AtomicLong} so multiple validation shards can update
 * them concurrently without external synchronisation — required since the validator
 * now runs Phase A across N parallel token-range workers under
 * {@code --validation-threads N}.
 *
 * <p>Consumers should treat the AtomicLong references as immutable and read values
 * via {@code .get()} (or the convenience getters); the {@link #durationMs} field
 * stays a plain {@code volatile long} because only the validator's outer block
 * writes it (once, when validation finishes).
 */
public class ValidationStats
{
    /** Total number of partitions compared between legacy and cursor output. */
    public final AtomicLong partitionsChecked = new AtomicLong(0L);

    /** Total number of rows compared between legacy and cursor output. */
    public final AtomicLong rowsChecked = new AtomicLong(0L);

    /** Number of partitions for which at least one mismatch was found. */
    public final AtomicLong partitionMismatches = new AtomicLong(0L);

    /**
     * Per-rule count of partitions whose mismatch was fully explained by an
     * active {@link ErrataRule} (and therefore suppressed via
     * {@code --ignore-errata}). Kept in an {@link EnumMap} initialised at
     * construction so all rules have an entry — readers don't need null-checks
     * and concurrent writers from multiple Phase A shards just call
     * {@link AtomicLong#incrementAndGet()} on the rule's pre-allocated counter.
     */
    public final Map<ErrataRule, AtomicLong> errataOccurrences;

    /** Total elapsed wall-clock time for the validation phase, in milliseconds. */
    public volatile long durationMs;

    public ValidationStats()
    {
        EnumMap<ErrataRule, AtomicLong> m = new EnumMap<>(ErrataRule.class);
        for (ErrataRule r : ErrataRule.values())
            m.put(r, new AtomicLong(0L));
        this.errataOccurrences = m;
    }

    /**
     * Increments the counter for the given errata rule. Safe to call from any
     * thread.
     */
    public void recordErrata(ErrataRule rule)
    {
        AtomicLong c = errataOccurrences.get(rule);
        if (c != null)
            c.incrementAndGet();
    }

    /** @return total number of errata-suppressed partitions across all rules */
    public long totalErrataOccurrences()
    {
        long total = 0L;
        for (AtomicLong c : errataOccurrences.values())
            total += c.get();
        return total;
    }

    /**
     * Returns {@code true} if any partition mismatches were recorded.
     *
     * <p>This is the primary indicator of a correctness failure: even a single
     * mismatch means the cursor and legacy outputs diverged. Errata-suppressed
     * occurrences are tracked separately via {@link #errataOccurrences} and do
     * NOT count toward this flag — that's the whole point of the suppression.
     */
    public boolean hasMismatches()
    {
        return partitionMismatches.get() > 0L;
    }
}
