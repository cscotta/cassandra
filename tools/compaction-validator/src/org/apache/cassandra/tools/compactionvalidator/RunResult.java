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

import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.validation.FormatViolation;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Immutable record of one complete compaction-validator run, including schema
 * configuration, per-phase performance statistics, and (on failure) a
 * description of the mismatch and the path to the preserved SSTables.
 *
 * <p>Use the nested {@link Builder} to construct instances.
 */
public final class RunResult
{
    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** The root RNG seed used for this run. */
    public final long seed;

    /** Monotonically increasing run number within the current process invocation. */
    public final int runNumber;

    /** {@code true} if validation passed; {@code false} if a mismatch was found. */
    public final boolean success;

    /** Wall-clock time (epoch ms) when the run started. */
    public final long startTimeMs;

    /** Wall-clock time (epoch ms) when the run finished (success or failure). */
    public final long endTimeMs;

    // -------------------------------------------------------------------------
    // Schema / configuration
    // -------------------------------------------------------------------------

    /** Keyspace name used for this run (e.g. {@code cvtest_deadbeef}). */
    public final String keyspaceName;

    /** Table name (always {@code t} by convention). */
    public final String tableName;

    /** Number of partition key components in the generated schema. */
    public final int partitionKeyCount;

    /** Number of clustering key components in the generated schema. */
    public final int clusteringKeyCount;

    /** Number of static columns in the generated schema. */
    public final int staticColumnCount;

    /** Number of regular (non-key, non-static) columns in the generated schema. */
    public final int regularColumnCount;

    /** UCS {@code target_sstable_size} in MiB. */
    public final long targetSstableSizeMiB;

    /** UCS {@code base_shard_count}. */
    public final int baseShardCount;

    /** Compression chunk size in KiB (0 means uncompressed). */
    public final int compressionChunkKb;

    /**
     * Display labels for the comparison's two sides — typically the names from
     * the YAML's {@code comparison.control.name} / {@code comparison.experiment.name}.
     * The {@link org.apache.cassandra.tools.compactionvalidator.logging.RunLogger}
     * uses these so per-run log entries show user-named sides instead of always
     * saying "Legacy:" / "Cursor:".
     */
    public final String controlName;
    public final String experimentName;

    /**
     * Full {@code CREATE TABLE} CQL used for this run. Captured into the result so the
     * log writer and TUI failure-inspection panel can render it without holding a
     * reference to the live {@link org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema}.
     * Never null on a successfully-orchestrated run; may be {@code null} when the run
     * failed before schema generation.
     */
    public final String schemaCql;

    // -------------------------------------------------------------------------
    // Per-phase statistics
    // -------------------------------------------------------------------------

    /** Statistics gathered during the data-generation phase. */
    public final DataGenStats dataGenStats;

    /** Statistics gathered during the legacy compaction phase. */
    public final CompactionStats legacyStats;

    /** Statistics gathered during the cursor-based compaction phase. */
    public final CompactionStats cursorStats;

    /** Statistics gathered during the validation phase. */
    public final ValidationStats validationStats;

    // -------------------------------------------------------------------------
    // Failure information (null on success)
    // -------------------------------------------------------------------------

    /**
     * Human-readable description of the first mismatch found, or {@code null}
     * if the run succeeded.
     */
    public final String failureDetail;

    /**
     * Filesystem path to the preserved SSTable directory on failure, or
     * {@code null} if the run succeeded (or cleanup was not suppressed).
     */
    public final String preservedDir;

    /**
     * Full mismatch detail when validation failed, or {@code null} on success or
     * on non-validation errors. Carried in the result so the log writer and the
     * TUI failure-inspection panel can render the full partition dumps.
     */
    public final MismatchReport mismatchReport;

    /**
     * Format-level invariant violations found by {@link
     * org.apache.cassandra.tools.compactionvalidator.validation.FormatAuditor}
     * after compaction completes. Populated for every run (empty list when the
     * cursor side's output is clean); a non-empty list typically indicates a
     * compaction-writer bug like the {@code IS_DELETED}/{@code IS_EXPIRING} flag
     * collision (compaction bug 1A). Never {@code null}.
     */
    public final java.util.List<FormatViolation> formatViolations;

    /**
     * Optional structural shape tag from the schema generator. {@code "NARROW"} for
     * 3-8 regular columns or {@code "WIDE"} for 64+ regular columns. Logged so
     * post-mortem analysis of soak runs can correlate failures with the column
     * shape rolled. Falls back to "(unknown)" when the run failed before the
     * schema was generated.
     */
    public final String schemaShape;

    /**
     * Per-run {@code gc_grace_seconds} table option — drives whether the
     * compaction tombstone-purge code path runs at all. Set to {@code -1} when
     * the run failed before the schema was generated. Logged so soak-run
     * post-mortem can correlate failures with whether GC was active.
     */
    public final int gcGraceSeconds;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private RunResult(Builder builder)
    {
        this.seed = builder.seed;
        this.runNumber = builder.runNumber;
        this.success = builder.success;
        this.startTimeMs = builder.startTimeMs;
        this.endTimeMs = builder.endTimeMs;
        this.keyspaceName = builder.keyspaceName;
        this.tableName = builder.tableName;
        this.partitionKeyCount = builder.partitionKeyCount;
        this.clusteringKeyCount = builder.clusteringKeyCount;
        this.staticColumnCount = builder.staticColumnCount;
        this.regularColumnCount = builder.regularColumnCount;
        this.targetSstableSizeMiB = builder.targetSstableSizeMiB;
        this.baseShardCount = builder.baseShardCount;
        this.compressionChunkKb = builder.compressionChunkKb;
        this.controlName = builder.controlName;
        this.experimentName = builder.experimentName;
        this.schemaCql = builder.schemaCql;
        this.dataGenStats = builder.dataGenStats;
        this.legacyStats = builder.legacyStats;
        this.cursorStats = builder.cursorStats;
        this.validationStats = builder.validationStats;
        this.failureDetail = builder.failureDetail;
        this.preservedDir = builder.preservedDir;
        this.mismatchReport = builder.mismatchReport;
        this.formatViolations = builder.formatViolations != null
                                ? java.util.Collections.unmodifiableList(builder.formatViolations)
                                : java.util.Collections.emptyList();
        this.schemaShape = builder.schemaShape != null ? builder.schemaShape : "(unknown)";
        this.gcGraceSeconds = builder.gcGraceSeconds;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns {@code true} if this run completed without a validation mismatch. */
    public boolean isSuccess()
    {
        return success;
    }

    /** Returns {@code true} if this run detected a validation mismatch. */
    public boolean isFailure()
    {
        return !success;
    }

    /**
     * Returns the ratio of legacy compaction duration to cursor compaction
     * duration.  Values greater than 1.0 indicate that cursor compaction was
     * faster.  Returns {@code 1.0} if either duration is zero to avoid
     * division-by-zero.
     */
    public double cursorSpeedup()
    {
        if (legacyStats == null || cursorStats == null)
            return 1.0;
        long legacyMs = legacyStats.durationMs;
        long cursorMs = cursorStats.durationMs;
        if (legacyMs == 0 || cursorMs == 0)
            return 1.0;
        return (double) legacyMs / (double) cursorMs;
    }

    /** Returns the total elapsed wall-clock time for this run in milliseconds. */
    public long elapsedMs()
    {
        return endTimeMs - startTimeMs;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder for {@link RunResult}.
     *
     * <p>All fields default to zero / {@code null} / {@code false}; set only
     * those that are relevant for a given run.
     */
    public static final class Builder
    {
        private long seed;
        private int runNumber;
        private boolean success;
        private long startTimeMs;
        private long endTimeMs;
        private String keyspaceName = "";
        private String tableName = "t";
        private int partitionKeyCount;
        private int clusteringKeyCount;
        private int staticColumnCount;
        private int regularColumnCount;
        private long targetSstableSizeMiB;
        private int baseShardCount;
        private int compressionChunkKb;
        private String controlName;
        private String experimentName;
        private String schemaCql;
        private DataGenStats dataGenStats;
        private CompactionStats legacyStats;
        private CompactionStats cursorStats;
        private ValidationStats validationStats;
        private String failureDetail;
        private String preservedDir;
        private MismatchReport mismatchReport;
        private java.util.List<FormatViolation> formatViolations;
        private String schemaShape;
        private int gcGraceSeconds = -1;

        public Builder seed(long seed) { this.seed = seed; return this; }
        public Builder runNumber(int runNumber) { this.runNumber = runNumber; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder startTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; return this; }
        public Builder endTimeMs(long endTimeMs) { this.endTimeMs = endTimeMs; return this; }
        public Builder keyspaceName(String keyspaceName) { this.keyspaceName = keyspaceName; return this; }
        public Builder tableName(String tableName) { this.tableName = tableName; return this; }
        public Builder partitionKeyCount(int partitionKeyCount) { this.partitionKeyCount = partitionKeyCount; return this; }
        public Builder clusteringKeyCount(int clusteringKeyCount) { this.clusteringKeyCount = clusteringKeyCount; return this; }
        public Builder staticColumnCount(int staticColumnCount) { this.staticColumnCount = staticColumnCount; return this; }
        public Builder regularColumnCount(int regularColumnCount) { this.regularColumnCount = regularColumnCount; return this; }
        public Builder targetSstableSizeMiB(long targetSstableSizeMiB) { this.targetSstableSizeMiB = targetSstableSizeMiB; return this; }
        public Builder baseShardCount(int baseShardCount) { this.baseShardCount = baseShardCount; return this; }
        public Builder compressionChunkKb(int compressionChunkKb) { this.compressionChunkKb = compressionChunkKb; return this; }
        public Builder controlName(String controlName) { this.controlName = controlName; return this; }
        public Builder experimentName(String experimentName) { this.experimentName = experimentName; return this; }
        public Builder schemaCql(String schemaCql) { this.schemaCql = schemaCql; return this; }
        public Builder dataGenStats(DataGenStats dataGenStats) { this.dataGenStats = dataGenStats; return this; }
        public Builder legacyStats(CompactionStats legacyStats) { this.legacyStats = legacyStats; return this; }
        public Builder cursorStats(CompactionStats cursorStats) { this.cursorStats = cursorStats; return this; }
        public Builder validationStats(ValidationStats validationStats) { this.validationStats = validationStats; return this; }
        public Builder failureDetail(String failureDetail) { this.failureDetail = failureDetail; return this; }
        public Builder preservedDir(String preservedDir) { this.preservedDir = preservedDir; return this; }
        public Builder mismatchReport(MismatchReport mismatchReport) { this.mismatchReport = mismatchReport; return this; }
        public Builder formatViolations(java.util.List<FormatViolation> formatViolations) { this.formatViolations = formatViolations; return this; }
        public Builder schemaShape(String schemaShape) { this.schemaShape = schemaShape; return this; }
        public Builder gcGraceSeconds(int gcGraceSeconds) { this.gcGraceSeconds = gcGraceSeconds; return this; }

        /** Builds the {@link RunResult}. */
        public RunResult build()
        {
            return new RunResult(this);
        }
    }
}
