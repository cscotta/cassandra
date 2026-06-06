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
package org.apache.cassandra.tools.compactionvalidator.compaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.PipelineSelector;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.tools.compactionvalidator.ProgressTap;
import org.apache.cassandra.tools.compactionvalidator.SstableInfo;
import org.apache.cassandra.utils.FBUtilities;

/**
 * Runs two {@link CompactionDriver}s — one for the legacy iterator pipeline and one for
 * the cursor pipeline — concurrently, against two independent
 * {@link ColumnFamilyStore}s that have been initialised with byte-identical inputs.
 *
 * <p>The two drivers are executed on separate threads provided by a fixed-size pool of
 * two so that wall-clock comparisons (legacy duration vs cursor duration) reflect
 * realistic concurrent load — neither side starves the other.  The pool is shut down
 * before the call returns whether the runs succeed or fail.
 *
 * <p>Failures: if either driver throws, the exception is unwrapped from
 * {@link ExecutionException} and re-thrown to the caller after the other side is also
 * waited on.  This avoids leaking a partially completed run.
 */
public final class ParallelCompactor
{
    private final ColumnFamilyStore legacyCfs;
    private final ColumnFamilyStore cursorCfs;
    private final PipelineSelector.Backend legacyBackend;
    private final PipelineSelector.Backend cursorBackend;
    /**
     * Per-side SSTable output format. {@code null} → use whatever the JVM-global
     * {@link DatabaseDescriptor#getSelectedSSTableFormat()} returns at compaction
     * time (BIG by default, set in {@code Main.bootstrapJvm}).
     *
     * <p>When the two sides specify the <em>same</em> format (or both are
     * {@code null}), {@link #run()} dispatches the two compactions in parallel
     * as before. When they differ, {@code run()} falls back to a serial
     * implementation that toggles
     * {@link DatabaseDescriptor#setSelectedSSTableFormat} around each side —
     * format selection is JVM-global at write time and there's no per-CFS
     * override hook in Cassandra's {@code CompactionAwareWriter} path.
     */
    private final SSTableFormat<?, ?> legacyFormat;
    private final SSTableFormat<?, ?> cursorFormat;
    private final int taskConcurrencyPerBackend;
    private final ProgressTap reporter;

    /**
     * @param legacyCfs CFS configured to read from the legacy/output-legacy directory
     * @param cursorCfs CFS configured to read from the cursor/output-cursor directory
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs, ColumnFamilyStore cursorCfs)
    {
        this(legacyCfs, cursorCfs,
             PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR,
             null, null, 1, null);
    }

    /**
     * Variant that publishes per-progress events to the supplied reporter so a TUI can
     * display live throughput. Pass {@code null} to disable reporting. Pipeline backends
     * default to ITERATOR for the legacy slot and CURSOR for the cursor slot.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             ProgressTap reporter)
    {
        this(legacyCfs, cursorCfs,
             PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR,
             null, null, 1, reporter);
    }

    /**
     * Variant with task concurrency. Pipeline backends default to ITERATOR for the
     * legacy slot and CURSOR for the cursor slot.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             int taskConcurrencyPerBackend,
                             ProgressTap reporter)
    {
        this(legacyCfs, cursorCfs,
             PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR,
             null, null, taskConcurrencyPerBackend, reporter);
    }

    /**
     * Backwards-compatible constructor without per-side format selection.
     * Equivalent to passing {@code null} for both formats — both sides use the
     * JVM-global {@link DatabaseDescriptor#getSelectedSSTableFormat()}.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             PipelineSelector.Backend legacyBackend,
                             PipelineSelector.Backend cursorBackend,
                             int taskConcurrencyPerBackend,
                             ProgressTap reporter)
    {
        this(legacyCfs, cursorCfs, legacyBackend, cursorBackend,
             null, null, taskConcurrencyPerBackend, reporter);
    }

    /**
     * Full constructor. Each side names its own pipeline backend so a comparison can
     * exercise iterator-vs-cursor (the original use case), iterator-vs-iterator (testing
     * compaction strategies on the same pipeline), or cursor-vs-cursor (testing compression
     * codecs on the cursor pipeline). Each side may also pin its own SSTable
     * output format (BIG / BTI); {@code null} on either side means "use the
     * JVM-global default."
     *
     * <p>Both sides share the same {@code taskConcurrencyPerBackend} value so that the
     * cursor-vs-legacy speedup measurement isn't perturbed by asymmetric parallelism.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             PipelineSelector.Backend legacyBackend,
                             PipelineSelector.Backend cursorBackend,
                             SSTableFormat<?, ?> legacyFormat,
                             SSTableFormat<?, ?> cursorFormat,
                             int taskConcurrencyPerBackend,
                             ProgressTap reporter)
    {
        if (legacyCfs == null)
            throw new IllegalArgumentException("legacyCfs must not be null");
        if (cursorCfs == null)
            throw new IllegalArgumentException("cursorCfs must not be null");
        if (legacyBackend == null || cursorBackend == null)
            throw new IllegalArgumentException("legacyBackend and cursorBackend must be non-null");
        if (taskConcurrencyPerBackend <= 0)
            throw new IllegalArgumentException("taskConcurrencyPerBackend must be positive");
        this.legacyCfs = legacyCfs;
        this.cursorCfs = cursorCfs;
        this.legacyBackend = legacyBackend;
        this.cursorBackend = cursorBackend;
        this.legacyFormat = legacyFormat;
        this.cursorFormat = cursorFormat;
        this.taskConcurrencyPerBackend = taskConcurrencyPerBackend;
        this.reporter = reporter;
    }

    /**
     * Runs both compactions to completion concurrently and returns their outputs.
     *
     * <p>Equally weighted: both sides start at the same instant and are waited for in
     * turn.  Per-side timing is captured in the returned {@link Result#legacyStats}
     * and {@link Result#cursorStats}.
     *
     * @return a {@link Result} bundling the two stats objects and the two output SSTable lists
     * @throws Exception if either side throws (the originating exception is rethrown directly)
     */
    public Result run() throws Exception
    {
        // Same format on both sides (or both unspecified) → run in parallel as
        // before. Different formats → must run serially because format selection
        // is JVM-global at write time and there's no per-CFS override hook in
        // CompactionAwareWriter — we toggle the global around each side.
        if (formatsCompatible(legacyFormat, cursorFormat))
            return runParallel();
        return runSerial();
    }

    /**
     * Returns {@code true} when the two sides can compact concurrently —
     * either both omit a format (use the JVM default) or both pin the same
     * format. Cross-format runs need serial execution; see {@link #runSerial}.
     */
    private static boolean formatsCompatible(SSTableFormat<?, ?> a, SSTableFormat<?, ?> b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.name().equals(b.name());
    }

    private Result runParallel() throws Exception
    {
        CompactionStats legacyStats = new CompactionStats();
        CompactionStats cursorStats = new CompactionStats();

        // Capture wall-clock NOW once and pin it across both pipelines. Without this,
        // the two CompactionDrivers each call FBUtilities.nowInSeconds() at slightly
        // different points in their setup, so a near-expiry cell or a near-gc-grace
        // tombstone might be dropped by one side and kept by the other — a spurious
        // post-compaction divergence flagged by Phase A even though both pipelines are
        // correct. Pinning the value forces identical GC / TTL purge decisions and
        // eliminates the false-positive class entirely.
        final long pinnedNowInSec = FBUtilities.nowInSeconds();

        // If a format is pinned (and both sides agree), set it once before the
        // parallel pool starts so both threads see the right global. {@code null}
        // means "use whatever's already set" — typically BIG from bootstrapJvm.
        SSTableFormat<?, ?> previousGlobal = DatabaseDescriptor.getSelectedSSTableFormat();
        SSTableFormat<?, ?> sharedFormat = legacyFormat != null ? legacyFormat : cursorFormat;
        if (sharedFormat != null && sharedFormat != previousGlobal)
            DatabaseDescriptor.setSelectedSSTableFormat(sharedFormat);

        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("compaction-validator-pair"));
        try
        {
            // Each Callable fires onCompactionComplete the instant its own driver returns,
            // not when both finish. Without this, a backend that finishes first would have
            // its TUI rate metric keep decrementing as wall-clock time advances against a
            // frozen partition count — partsPerSec stays computed from elapsed = now - start
            // until frozenDurationMs is set, which previously only happened after BOTH sides
            // had completed.
            Callable<Collection<SSTableReader>> legacyTask = () -> {
                Collection<SSTableReader> r = new CompactionDriver(legacyCfs,
                                                                   legacyBackend,
                                                                   legacyStats,
                                                                   taskConcurrencyPerBackend,
                                                                   reporter,
                                                                   pinnedNowInSec,
                                                                   "legacy").run();
                if (reporter != null)
                {
                    try { reporter.onCompactionComplete("legacy", legacyStats); }
                    catch (Throwable ignored) {}
                }
                return r;
            };
            Callable<Collection<SSTableReader>> cursorTask = () -> {
                Collection<SSTableReader> r = new CompactionDriver(cursorCfs,
                                                                   cursorBackend,
                                                                   cursorStats,
                                                                   taskConcurrencyPerBackend,
                                                                   reporter,
                                                                   pinnedNowInSec,
                                                                   "cursor").run();
                if (reporter != null)
                {
                    try { reporter.onCompactionComplete("cursor", cursorStats); }
                    catch (Throwable ignored) {}
                }
                return r;
            };

            Future<Collection<SSTableReader>> legacyFuture = pool.submit(legacyTask);
            Future<Collection<SSTableReader>> cursorFuture = pool.submit(cursorTask);

            // Always wait for both even if one throws, then propagate the failure.
            Collection<SSTableReader> legacyOut = null;
            Collection<SSTableReader> cursorOut = null;
            Exception failure = null;

            try
            {
                legacyOut = legacyFuture.get();
            }
            catch (ExecutionException ee)
            {
                failure = unwrap(ee);
            }

            try
            {
                cursorOut = cursorFuture.get();
            }
            catch (ExecutionException ee)
            {
                Exception cursorFailure = unwrap(ee);
                if (failure == null)
                    failure = cursorFailure;
                else
                    failure.addSuppressed(cursorFailure);
            }

            if (failure != null)
                throw failure;

            return new Result(legacyStats, cursorStats, legacyOut, cursorOut);
        }
        finally
        {
            pool.shutdownNow();
            // Restore the global format if we changed it. The orchestrator's next
            // run will reset it from its own configured side anyway, but we leave
            // the JVM in the same state we found it for cleanliness.
            if (sharedFormat != null && sharedFormat != previousGlobal)
                DatabaseDescriptor.setSelectedSSTableFormat(previousGlobal);
        }
    }

    /**
     * Cross-format dispatch: toggle the JVM-global format between sides and run
     * them serially. {@code DatabaseDescriptor.setSelectedSSTableFormat} controls
     * which {@code SSTableFormat} {@code ColumnFamilyStore.newSSTableDescriptor}
     * picks for compaction-output writers, so we must set the right format on the
     * thread that constructs each side's {@code CompactionAwareWriter}.
     *
     * <p>Both sides still share a single pinned {@code nowInSec} captured before
     * either runs — we lose parallelism for cross-format runs but we cannot
     * lose the clock-pinning safeguard, otherwise GCGS=0 runs would spuriously
     * diverge whenever the two compactions straddle a wall-clock-second
     * boundary (gcBefore = nowInSec − gcGraceSeconds, so each side picking its
     * own nowInSec means each side gets a slightly different gcBefore and they
     * disagree on which tombstones are purgeable).
     */
    private Result runSerial() throws Exception
    {
        CompactionStats legacyStats = new CompactionStats();
        CompactionStats cursorStats = new CompactionStats();

        // Pin nowInSec ONCE so both sides see the same gcBefore. Same reasoning
        // as runParallel(); the only difference is that here both sides run on
        // this thread sequentially while format-toggling.
        final long pinnedNowInSec = FBUtilities.nowInSeconds();

        // Pre-emit onCompactionInputs for BOTH sides at the start of the serial
        // run. Without this, the not-yet-running side's TUI panel sits empty
        // for the duration of the first side's compaction and looks broken;
        // pre-emitting lets both panels render their input SSTables and a
        // "queued" status the moment the phase begins. The legacy side's
        // CompactionDriver will fire onCompactionInputs again with the same
        // payload when it actually starts, which is a harmless re-render.
        if (reporter != null)
        {
            try { reporter.onCompactionInputs("legacy", liveInputsOf(legacyCfs)); }
            catch (Throwable ignored) {}
            try { reporter.onCompactionInputs("cursor", liveInputsOf(cursorCfs)); }
            catch (Throwable ignored) {}
            // Mark the cursor side as queued so its panel doesn't look frozen
            // while the legacy side compacts. The first CompactionTaskStart on
            // the cursor side will replace this string.
            try { reporter.onCompactionStatus("cursor", "Queued (waiting for control format to finish)"); }
            catch (Throwable ignored) {}
        }

        SSTableFormat<?, ?> previousGlobal = DatabaseDescriptor.getSelectedSSTableFormat();
        try
        {
            // Legacy side first.
            if (legacyFormat != null)
                DatabaseDescriptor.setSelectedSSTableFormat(legacyFormat);
            else if (previousGlobal != null)
                DatabaseDescriptor.setSelectedSSTableFormat(previousGlobal);

            Collection<SSTableReader> legacyOut = new CompactionDriver(legacyCfs,
                                                                       legacyBackend,
                                                                       legacyStats,
                                                                       taskConcurrencyPerBackend,
                                                                       reporter,
                                                                       pinnedNowInSec,
                                                                       "legacy").run();
            if (reporter != null)
            {
                try { reporter.onCompactionComplete("legacy", legacyStats); }
                catch (Throwable ignored) {}
            }

            // Switch to the experiment side's format.
            if (cursorFormat != null)
                DatabaseDescriptor.setSelectedSSTableFormat(cursorFormat);
            else if (previousGlobal != null)
                DatabaseDescriptor.setSelectedSSTableFormat(previousGlobal);

            Collection<SSTableReader> cursorOut = new CompactionDriver(cursorCfs,
                                                                       cursorBackend,
                                                                       cursorStats,
                                                                       taskConcurrencyPerBackend,
                                                                       reporter,
                                                                       pinnedNowInSec,
                                                                       "cursor").run();
            if (reporter != null)
            {
                try { reporter.onCompactionComplete("cursor", cursorStats); }
                catch (Throwable ignored) {}
            }

            return new Result(legacyStats, cursorStats, legacyOut, cursorOut);
        }
        finally
        {
            DatabaseDescriptor.setSelectedSSTableFormat(previousGlobal);
        }
    }

    /**
     * Snapshots a CFS's currently-live SSTables as the boundary-friendly
     * {@code SstableInfo} list used by {@code ProgressTap.onCompactionInputs}.
     * Used by {@link #runSerial} to pre-emit both sides' input sets so the
     * not-yet-running side's TUI panel doesn't sit blank during the other
     * side's compaction.
     */
    private static List<SstableInfo>
    liveInputsOf(ColumnFamilyStore cfs)
    {
        Collection<SSTableReader> live = cfs.getLiveSSTables();
        List<SstableInfo> out = new ArrayList<>(live.size());
        for (SSTableReader r : live)
        {
            String full = r.getFilename();
            int slash = Math.max(full.lastIndexOf('/'), full.lastIndexOf('\\'));
            String basename = slash >= 0 ? full.substring(slash + 1) : full;
            int level = 0;
            try { level = r.getSSTableLevel(); }
            catch (Throwable ignored) {}
            out.add(new SstableInfo(basename, r.onDiskLength(), level));
        }
        return out;
    }

    private static Exception unwrap(ExecutionException ee)
    {
        Throwable cause = ee.getCause();
        if (cause instanceof Exception)
            return (Exception) cause;
        if (cause instanceof Error)
            throw (Error) cause;
        return ee;
    }

    /**
     * The result of a single parallel compaction.
     *
     * <p>Fields are public final to keep the data class small and free of getter
     * boilerplate.  Statistics are mutable (the underlying {@link CompactionStats}
     * volatile fields can be updated up until the run completes), but the references
     * themselves are final and the SSTable collections are read-only views by convention.
     */
    public static final class Result
    {
        /** Stats accumulated during the legacy-side run. */
        public final CompactionStats legacyStats;
        /** Stats accumulated during the cursor-side run. */
        public final CompactionStats cursorStats;
        /** Output SSTables produced by the legacy run. */
        public final Collection<SSTableReader> legacySSTables;
        /** Output SSTables produced by the cursor run. */
        public final Collection<SSTableReader> cursorSSTables;

        /**
         * @param legacyStats     stats accumulated during the legacy-side run
         * @param cursorStats     stats accumulated during the cursor-side run
         * @param legacySSTables  output SSTables from the legacy run
         * @param cursorSSTables  output SSTables from the cursor run
         */
        public Result(CompactionStats legacyStats,
                      CompactionStats cursorStats,
                      Collection<SSTableReader> legacySSTables,
                      Collection<SSTableReader> cursorSSTables)
        {
            this.legacyStats = legacyStats;
            this.cursorStats = cursorStats;
            this.legacySSTables = legacySSTables;
            this.cursorSSTables = cursorSSTables;
        }
    }

    /**
     * Simple thread factory that gives each pool a deterministic, human-readable
     * thread name prefix to aid debugging.
     */
    private static final class NamedThreadFactory implements ThreadFactory
    {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        NamedThreadFactory(String prefix)
        {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, prefix + '-' + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
