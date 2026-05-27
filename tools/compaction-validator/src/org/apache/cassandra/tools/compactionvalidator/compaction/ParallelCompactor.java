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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.PipelineSelector;
import org.apache.cassandra.io.sstable.format.SSTableReader;
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
    private final int taskConcurrencyPerBackend;
    private final org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter;

    /**
     * @param legacyCfs CFS configured to read from the legacy/output-legacy directory
     * @param cursorCfs CFS configured to read from the cursor/output-cursor directory
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs, ColumnFamilyStore cursorCfs)
    {
        this(legacyCfs, cursorCfs, PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR, 1, null);
    }

    /**
     * Variant that publishes per-progress events to the supplied reporter so a TUI can
     * display live throughput. Pass {@code null} to disable reporting. Pipeline backends
     * default to ITERATOR for the legacy slot and CURSOR for the cursor slot.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter)
    {
        this(legacyCfs, cursorCfs, PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR, 1, reporter);
    }

    /**
     * Variant with task concurrency. Pipeline backends default to ITERATOR for the
     * legacy slot and CURSOR for the cursor slot.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             int taskConcurrencyPerBackend,
                             org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter)
    {
        this(legacyCfs, cursorCfs,
             PipelineSelector.Backend.ITERATOR, PipelineSelector.Backend.CURSOR,
             taskConcurrencyPerBackend, reporter);
    }

    /**
     * Full constructor. Each side names its own pipeline backend so a comparison can
     * exercise iterator-vs-cursor (the original use case), iterator-vs-iterator (testing
     * compaction strategies on the same pipeline), or cursor-vs-cursor (testing compression
     * codecs on the cursor pipeline).
     *
     * <p>Both sides share the same {@code taskConcurrencyPerBackend} value so that the
     * cursor-vs-legacy speedup measurement isn't perturbed by asymmetric parallelism.
     */
    public ParallelCompactor(ColumnFamilyStore legacyCfs,
                             ColumnFamilyStore cursorCfs,
                             PipelineSelector.Backend legacyBackend,
                             PipelineSelector.Backend cursorBackend,
                             int taskConcurrencyPerBackend,
                             org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter)
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
                                                                   pinnedNowInSec).run();
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
                                                                   pinnedNowInSec).run();
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
        }
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
