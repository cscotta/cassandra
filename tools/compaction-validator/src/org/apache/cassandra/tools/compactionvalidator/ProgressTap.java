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
package org.apache.cassandra.tools.compactionvalidator;

import java.util.List;

import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.datagen.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Sink for progress events emitted by the run orchestrator.  Implementations
 * include the plain-text reporter (which writes to {@code stdout}) and the
 * Lanterna-based TUI (which renders a multi-panel animated terminal UI).
 *
 * <p>All methods are non-blocking and may be invoked from any thread; callers
 * should treat published values as transient snapshots.  Implementations are
 * responsible for handling concurrent invocations safely.
 */
public interface ProgressTap
{
    /** Called once at the very start of a run, before any other event. */
    void onRunStart(int runNumber, long seed);

    /**
     * Display labels for the comparison's two sides — passed once per run after
     * {@link #onRunStart} so panels / reporters can show config-driven names
     * (e.g. {@code "legacy"} / {@code "cursor"}, {@code "stcs"} / {@code "lcs"})
     * instead of hardcoded labels. The internal routing IDs on later events
     * (the {@code backend} string on compaction events, etc.) stay
     * {@code "legacy"} / {@code "cursor"} as opaque tags — implementations
     * translate them through the labels published here.
     *
     * <p>Default no-op so existing implementations don't need to override.
     */
    default void onComparisonLabels(String controlName, String experimentName) {}

    /** Called once after the schema has been generated but before data generation starts. */
    void onSchemaReady(GeneratedSchema schema);

    /**
     * Called periodically during data generation.
     *
     * @param bytes        cumulative bytes written so far
     * @param target       target total bytes for this run
     * @param partitions   cumulative partitions written
     * @param rows         cumulative rows written
     * @param bytesPerSec  current write throughput in bytes/s
     */
    void onDataGenProgress(long bytes, long target, long partitions, long rows, double bytesPerSec);

    /** Called once at the end of the data-generation phase. */
    void onDataGenComplete(DataGenStats stats, long durationMs);

    /**
     * Called periodically during compaction.
     *
     * @param backend      either {@code "legacy"} or {@code "cursor"}
     * @param bytes        cumulative bytes processed by this backend
     * @param target       total input bytes to process (sum of source SSTable sizes); 0 if unknown
     * @param partitions   cumulative partitions processed by this backend
     * @param bytesPerSec  current read+write throughput in bytes/s
     */
    /**
     * Called periodically during compaction.
     *
     * @param backend           {@code "legacy"} or {@code "cursor"}
     * @param bytes             cumulative bytes scanned across all tasks completed so far + this one
     * @param target            sum of initial input bytes (kept for cumulative-vs-input ratio reporting, but
     *                          is unreliable as a progress denominator since UCS does multiple passes and
     *                          {@code bytes} can exceed it. Use {@code currentTaskBytes/currentTaskTotal}
     *                          for an actual 0-100% bar.)
     * @param partitions        cumulative partition count across all tasks
     * @param bytesPerSec       current throughput
     * @param currentTaskBytes  bytes scanned in the in-flight task only — resets to 0 between tasks
     * @param currentTaskTotal  total bytes the in-flight task will read (sum of its input sstables' on-disk sizes)
     */
    void onCompactionProgress(String backend, long bytes, long target, long partitions,
                              double bytesPerSec, long currentTaskBytes, long currentTaskTotal);

    /**
     * Called when the named backend has finished writing one output SSTable.
     *
     * @param backend   either {@code "legacy"} or {@code "cursor"}
     * @param filename  base filename (e.g. {@code na-1-big-Data.db})
     * @param sizeBytes on-disk size of the new SSTable
     * @param level     visual level for the LSM widget (typically 0)
     */
    void onSstableEmitted(String backend, String filename, long sizeBytes, int level);

    /** Called once when the named backend's compaction has completed. */
    void onCompactionComplete(String backend, CompactionStats stats);

    /** Called periodically during the validation phase. */
    void onValidationProgress(long partitionsChecked);

    /** Called once at the end of the validation phase. */
    void onValidationComplete(ValidationStats stats, boolean success);

    /** Called once when the entire run has succeeded. */
    void onRunComplete(RunResult result);

    /** Called once when the entire run has failed (validation mismatch). */
    void onRunFailed(MismatchReport report, RunResult result);

    /**
     * Called once at the start of a backend's compaction phase, before any tasks run.
     * Lists every SSTable the backend has on disk at start (these are the run's input set).
     * Default no-op so existing implementations don't need to override.
     */
    default void onCompactionInputs(String backend, List<SstableInfo> inputs) {}

    /**
     * Called immediately before a single compaction task starts. Lists the SSTables
     * the task will read in this round, plus the task's target output level (or {@code -1}
     * if the strategy doesn't define one — UCS doesn't, LCS does), the 1-based index of
     * this task within its strategy round, the total task count for the round, and the
     * cumulative count of tasks already completed for this backend across all rounds.
     * Default no-op.
     */
    default void onCompactionTaskStart(String backend, List<SstableInfo> taskInputs,
                                       int targetLevel, int taskIndex, int tasksInBatch, int totalTasksDone) {}

    /**
     * Called immediately after a single compaction task finishes. Lists the new SSTables
     * the task produced. The {@code taskIndex} matches the one passed to the corresponding
     * {@link #onCompactionTaskStart}, which lets the consumer disambiguate completions when
     * multiple tasks run concurrently (each {@code Start} may be followed by other tasks'
     * {@code Start}/{@code End} events before this task's matching {@code End} arrives).
     * Default no-op.
     */
    default void onCompactionTaskEnd(String backend, int taskIndex, List<SstableInfo> taskOutputs) {}

    /**
     * Free-form status text for one side. Used in cross-format serial runs
     * (BIG vs BTI) to mark the not-yet-running side as "queued" while the other
     * side compacts — without this the second side's panel sits blank for the
     * entire first half of the phase. The first {@link #onCompactionTaskStart}
     * for that side naturally overwrites the queued message. Pass an empty
     * {@code message} to clear the status line. Default no-op.
     */
    default void onCompactionStatus(String backend, String message) {}
}
