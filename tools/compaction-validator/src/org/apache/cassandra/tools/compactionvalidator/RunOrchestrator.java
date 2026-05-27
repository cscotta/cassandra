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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.statements.schema.CreateTableStatement;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.Keyspaces;
import org.apache.cassandra.schema.Schema;
import org.apache.cassandra.schema.SchemaTransformation;
import org.apache.cassandra.schema.SchemaTransformations;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.schema.UserFunctions;
import org.apache.cassandra.schema.Views;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.tools.compactionvalidator.compaction.ParallelCompactor;
import org.apache.cassandra.tools.compactionvalidator.compaction.SstableSetManager;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenerator;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.schema.SchemaGenerator;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;
import org.apache.cassandra.tools.compactionvalidator.validation.FormatAuditor;
import org.apache.cassandra.tools.compactionvalidator.validation.FormatViolation;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;
import org.apache.cassandra.tools.compactionvalidator.validation.Validator;

/**
 * Drives a single end-to-end compaction-validator run: schema generation, data
 * generation, parallel legacy/cursor compaction, and validation of the two
 * outputs.
 *
 * <p>The orchestrator owns the run-local working directory layout, the two
 * offline {@link ColumnFamilyStore} instances (one per backend), and the
 * progress reporting hand-off to a {@link ProgressTap}.  It does <em>not</em>
 * own the JVM's static schema/cluster metadata state — that is initialised
 * once at process startup by {@link Main#bootstrapJvm()}.
 *
 * <p>To support running both backends side-by-side against byte-identical
 * inputs, the orchestrator constructs two parallel keyspaces — {@code
 * <ks>_legacy} and {@code <ks>_cursor} — each with its own data directory and
 * its own table id.  The randomly generated schema is registered under both
 * names by rewriting the keyspace prefix in the {@code CREATE TABLE} CQL
 * before passing it to the parser.
 *
 * <p>On success, the run directory is removed (unless the caller asked for
 * {@code --no-cleanup}).  On failure (validation mismatch or thrown
 * exception), the run directory is preserved and its absolute path is
 * recorded in the returned {@link RunResult}.
 */
public final class RunOrchestrator
{
    private static final Logger logger = LoggerFactory.getLogger(RunOrchestrator.class);

    private final long rootSeed;
    private final int runNumber;
    private final File workingDir;
    private final long targetBytes;
    private final int dataGenThreads;
    private final int compactionThreads;
    private final int validationThreads;
    private final java.util.Set<org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule> activeErrata;
    private final boolean cleanupOnSuccess;
    private final ProgressTap reporter;
    private final org.apache.cassandra.tools.compactionvalidator.config.RunConfig config;

    /**
     * Creates an orchestrator for one run.
     *
     * @param rootSeed          root RNG seed for this run
     * @param runNumber         1-based run number within the current process invocation
     * @param workingDir        top-level working directory shared across runs
     * @param targetBytes       approximate amount of source data to generate
     * @param dataGenThreads    number of parallel data-generation threads
     * @param compactionThreads number of concurrent compaction tasks per side
     * @param validationThreads number of concurrent token-range workers in validation Phase A
     * @param activeErrata      known errata rules to suppress during validation
     * @param cleanupOnSuccess  if {@code true}, delete the run directory after a successful run
     * @param reporter          progress callback (must not be {@code null}; pass a no-op
     *                          implementation if no reporting is desired)
     * @param config            full validated {@link org.apache.cassandra.tools.compactionvalidator.config.RunConfig}
     *                          (drives per-side compaction/compression/pipeline + side display names)
     */
    public RunOrchestrator(long rootSeed,
                           int runNumber,
                           File workingDir,
                           long targetBytes,
                           int dataGenThreads,
                           int compactionThreads,
                           int validationThreads,
                           java.util.Set<org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule> activeErrata,
                           boolean cleanupOnSuccess,
                           ProgressTap reporter,
                           org.apache.cassandra.tools.compactionvalidator.config.RunConfig config)
    {
        if (workingDir == null)
            throw new IllegalArgumentException("workingDir must not be null");
        if (targetBytes <= 0)
            throw new IllegalArgumentException("targetBytes must be positive");
        if (dataGenThreads <= 0)
            throw new IllegalArgumentException("dataGenThreads must be positive");
        if (compactionThreads <= 0)
            throw new IllegalArgumentException("compactionThreads must be positive");
        if (validationThreads <= 0)
            throw new IllegalArgumentException("validationThreads must be positive");
        if (reporter == null)
            throw new IllegalArgumentException("reporter must not be null");
        if (config == null || config.comparison == null
            || config.comparison.control == null || config.comparison.experiment == null)
            throw new IllegalArgumentException("RunConfig must declare comparison.control and comparison.experiment");

        this.rootSeed = rootSeed;
        this.runNumber = runNumber;
        this.workingDir = workingDir;
        this.targetBytes = targetBytes;
        this.dataGenThreads = dataGenThreads;
        this.compactionThreads = compactionThreads;
        this.validationThreads = validationThreads;
        this.activeErrata = activeErrata == null ? java.util.Collections.emptySet() : activeErrata;
        this.cleanupOnSuccess = cleanupOnSuccess;
        this.reporter = reporter;
        this.config = config;
    }

    /**
     * Runs the full pipeline and returns a {@link RunResult} describing the outcome.
     *
     * <p>This method never propagates exceptions: any unexpected failure is
     * captured in {@link RunResult#failureDetail} and returned as a failed run.
     */
    public RunResult execute()
    {
        long startMs = System.currentTimeMillis();
        reporter.onRunStart(runNumber, rootSeed);
        // Publish the per-run side labels so panels and reporters can swap their
        // hardcoded "LEGACY"/"CURSOR" headers for whatever the YAML config named
        // the two sides. Fired before any compaction event so panels see the
        // labels in time for first render.
        reporter.onComparisonLabels(config.comparison.controlName(),
                                    config.comparison.experimentName());

        SstableSetManager sstableSet = null;
        // Hoisted for the finally block: we need to release SSTableReader refs and drop the
        // per-run keyspaces from Schema.instance once a run finishes (success or failure),
        // otherwise Cassandra's Ref.OnLeak watchdog logs LEAK DETECTED for every output
        // SSTableReader on the next GC, and Schema.instance grows unboundedly across runs.
        ColumnFamilyStore legacyCfs = null;
        ColumnFamilyStore cursorCfs = null;
        String legacyKsForCleanup = null;
        String cursorKsForCleanup = null;
        // Set to true when the run reaches the validation-pass branch and the caller wants
        // cleanupOnSuccess. The directory-deletion step is in the finally block so it runs
        // AFTER cfs.invalidate() — invalidate writes a transaction log to the data dir.
        boolean shouldDeleteRunDir = false;
        RunResult.Builder builder = new RunResult.Builder()
            .seed(rootSeed)
            .runNumber(runNumber)
            .startTimeMs(startMs);

        try
        {
            // ---- Phase 0: directory setup --------------------------------------------------
            sstableSet = new SstableSetManager(workingDir, rootSeed);
            sstableSet.prepare();

            // ---- Phase 1: schema generation ------------------------------------------------
            GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
            reporter.onSchemaReady(schema);

            String baseKs = schema.getKeyspaceName();
            // Per-side keyspace suffixes derive from the configured side names so two
            // runs with different control/experiment labels get different keyspaces.
            // Sanitize to keep the name a legal CQL identifier.
            org.apache.cassandra.tools.compactionvalidator.config.SideConfig controlSide = config.comparison.control;
            org.apache.cassandra.tools.compactionvalidator.config.SideConfig experimentSide = config.comparison.experiment;
            String controlSuffix    = "_" + sanitizeKeyspacePart(config.comparison.controlName());
            String experimentSuffix = "_" + sanitizeKeyspacePart(config.comparison.experimentName());
            String legacyKs = baseKs + controlSuffix;
            String cursorKs = baseKs + experimentSuffix;
            legacyKsForCleanup = legacyKs;
            cursorKsForCleanup = cursorKs;

            builder.keyspaceName(baseKs)
                   .tableName(schema.getTableName())
                   .partitionKeyCount(schema.getPartitionKeys().size())
                   .clusteringKeyCount(schema.getClusteringKeys().size())
                   .staticColumnCount(schema.getStaticColumns().size())
                   .regularColumnCount(schema.getRegularColumns().size())
                   .schemaCql(schema.getCql())
                   .controlName(config.comparison.controlName())
                   .experimentName(config.comparison.experimentName());

            // Extract UCS / compression parameters from the CQL for the result record.
            CompactionParams params = parseCompactionParams(schema.getCql());
            builder.targetSstableSizeMiB(params.targetSstableSizeMiB)
                   .baseShardCount(params.baseShardCount)
                   .compressionChunkKb(params.compressionChunkKb);

            // ---- Phase 2: data generation --------------------------------------------------
            // Use the original (base) keyspace name for source generation; the per-side CFSes
            // get their own keyspaces with potentially different compaction/compression options.
            registerKeyspaceAndTable(baseKs, schema.getCql());

            long dataGenStartMs = System.currentTimeMillis();
            DataGenerator dataGen = new DataGenerator(schema,
                                                     sstableSet.sourceDir(),
                                                     targetBytes,
                                                     dataGenThreads,
                                                     rootSeed);

            // Spawn a daemon ticker that polls live data-gen stats every 250 ms and pushes
            // an onDataGenProgress event so the TUI's progress bar / counters / rates update
            // smoothly while DataGenerator.generate() is running. The ticker is shut down in
            // a finally block before the final progress + complete events are published, so
            // there's no race against the post-completion render.
            final DataGenStats liveStats = dataGen.getStats();
            final long dataGenTickerStartMs = dataGenStartMs;
            java.util.concurrent.ScheduledExecutorService dataGenTicker =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread th = new Thread(r, "compaction-validator-datagen-ticker");
                    th.setDaemon(true);
                    return th;
                });
            dataGenTicker.scheduleAtFixedRate(() -> {
                try
                {
                    long bytes = liveStats.estimatedBytesWritten();
                    long partitions = liveStats.getPartitionsWritten();
                    long rows = liveStats.getRowsWritten();
                    long elapsedMs = Math.max(1L, System.currentTimeMillis() - dataGenTickerStartMs);
                    double bps = bytes * 1000.0 / elapsedMs;
                    reporter.onDataGenProgress(bytes, targetBytes, partitions, rows, bps);
                }
                catch (Throwable ignored)
                {
                    // Reporter must not break datagen.
                }
            }, 250L, 250L, java.util.concurrent.TimeUnit.MILLISECONDS);

            DataGenStats dataGenStats;
            try
            {
                dataGenStats = dataGen.generate();
            }
            finally
            {
                dataGenTicker.shutdownNow();
            }
            long dataGenDurationMs = System.currentTimeMillis() - dataGenStartMs;
            dataGenStats.durationMs = dataGenDurationMs;
            builder.dataGenStats(dataGenStats);

            // Publish a final data-gen progress snapshot then mark the phase complete.
            double dataGenBps = dataGenDurationMs > 0
                                ? dataGenStats.getBytesWritten() * 1000.0 / dataGenDurationMs
                                : 0.0;
            reporter.onDataGenProgress(dataGenStats.getBytesWritten(),
                                       targetBytes,
                                       dataGenStats.getPartitionsWritten(),
                                       dataGenStats.getRowsWritten(),
                                       dataGenBps);
            reporter.onDataGenComplete(dataGenStats, dataGenDurationMs);

            // ---- Phase 3a: provision two offline ColumnFamilyStores ------------------------
            // Pre-compute each CFS's expected data path, mkdir it, and hard-link the source
            // SSTables into it BEFORE CFS construction. CFS construction calls
            // Directories.getUIDGenerator(SequenceBasedSSTableId.Builder.instance), which scans
            // the data directory to find the max existing id and advances the generator past it.
            // If we hard-linked AFTER construction, the generator would have started at 1 and
            // collided with our pre-existing pa-1-big files, tripping the
            // ColumnFamilyStore.newSSTableDescriptor assertion during compaction.
            File legacyDir = sstableSet.legacyOutputDir();
            File cursorDir = sstableSet.cursorOutputDir();

            // Per-side CQL: each side overlays its CompactionSpec / CompressionSpec on top
            // of the seed-picked defaults. Sides that omit either attribute inherit the
            // seed's value identically (so omitted attributes don't introduce variance).
            String legacyCql = schema.buildCqlForSide(legacyKs, controlSide);
            String cursorCql = schema.buildCqlForSide(cursorKs, experimentSide);

            // Register schema first; deterministic table ids let us predict the data path.
            org.apache.cassandra.schema.TableId legacyTableId = registerKeyspaceAndTable(legacyKs, legacyCql);
            org.apache.cassandra.schema.TableId cursorTableId = registerKeyspaceAndTable(cursorKs, cursorCql);

            File legacyCfsDir = expectedCfsDataDir(legacyDir, legacyKs, schema.getTableName(), legacyTableId);
            File cursorCfsDir = expectedCfsDataDir(cursorDir, cursorKs, schema.getTableName(), cursorTableId);
            if (!legacyCfsDir.exists() && !legacyCfsDir.mkdirs())
                throw new java.io.IOException("Could not create CFS data directory: " + legacyCfsDir);
            if (!cursorCfsDir.exists() && !cursorCfsDir.mkdirs())
                throw new java.io.IOException("Could not create CFS data directory: " + cursorCfsDir);
            sstableSet.hardLinkSourceTo(legacyCfsDir);
            sstableSet.hardLinkSourceTo(cursorCfsDir);

            ColumnFamilyStore legacyCfsLocal = createOfflineCfs(legacyKs,
                                                          schema.getTableName(),
                                                          legacyCql,
                                                          legacyDir);
            ColumnFamilyStore cursorCfsLocal = createOfflineCfs(cursorKs,
                                                          schema.getTableName(),
                                                          cursorCql,
                                                          cursorDir);
            legacyCfs = legacyCfsLocal;
            cursorCfs = cursorCfsLocal;

            // ---- Phase 3b: open SSTables and register with their CFS -----------------------
            loadSSTables(legacyCfsLocal);
            loadSSTables(cursorCfsLocal);

            // ---- Phase 4: parallel compaction ----------------------------------------------
            // Map each side's pipeline string ("ITERATOR" / "CURSOR") to a backend enum,
            // falling back to the historical default (ITERATOR for control, CURSOR for
            // experiment) when the config omits a value — this keeps legacy-vs-cursor
            // runs working with a minimal config and lets non-pipeline comparisons
            // (e.g. UCS-vs-LCS with both on iterator) stay symmetric on the pipeline axis.
            org.apache.cassandra.db.compaction.PipelineSelector.Backend controlBackend  =
                resolvePipeline(controlSide.pipeline,
                                org.apache.cassandra.db.compaction.PipelineSelector.Backend.ITERATOR);
            org.apache.cassandra.db.compaction.PipelineSelector.Backend experimentBackend =
                resolvePipeline(experimentSide.pipeline,
                                org.apache.cassandra.db.compaction.PipelineSelector.Backend.CURSOR);

            ParallelCompactor.Result compactionResult = new ParallelCompactor(legacyCfsLocal, cursorCfsLocal,
                                                                              controlBackend, experimentBackend,
                                                                              compactionThreads, reporter).run();
            builder.legacyStats(compactionResult.legacyStats)
                   .cursorStats(compactionResult.cursorStats)
                   .schemaShape(schema.getShape().name())
                   .gcGraceSeconds(schema.getGcGraceSeconds());

            // ---- Phase 4b: format-level invariant audit on the experiment side -------------
            // Walks the experiment-side SSTables byte-by-byte and asserts cell-flag /
            // ordering invariants that the standard SSTable reader is too tolerant to
            // surface. Catches compaction-bug 1A (IS_DELETED + IS_EXPIRING set in the
            // same flags byte) and out-of-order rows. Cheap: O(scanned bytes) and adds
            // a few seconds to a 1 GiB run. We only audit the experiment side — the
            // control side is the format reference, and any divergence relative to
            // itself would be a different (more serious) class of bug.
            //
            // Wrapped in a fresh ArrayList because the validator below may also append
            // SCANNER_FAILURE violations on a CorruptSSTableException, and
            // FormatAuditor.audit returns an empty immutable list when given no input.
            java.util.List<FormatViolation> formatViolations = new java.util.ArrayList<>(
                FormatAuditor.audit(compactionResult.cursorSSTables,
                                    cursorCfsLocal.metadata().comparator));

            // ---- Phase 5: validation -------------------------------------------------------
            ValidationStats validationStats = new ValidationStats();

            Validator validator = new Validator(legacyCfs,
                                                cursorCfs,
                                                compactionResult.legacySSTables,
                                                compactionResult.cursorSSTables,
                                                validationStats,
                                                validationThreads,
                                                activeErrata);
            // Spawn a daemon ticker that polls validationStats every 250 ms and pushes
            // onValidationProgress so the TUI's bar updates while validator.validate() is
            // running. Validator itself doesn't have a reporter handle (tests/CFS-only);
            // this keeps the responsibility in the orchestrator. Shut down in a finally
            // block before the post-validation events fire so there's no race.
            final ValidationStats validationStatsRef = validationStats;
            final int legacySstableCount = compactionResult.legacySSTables == null
                                           ? 0 : compactionResult.legacySSTables.size();
            final int cursorSstableCount = compactionResult.cursorSSTables == null
                                           ? 0 : compactionResult.cursorSSTables.size();
            java.util.concurrent.ScheduledExecutorService validateTicker =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread th = new Thread(r, "compaction-validator-validate-ticker");
                    th.setDaemon(true);
                    return th;
                });
            validateTicker.scheduleAtFixedRate(() -> {
                try { reporter.onValidationProgress(validationStatsRef.partitionsChecked.get()); }
                catch (Throwable ignored) {}
            }, 250L, 250L, java.util.concurrent.TimeUnit.MILLISECONDS);

            Optional<MismatchReport> mismatch;
            try
            {
                mismatch = validator.validate();
            }
            catch (org.apache.cassandra.io.sstable.CorruptSSTableException corrupt)
            {
                // Validator scanner hit an unreadable experiment-side SSTable. This
                // is the documented cascading symptom of compaction bug 1A: once a
                // flag-byte-misaligned cell desynchronises the deserialiser, every
                // subsequent cell in the SSTable looks malformed and reads throw.
                //
                // Convert to a SCANNER_FAILURE FormatViolation so the standard
                // suppression logic below decides whether to fail the run (errata
                // active → suppressed, soak loop rides through) or surface it
                // (errata inactive → fail with the same detail the auditor would
                // have produced). Throwing through here would have terminated the
                // run on the first corrupted SSTable, bypassing the suppression
                // path entirely.
                String path = corrupt.path != null
                              ? corrupt.path.toString()
                              : "(unknown experiment-side SSTable)";
                String detail = corrupt.getCause() != null
                                ? corrupt.getCause().getClass().getSimpleName()
                                  + (corrupt.getCause().getMessage() != null
                                     ? ": " + corrupt.getCause().getMessage() : "")
                                : corrupt.getClass().getSimpleName()
                                  + (corrupt.getMessage() != null
                                     ? ": " + corrupt.getMessage() : "");
                formatViolations.add(new FormatViolation(
                    FormatViolation.Kind.SCANNER_FAILURE,
                    path,
                    "(validator scan failed)",
                    detail));
                mismatch = Optional.empty();
            }
            finally
            {
                validateTicker.shutdownNow();
            }

            // Now that the validator (and any of its scanner-side exceptions) has
            // contributed to formatViolations, classify the full list. Suppressed
            // violations bump their rule's counter in ValidationStats so the
            // Errata: line in the run log reports them alongside Phase A mismatches;
            // unsuppressed violations are folded into the failure decision below.
            java.util.List<FormatViolation> unsuppressedViolations = new java.util.ArrayList<>();
            for (FormatViolation v : formatViolations)
            {
                org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule rule =
                    org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule
                        .forFormatViolation(v.kind);
                if (rule != null && activeErrata.contains(rule))
                    validationStats.recordErrata(rule);
                else
                    unsuppressedViolations.add(v);
            }

            builder.formatViolations(formatViolations);
            builder.validationStats(validationStats);

            // ---- Result assembly -----------------------------------------------------------
            long endMs = System.currentTimeMillis();
            builder.endTimeMs(endMs);

            if (mismatch.isPresent())
            {
                MismatchReport report = mismatch.get();
                String preserved = sstableSet.runDir().getAbsolutePath();
                RunResult result = builder.success(false)
                                          .failureDetail(report.description)
                                          .preservedDir(preserved)
                                          .mismatchReport(report)
                                          .build();
                reporter.onValidationComplete(validationStats, false);
                reporter.onRunFailed(report, result);
                return result;
            }

            // Phase A passed: now decide whether the FormatAuditor's unsuppressed
            // violations should fail the run. They might not have surfaced as Phase A
            // mismatches if the bug's symptom is intra-cell metadata (e.g. a broken
            // expiration formula whose bytes happen to hash the same as legacy's
            // expiring cell). Treat any unsuppressed violation as a failure so we
            // don't silently pass when the auditor saw something the hash sweep
            // missed.
            if (!unsuppressedViolations.isEmpty())
            {
                FormatViolation first = unsuppressedViolations.get(0);
                String detail = String.format(
                    "FormatAuditor reported %d unsuppressed violation(s); first: [%s] %s — %s",
                    unsuppressedViolations.size(),
                    first.kind.cliName(),
                    first.partitionKeyHex,
                    first.detail);
                String preserved = sstableSet.runDir().getAbsolutePath();
                MismatchReport synthetic = new MismatchReport(
                    first.partitionKeyHex,
                    detail,
                    "(no cell-level dump: format-violation failure, see Format: block in run log)",
                    "(no cell-level dump: format-violation failure, see Format: block in run log)",
                    validationStats.partitionsChecked.get());
                RunResult result = builder.success(false)
                                          .failureDetail(detail)
                                          .preservedDir(preserved)
                                          .mismatchReport(synthetic)
                                          .build();
                reporter.onValidationComplete(validationStats, false);
                reporter.onRunFailed(synthetic, result);
                return result;
            }

            reporter.onValidationComplete(validationStats, true);
            RunResult result = builder.success(true).build();

            // Defer the actual directory deletion to the finally block so we can run it
            // AFTER cfs.invalidate() — invalidate writes a transaction log into the data
            // dir to obsolete its SSTables, so the dir must still exist when it runs.
            if (cleanupOnSuccess)
            {
                shouldDeleteRunDir = true;
            }
            else
            {
                builder.preservedDir(sstableSet.runDir().getAbsolutePath());
                // Re-build with the preserved-dir field set so the caller sees it.
                result = builder.success(true)
                                .preservedDir(sstableSet.runDir().getAbsolutePath())
                                .build();
            }

            reporter.onRunComplete(result);
            return result;
        }
        catch (Throwable t)
        {
            // Any failure path: report a failed run with the exception message.
            logger.error("Run #{} (seed={}) failed with exception",
                         runNumber, SeedUtil.toHex(rootSeed), t);
            long endMs = System.currentTimeMillis();
            String preserved = sstableSet != null ? sstableSet.runDir().getAbsolutePath() : null;
            String detail = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();

            RunResult failure = builder.success(false)
                                       .endTimeMs(endMs)
                                       .failureDetail(detail)
                                       .preservedDir(preserved)
                                       .build();

            // Best-effort: notify the reporter of failure so any TUI updates correctly.
            try
            {
                ValidationStats vs = failure.validationStats != null
                                     ? failure.validationStats
                                     : new ValidationStats();
                reporter.onValidationComplete(vs, false);
                reporter.onRunFailed(new MismatchReport("(none)",
                                                       detail,
                                                       "(no dump: exception during run)",
                                                       "(no dump: exception during run)",
                                                       0L),
                                     failure);
            }
            catch (Throwable reportEx)
            {
                logger.debug("Reporter failure during exception handling: {}", reportEx.toString());
            }
            return failure;
        }
        finally
        {
            // Release SSTableReader refs and drop per-run keyspaces from Schema.instance.
            // Without this, every output SSTableReader hangs on its CFS (which goes out of
            // scope at end of run), the next GC tidies the unreleased Refs, and Cassandra's
            // leak watchdog logs many "LEAK DETECTED" lines per run. Also prevents the
            // global Schema.instance from accumulating a keyspace per run for the lifetime
            // of the process. Wrapped in inner try/catch so cleanup itself can never break
            // the run result.
            // CFS teardown: dropData=true releases SSTable refs AND deletes their files via
            // a lifecycle transaction log. We pass dropData=true on the cleanup path (the run
            // dir is about to be wiped anyway) and dropData=false when --no-cleanup was set
            // so the user can inspect the final compacted output sstables on disk.
            //
            // The trade-off with dropData=false: SSTableReaders aren't released, so the next
            // GC cycle logs "LEAK DETECTED" warnings for each one. Acceptable for --no-cleanup
            // which is typically used with --once for repro inspection (process exits before
            // a leak-triggering GC runs anyway). Users who run continuously with --no-cleanup
            // will see leak warnings — that's the cost of keeping output files on disk.
            boolean dropData = shouldDeleteRunDir;
            if (legacyCfs != null)
            {
                try { legacyCfs.invalidate(/* expectMBean = */ false, dropData); }
                catch (Throwable t) { logger.debug("legacy CFS invalidate failed: {}", t.toString()); }
            }
            if (cursorCfs != null)
            {
                try { cursorCfs.invalidate(false, dropData); }
                catch (Throwable t) { logger.debug("cursor CFS invalidate failed: {}", t.toString()); }
            }
            if (legacyKsForCleanup != null)
                tryDropKeyspace(legacyKsForCleanup);
            if (cursorKsForCleanup != null)
                tryDropKeyspace(cursorKsForCleanup);

            // Now that all CFS state is torn down (no more open SSTableReaders, no more
            // pending obsoletion transaction logs), it's safe to wipe the run directory.
            if (shouldDeleteRunDir && sstableSet != null)
            {
                try
                {
                    sstableSet.cleanupAll();
                }
                catch (Exception cleanupEx)
                {
                    logger.warn("Cleanup of run directory {} failed: {}",
                                sstableSet.runDir().getAbsolutePath(), cleanupEx.toString());
                }
            }
        }
    }

    /**
     * Strips characters that aren't legal in a CQL identifier from a side name and
     * lowercases what's left. The result is appended to the base keyspace as a
     * suffix, so we don't need quoting — just match Cassandra's identifier grammar
     * (letters, digits, underscores).
     */
    private static String sanitizeKeyspacePart(String name)
    {
        if (name == null || name.isEmpty())
            return "side";
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++)
        {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')
                sb.append(c);
            else
                sb.append('_');
        }
        if (sb.length() == 0 || !Character.isLetter(sb.charAt(0)))
            sb.insert(0, 's');  // CQL identifiers must start with a letter
        return sb.toString();
    }

    /**
     * Maps the user-supplied pipeline name (or null) to a
     * {@link org.apache.cassandra.db.compaction.PipelineSelector.Backend}.
     * Accepts the canonical {@code ITERATOR} / {@code CURSOR} spellings plus a few
     * convenience aliases ({@code legacy}/{@code iter} for ITERATOR).
     */
    private static org.apache.cassandra.db.compaction.PipelineSelector.Backend resolvePipeline(
        String name,
        org.apache.cassandra.db.compaction.PipelineSelector.Backend defaultBackend)
    {
        if (name == null || name.trim().isEmpty())
            return defaultBackend;
        String n = name.trim().toUpperCase();
        switch (n)
        {
            case "ITERATOR":
            case "LEGACY":
            case "ITER":
                return org.apache.cassandra.db.compaction.PipelineSelector.Backend.ITERATOR;
            case "CURSOR":
                return org.apache.cassandra.db.compaction.PipelineSelector.Backend.CURSOR;
            default:
                throw new IllegalArgumentException("Unknown pipeline '" + name
                                                   + "' (expected ITERATOR or CURSOR)");
        }
    }

    /** Best-effort drop of {@code keyspaceName} from the global Schema. Never throws. */
    private static void tryDropKeyspace(String keyspaceName)
    {
        try
        {
            Schema.instance.submit(new SchemaTransformation()
            {
                @Override
                public Keyspaces apply(org.apache.cassandra.tcm.ClusterMetadata metadata)
                {
                    return metadata.schema.getKeyspaces().without(keyspaceName);
                }

                @Override
                public boolean compatibleWith(org.apache.cassandra.tcm.ClusterMetadata metadata)
                {
                    return metadata.directory.commonSerializationVersion
                                             .isAtLeast(org.apache.cassandra.tcm.serialization.Version.V0);
                }
            });
        }
        catch (Throwable t)
        {
            logger.debug("Drop of keyspace {} failed: {}", keyspaceName, t.toString());
        }
    }

    // -------------------------------------------------------------------------
    // Schema / CFS helpers
    // -------------------------------------------------------------------------

    /**
     * Registers the supplied keyspace plus the table described by {@code cql} in
     * the local schema if they do not already exist.  Idempotent: subsequent
     * invocations with the same names are no-ops.
     *
     * <p>The table id is computed deterministically from {@code keyspaceName.tableName}
     * so the on-disk data directory ({@code <dataDir>/<keyspace>/<table>-<tableId>}) can
     * be predicted before the CFS is created — important so source SSTables can be
     * hard-linked into that directory ahead of CFS construction (otherwise the CFS's
     * SSTable id generator would scan an empty directory and later collide with
     * post-construction file additions).
     */
    private static org.apache.cassandra.schema.TableId registerKeyspaceAndTable(String keyspaceName, String cql)
    {
        // Ensure the keyspace exists.
        KeyspaceMetadata existing = Schema.instance.getKeyspaceMetadata(keyspaceName);
        if (existing == null)
        {
            KeyspaceMetadata ksm = KeyspaceMetadata.create(keyspaceName,
                                                           KeyspaceParams.simple(1),
                                                           Tables.none(),
                                                           Views.none(),
                                                           Types.none(),
                                                           UserFunctions.none());
            Schema.instance.submit(SchemaTransformations.addKeyspace(ksm, true));
        }

        // Ensure the table exists.
        CreateTableStatement.Raw rawStatement = QueryProcessor.parseStatement(cql,
                                                                              CreateTableStatement.Raw.class,
                                                                              "CREATE TABLE");
        String tableName = rawStatement.table();
        TableMetadata existingTable = Schema.instance.getTableMetadata(keyspaceName, tableName);
        if (existingTable != null)
            return existingTable.id;

        ClientState state = ClientState.forInternalCalls();
        CreateTableStatement statement = rawStatement.prepare(state);
        statement.validate(state);

        org.apache.cassandra.schema.TableId deterministicId = deterministicTableId(keyspaceName, tableName);
        TableMetadata tableMetadata = statement.builder(Types.none(), UserFunctions.none())
                                                .id(deterministicId)
                                                .build();
        Schema.instance.submit(SchemaTransformations.addTable(tableMetadata, true));
        return deterministicId;
    }

    /** Computes a deterministic {@link org.apache.cassandra.schema.TableId} from keyspace+table name. */
    private static org.apache.cassandra.schema.TableId deterministicTableId(String keyspaceName, String tableName)
    {
        byte[] key = (keyspaceName + "." + tableName).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.apache.cassandra.schema.TableId.fromUUID(java.util.UUID.nameUUIDFromBytes(key));
    }

    /**
     * Returns the on-disk data directory the CFS will use:
     * {@code <dataDir>/<keyspace>/<tableName>-<tableId.toHexString()>}.
     * Mirrors {@link org.apache.cassandra.db.Directories} 2.1+ layout via
     * {@link TableMetadata#getTableDirectoryName()}.
     */
    private static File expectedCfsDataDir(File dataDir, String keyspaceName, String tableName,
                                           org.apache.cassandra.schema.TableId tableId)
    {
        return new File(new File(dataDir, keyspaceName), tableName + "-" + tableId.toHexString());
    }

    /**
     * Creates an offline {@link ColumnFamilyStore} pointing at {@code dataDir} for
     * the keyspace + table described by {@code cql}.
     *
     * <p>The keyspace and table are first registered in the local schema (idempotent).
     * A {@link Directories} object is then constructed with a single
     * {@link Directories.DataDirectory} pointing at {@code dataDir}, and a fresh CFS
     * is built with that directory.  The CFS is registered against the per-keyspace
     * {@link Keyspace} instance returned by {@link Keyspace#openWithoutSSTables}.
     */
    private static ColumnFamilyStore createOfflineCfs(String keyspaceName,
                                                      String tableName,
                                                      String cql,
                                                      File dataDir)
    {
        registerKeyspaceAndTable(keyspaceName, cql);

        TableMetadata tableMetadata = Schema.instance.getTableMetadata(keyspaceName, tableName);
        if (tableMetadata == null)
            throw new IllegalStateException("table metadata for " + keyspaceName + '.' + tableName
                                            + " was not registered");

        // Build a Directories restricted to this run's per-backend output directory so
        // the CFS only ever sees files in that directory.
        org.apache.cassandra.io.util.File dataDirCassandra =
            new org.apache.cassandra.io.util.File(dataDir.toPath());
        Directories.DataDirectory[] dataDirectories = {
            new Directories.DataDirectory(dataDirCassandra)
        };
        Directories directories = new Directories(tableMetadata, dataDirectories);

        Keyspace keyspace = Keyspace.openWithoutSSTables(keyspaceName);
        if (keyspace == null)
            throw new IllegalStateException("keyspace " + keyspaceName + " was not registered");

        return ColumnFamilyStore.createColumnFamilyStore(keyspace,
                                                         tableName,
                                                         tableMetadata,
                                                         directories,
                                                         /* loadSSTables = */ false,
                                                         /* registerBookkeeping = */ false,
                                                         /* addIndexes = */ false);
    }

    /**
     * Opens every SSTable in {@code cfs}'s data directory and registers it with the CFS.
     *
     * <p>Auto-compaction is disabled before the SSTables are added so that the
     * orchestrator drives compaction explicitly via the
     * {@link org.apache.cassandra.tools.compactionvalidator.compaction.CompactionDriver}.
     */
    private static void loadSSTables(ColumnFamilyStore cfs) throws Exception
    {
        Directories.SSTableLister lister = cfs.getDirectories()
                                              .sstableLister(Directories.OnTxnErr.IGNORE)
                                              .skipTemporary(true);

        List<SSTableReader> sstables = new ArrayList<>();
        for (Map.Entry<Descriptor, Set<Component>> entry : lister.list().entrySet())
        {
            Set<Component> components = entry.getValue();
            if (!components.contains(SSTableFormat.Components.DATA))
                continue;

            SSTableReader sstable = SSTableReader.openNoValidation(entry.getKey(), components, cfs);
            sstables.add(sstable);
        }

        cfs.disableAutoCompaction();
        cfs.addSSTables(sstables);
    }

    /**
     * Returns a copy of {@code cql} in which every occurrence of the substring
     * {@code "<oldKs>.<tableName>"} is replaced with {@code "<newKs>.<tableName>"}.
     *
     * <p>This is a syntactic rewrite that targets the keyspace-qualified table
     * reference produced by {@link SchemaGenerator}; the CQL never embeds the
     * keyspace name in a string literal so a plain replacement is safe.
     */
    static String renameKeyspace(String cql, String oldKs, String newKs, String tableName)
    {
        if (cql == null || oldKs == null || newKs == null || tableName == null)
            throw new IllegalArgumentException("cql, oldKs, newKs and tableName must all be non-null");
        if (oldKs.equals(newKs))
            return cql;

        String oldRef = oldKs + '.' + tableName;
        String newRef = newKs + '.' + tableName;
        return cql.replace(oldRef, newRef);
    }

    // -------------------------------------------------------------------------
    // CQL parameter scraping
    // -------------------------------------------------------------------------

    private static final Pattern TARGET_SSTABLE_SIZE_PATTERN =
        Pattern.compile("'target_sstable_size'\\s*:\\s*'(\\d+)\\s*MiB'", Pattern.CASE_INSENSITIVE);

    private static final Pattern BASE_SHARD_COUNT_PATTERN =
        Pattern.compile("'base_shard_count'\\s*:\\s*'(\\d+)'", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHUNK_LENGTH_PATTERN =
        Pattern.compile("'chunk_length_in_kb'\\s*:\\s*'(\\d+)'", Pattern.CASE_INSENSITIVE);

    /**
     * Best-effort extraction of UCS / compression parameters from the generated
     * CREATE TABLE CQL.  Defaults are returned for any parameter that cannot be
     * located.
     */
    static CompactionParams parseCompactionParams(String cql)
    {
        long targetSize = 0L;
        Matcher m = TARGET_SSTABLE_SIZE_PATTERN.matcher(cql);
        if (m.find())
        {
            try
            {
                targetSize = Long.parseLong(m.group(1));
            }
            catch (NumberFormatException ignored)
            {
                // leave at default 0
            }
        }

        int baseShard = 0;
        m = BASE_SHARD_COUNT_PATTERN.matcher(cql);
        if (m.find())
        {
            try
            {
                baseShard = Integer.parseInt(m.group(1));
            }
            catch (NumberFormatException ignored)
            {
                // leave at default 0
            }
        }

        int chunkKb = 0;
        m = CHUNK_LENGTH_PATTERN.matcher(cql);
        if (m.find())
        {
            try
            {
                chunkKb = Integer.parseInt(m.group(1));
            }
            catch (NumberFormatException ignored)
            {
                // leave at default 0
            }
        }
        return new CompactionParams(targetSize, baseShard, chunkKb);
    }

    /** Tiny holder for compaction-related parameters scraped from the CREATE TABLE CQL. */
    static final class CompactionParams
    {
        final long targetSstableSizeMiB;
        final int baseShardCount;
        final int compressionChunkKb;

        CompactionParams(long targetSstableSizeMiB, int baseShardCount, int compressionChunkKb)
        {
            this.targetSstableSizeMiB = targetSstableSizeMiB;
            this.baseShardCount = baseShardCount;
            this.compressionChunkKb = compressionChunkKb;
        }
    }
}
