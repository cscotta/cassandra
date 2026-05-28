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
package org.apache.cassandra.tools.compactionvalidator.tui;

import org.apache.cassandra.tools.compactionvalidator.RunResult;
import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Sealed hierarchy of TUI events emitted by {@code TuiManager}'s
 * {@link org.apache.cassandra.tools.compactionvalidator.ProgressTap} adapter
 * onto {@link ProgressBus}.  The render thread drains the bus and dispatches
 * events to individual {@link TuiPanel}s.
 *
 * <p>All concrete subtypes are immutable; their fields are public final to
 * keep the data class minimal and free of getter boilerplate.
 */
public sealed abstract class TuiEvent
        permits TuiEvent.RunStart,
                TuiEvent.ComparisonLabels,
                TuiEvent.SchemaReady,
                TuiEvent.DataGenProgress,
                TuiEvent.DataGenComplete,
                TuiEvent.CompactionProgress,
                TuiEvent.CompactionInputs,
                TuiEvent.CompactionTaskStart,
                TuiEvent.CompactionTaskEnd,
                TuiEvent.SstableEmitted,
                TuiEvent.CompactionComplete,
                TuiEvent.CompactionStatus,
                TuiEvent.ValidationProgress,
                TuiEvent.ValidationComplete,
                TuiEvent.RunComplete,
                TuiEvent.RunFailed
{
    /** Wall-clock time (epoch ms) when this event was created. */
    public final long timestampMs = System.currentTimeMillis();

    /** Emitted once at the start of every run. */
    public static final class RunStart extends TuiEvent
    {
        public final int runNumber;
        public final long seed;

        public RunStart(int runNumber, long seed)
        {
            this.runNumber = runNumber;
            this.seed = seed;
        }
    }

    /**
     * Display labels for the run's two sides — fired once per run after RunStart so
     * panels can swap their hardcoded "LEGACY"/"CURSOR" headers for the names the
     * user picked in their YAML config (e.g. "stcs" / "lcs"). Internal routing on
     * subsequent events still uses the opaque {@code "legacy"} / {@code "cursor"}
     * tags; this event is the dictionary that maps tag → display label.
     */
    public static final class ComparisonLabels extends TuiEvent
    {
        public final String controlName;
        public final String experimentName;

        public ComparisonLabels(String controlName, String experimentName)
        {
            this.controlName = controlName;
            this.experimentName = experimentName;
        }
    }

    /** Emitted when the run's schema is generated. */
    public static final class SchemaReady extends TuiEvent
    {
        public final GeneratedSchema schema;

        public SchemaReady(GeneratedSchema schema)
        {
            this.schema = schema;
        }
    }

    /** Periodic progress update during data generation. */
    public static final class DataGenProgress extends TuiEvent
    {
        public final long bytes;
        public final long target;
        public final long partitions;
        public final long rows;
        public final double bytesPerSec;

        public DataGenProgress(long bytes, long target, long partitions, long rows, double bytesPerSec)
        {
            this.bytes = bytes;
            this.target = target;
            this.partitions = partitions;
            this.rows = rows;
            this.bytesPerSec = bytesPerSec;
        }
    }

    /** Emitted once when data generation finishes. */
    public static final class DataGenComplete extends TuiEvent
    {
        public final DataGenStats stats;
        public final long durationMs;

        public DataGenComplete(DataGenStats stats, long durationMs)
        {
            this.stats = stats;
            this.durationMs = durationMs;
        }
    }

    /** Periodic progress update during compaction. */
    public static final class CompactionProgress extends TuiEvent
    {
        /** Backend identifier: {@code "legacy"} or {@code "cursor"}. */
        public final String backend;
        /** Cumulative bytes scanned across all completed tasks plus the in-flight one. */
        public final long bytes;
        /**
         * Sum of initial input bytes — kept for legacy display, but UCS-style multi-pass
         * compaction can drive {@link #bytes} past this number. Don't use as a 0-100% bar
         * denominator; use {@link #currentTaskTotal} instead.
         */
        public final long target;
        public final long partitions;
        public final double bytesPerSec;
        /** Bytes scanned in the in-flight task only — resets to 0 between tasks. Use with {@link #currentTaskTotal}. */
        public final long currentTaskBytes;
        /** Total bytes the in-flight task will read (sum of its input sstables' on-disk sizes). May be 0 if unknown. */
        public final long currentTaskTotal;

        public CompactionProgress(String backend, long bytes, long target, long partitions, double bytesPerSec,
                                  long currentTaskBytes, long currentTaskTotal)
        {
            this.backend = backend;
            this.bytes = bytes;
            this.target = target;
            this.partitions = partitions;
            this.bytesPerSec = bytesPerSec;
            this.currentTaskBytes = currentTaskBytes;
            this.currentTaskTotal = currentTaskTotal;
        }
    }

    /** Emitted when an SSTable is written to disk by either backend. */
    public static final class SstableEmitted extends TuiEvent
    {
        public final String backend;
        public final String filename;
        public final long sizeBytes;
        public final int level;

        public SstableEmitted(String backend, String filename, long sizeBytes, int level)
        {
            this.backend = backend;
            this.filename = filename;
            this.sizeBytes = sizeBytes;
            this.level = level;
        }
    }

    /** Emitted once at the start of a backend's compaction phase, listing the input set. */
    public static final class CompactionInputs extends TuiEvent
    {
        public final String backend;
        public final java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> inputs;

        public CompactionInputs(String backend,
                                java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> inputs)
        {
            this.backend = backend;
            this.inputs = inputs;
        }
    }

    /**
     * Emitted at the start of every compaction task.
     * {@code targetLevel} is -1 if unknown. {@code taskIndex}/{@code tasksInBatch} describe
     * the task's position within the current strategy round (1-based; e.g. "task 3 of 12").
     * {@code totalTasksDone} is the cumulative count of tasks completed for this backend
     * across all rounds so far.
     */
    public static final class CompactionTaskStart extends TuiEvent
    {
        public final String backend;
        public final java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> taskInputs;
        public final int targetLevel;
        public final int taskIndex;
        public final int tasksInBatch;
        public final int totalTasksDone;

        public CompactionTaskStart(String backend,
                                   java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> taskInputs,
                                   int targetLevel,
                                   int taskIndex,
                                   int tasksInBatch,
                                   int totalTasksDone)
        {
            this.backend = backend;
            this.taskInputs = taskInputs;
            this.targetLevel = targetLevel;
            this.taskIndex = taskIndex;
            this.tasksInBatch = tasksInBatch;
            this.totalTasksDone = totalTasksDone;
        }
    }

    /** Emitted after every compaction task completes, listing the SSTables it produced. */
    public static final class CompactionTaskEnd extends TuiEvent
    {
        public final String backend;
        public final int taskIndex;
        public final java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> taskOutputs;

        public CompactionTaskEnd(String backend,
                                 int taskIndex,
                                 java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> taskOutputs)
        {
            this.backend = backend;
            this.taskIndex = taskIndex;
            this.taskOutputs = taskOutputs;
        }
    }

    /** Emitted once when a compaction backend has finished. */
    public static final class CompactionComplete extends TuiEvent
    {
        public final String backend;
        public final CompactionStats stats;

        public CompactionComplete(String backend, CompactionStats stats)
        {
            this.backend = backend;
            this.stats = stats;
        }
    }

    /**
     * Free-form status text for one side, used to surface states that don't fit
     * the task-start/task-end/progress sequence — most importantly the
     * "queued, waiting for the other side to finish" state during cross-format
     * serial runs (where one format runs to completion before the other can
     * start, because {@code DatabaseDescriptor.setSelectedSSTableFormat} is
     * JVM-global). Setting an empty {@code message} clears the status line.
     */
    public static final class CompactionStatus extends TuiEvent
    {
        public final String backend;
        public final String message;

        public CompactionStatus(String backend, String message)
        {
            this.backend = backend;
            this.message = message;
        }
    }

    /** Periodic progress update during validation. */
    public static final class ValidationProgress extends TuiEvent
    {
        public final long partitionsChecked;

        public ValidationProgress(long partitionsChecked)
        {
            this.partitionsChecked = partitionsChecked;
        }
    }

    /** Emitted once when validation finishes. */
    public static final class ValidationComplete extends TuiEvent
    {
        public final ValidationStats stats;
        public final boolean success;

        public ValidationComplete(ValidationStats stats, boolean success)
        {
            this.stats = stats;
            this.success = success;
        }
    }

    /** Emitted once when a run completes successfully. */
    public static final class RunComplete extends TuiEvent
    {
        public final RunResult result;

        public RunComplete(RunResult result)
        {
            this.result = result;
        }
    }

    /** Emitted once when a run fails (validation mismatch). */
    public static final class RunFailed extends TuiEvent
    {
        public final MismatchReport report;
        public final RunResult result;

        public RunFailed(MismatchReport report, RunResult result)
        {
            this.report = report;
            this.result = result;
        }
    }
}
