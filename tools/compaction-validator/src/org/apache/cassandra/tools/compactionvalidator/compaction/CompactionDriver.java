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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.ActiveCompactionsTracker;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.CompactionTask;
import org.apache.cassandra.db.compaction.CompactionTasks;
import org.apache.cassandra.db.compaction.DirectCompactionRunner;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.PipelineSelector;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.tools.compactionvalidator.SstableInfo;
import org.apache.cassandra.utils.FBUtilities;

/**
 * Drives one {@link ColumnFamilyStore} through compaction using a chosen pipeline backend.
 *
 * <p>The driver loops on
 * {@link CompactionStrategyManager#getNextBackgroundTasks(long)} and feeds each returned
 * {@link CompactionTask} to a {@link DirectCompactionRunner} configured with the requested
 * {@link PipelineSelector.Backend}.  Non-{@code CompactionTask} subclasses (for example
 * {@code SingleSSTableLCSTask} for LCS upgrades) are still executed via
 * {@link AbstractCompactionTask#execute(ActiveCompactionsTracker)} but bypass the
 * cursor/iterator selection because they never enter the partition-level pipeline.
 *
 * <p>After background tasks settle, an optional maximal pass is performed via
 * {@link CompactionStrategyManager#getMaximalTasks(long, boolean, int, OperationType)}
 * to ensure the table is fully compacted under the chosen backend.  This is what makes
 * the legacy- and cursor-side outputs comparable: both ultimately compact down to a
 * minimal set of SSTables.
 *
 * <p>A defensive {@value #MAX_ITERATIONS} cap on the outer loop prevents the driver
 * from spinning forever should
 * {@link CompactionStrategyManager#getNextBackgroundTasks(long)} keep returning work
 * (for instance, in pathological strategy configurations).
 *
 * <p>This class updates the supplied {@link CompactionStats} on every progress
 * callback so a UI thread can read live throughput while the run is in flight.
 */
public final class CompactionDriver
{
    private static final Logger logger = LoggerFactory.getLogger(CompactionDriver.class);

    /** Hard cap on the outer compaction loop, preventing infinite spinning. */
    public static final int MAX_ITERATIONS = 1000;

    private final ColumnFamilyStore cfs;
    private final PipelineSelector.Backend backend;
    private final CompactionStats stats;
    private final org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter;
    private final String backendLabel;
    private final int taskConcurrency;
    /**
     * Optional fixed {@code nowInSec} override propagated to every {@link DirectCompactionRunner}
     * we spawn so the iterator and cursor sides of the validator's A/B comparison see the same
     * "now" for tombstone GC and TTL purge decisions. {@code null} means each compaction reads
     * the wall clock independently — fine for stand-alone use, harmful only for parallel A/B
     * comparison runs.
     */
    private final Long fixedNowInSec;

    /**
     * @param cfs     the column family store to compact
     * @param backend the pipeline backend to use for every {@link CompactionTask}
     * @param stats   mutable statistics object updated as the run progresses
     */
    public CompactionDriver(ColumnFamilyStore cfs, PipelineSelector.Backend backend, CompactionStats stats)
    {
        this(cfs, backend, stats, 1, null);
    }

    /**
     * Variant that publishes per-progress events to the supplied {@link org.apache.cassandra.tools.compactionvalidator.ProgressTap}
     * so a TUI / plain-text reporter can show live compaction throughput. Pass {@code null} to disable reporting.
     */
    public CompactionDriver(ColumnFamilyStore cfs,
                            PipelineSelector.Backend backend,
                            CompactionStats stats,
                            org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter)
    {
        this(cfs, backend, stats, 1, reporter);
    }

    /**
     * Full constructor with a per-round task concurrency cap. UCS typically returns a batch
     * of independent tasks per call to {@code getNextBackgroundTasks} — tasks operating on
     * disjoint SSTable sets, safe to run concurrently. With {@code taskConcurrency > 1} the
     * driver submits those tasks to a fixed-size pool and waits for the round to finish
     * before draining lifecycle deletions and asking the strategy for the next round.
     */
    public CompactionDriver(ColumnFamilyStore cfs,
                            PipelineSelector.Backend backend,
                            CompactionStats stats,
                            int taskConcurrency,
                            org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter)
    {
        this(cfs, backend, stats, taskConcurrency, reporter, null);
    }

    /**
     * Variant that accepts a fixed {@code nowInSec} override propagated through to every
     * {@link DirectCompactionRunner} it spawns. Pass a non-null value when running this
     * driver alongside a sibling driver on the other side of an A/B comparison so both
     * sides see identical "now" semantics; pass {@code null} for stand-alone use.
     */
    public CompactionDriver(ColumnFamilyStore cfs,
                            PipelineSelector.Backend backend,
                            CompactionStats stats,
                            int taskConcurrency,
                            org.apache.cassandra.tools.compactionvalidator.ProgressTap reporter,
                            Long fixedNowInSec)
    {
        if (cfs == null)
            throw new IllegalArgumentException("cfs must not be null");
        if (backend == null)
            throw new IllegalArgumentException("backend must not be null");
        if (stats == null)
            throw new IllegalArgumentException("stats must not be null");
        if (taskConcurrency <= 0)
            throw new IllegalArgumentException("taskConcurrency must be positive");
        this.cfs = cfs;
        this.backend = backend;
        this.stats = stats;
        this.reporter = reporter;
        this.taskConcurrency = taskConcurrency;
        this.backendLabel = backend == PipelineSelector.Backend.CURSOR ? "cursor" : "legacy";
        this.fixedNowInSec = fixedNowInSec;
    }

    /**
     * Executes every available compaction task on this driver's CFS until the strategy
     * reports nothing more to do, then performs a maximal pass to ensure full convergence.
     *
     * <p>Updates {@link CompactionStats#bytesProcessed}, {@link CompactionStats#partitionsProcessed},
     * {@link CompactionStats#rowsProcessed}, {@link CompactionStats#sstablesIn},
     * {@link CompactionStats#sstablesOut}, and {@link CompactionStats#durationMs} on the
     * supplied stats object.
     *
     * @return all SSTables produced by this driver across every task it executed
     * @throws Exception if any individual task fails
     */
    public Collection<SSTableReader> run() throws Exception
    {
        long startNanos = System.nanoTime();

        // Capture the initial set of input SSTables so we can report sstablesIn even
        // though the tasks' transactions are not visible from this package.
        Set<SSTableReader> initialInputs = new HashSet<>(cfs.getLiveSSTables());

        // Snapshot total input bytes once so the TUI can compute a stable progress bar
        // (bytesProcessed / inputBytes). Multi-pass strategies may exceed 100% — that's fine.
        final long inputBytes = initialInputs.stream().mapToLong(SSTableReader::onDiskLength).sum();

        // Use a LinkedHashSet to preserve emission order while deduplicating in case a
        // task emits the same reader twice (shouldn't happen, but defensive).
        Set<SSTableReader> emitted = new LinkedHashSet<>();

        // Tell the reporter about the initial input set so the TUI can show the source
        // SSTables instead of "(no SSTables yet)" while the first task is still scanning.
        if (reporter != null)
        {
            try { reporter.onCompactionInputs(backendLabel, toInfoList(initialInputs)); }
            catch (Throwable ignored) {}
        }

        // Cumulative counters across all *completed* tasks. Each task's per-task
        // running totals are folded into these AtomicLongs after the task finishes;
        // the in-flight portion is held in {@link InFlightTracker} until then.
        final AtomicLong cumBytes = new AtomicLong(0L);
        final AtomicLong cumParts = new AtomicLong(0L);
        final AtomicLong cumRows  = new AtomicLong(0L);
        // Tracker shared across all concurrent tasks in this driver. Each task's
        // ProgressCallback updates its slot here; the aggregated reporter event sums
        // all in-flight slots (so the per-side TUI bar shows the sum of progress
        // across every task currently running).
        final InFlightTracker tracker = new InFlightTracker();
        // Cumulative count of tasks finished for this backend across all rounds.
        final AtomicLong totalTasksDone = new AtomicLong(0L);

        // Pool for parallel task execution. With taskConcurrency=1 this still runs
        // tasks one at a time (the pool just becomes a queue) so the sequential
        // behaviour stays identical to before — no risk of regressing the default path.
        // The pool is created once and reused across all rounds + the maximal pass to
        // keep thread-creation overhead negligible for short tasks.
        ExecutorService taskExec = Executors.newFixedThreadPool(taskConcurrency,
                                                                 new NamedThreadFactory(backendLabel + "-compaction-task"));
        try
        {
            CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();

            // Outer loop: drain background tasks until the strategy has nothing more to do.
            for (int iter = 0; iter < MAX_ITERATIONS; iter++)
            {
                // Use the fixed override here too — getNextBackgroundTasks consults nowInSec
                // for tombstone-driven re-compaction triggers; if the two sides see different
                // "now"s they may emit different task sets even on identical inputs.
                long nowInSec = fixedNowInSec != null ? fixedNowInSec : FBUtilities.nowInSeconds();
                Collection<AbstractCompactionTask> tasks = strategy.getNextBackgroundTasks(nowInSec);
                if (tasks == null || tasks.isEmpty())
                    break;

                Collection<SSTableReader> roundOutputs = runTasks(tasks, taskExec, inputBytes,
                                                                  cumBytes, cumParts, cumRows,
                                                                  tracker, totalTasksDone);
                emitted.addAll(roundOutputs);

                // Each compaction task obsoletes its input SSTables via a transaction log
                // and queues their deletion on Cassandra's tidier thread. UCS then asks for
                // the next round's tasks, which should NOT include the just-obsoleted inputs.
                // Without this barrier the deletions run async and the next strategy.getNext-
                // BackgroundTasks() call can see both the obsolete inputs AND the fresh
                // outputs — so it picks them ALL up and we end up doing redundant work +
                // the obsolete files linger on disk, blowing the output dir up to N×
                // input size after N rounds.
                LifecycleTransaction.waitForDeletions();
            }

            // Final maximal pass to ensure the table is fully compacted under the chosen backend.
            // Any SSTables produced by the loop above are visible to the strategy by now.
            long nowInSec = fixedNowInSec != null ? fixedNowInSec : FBUtilities.nowInSeconds();
            try (CompactionTasks maximal = strategy.getMaximalTasks(nowInSec, false, Integer.MAX_VALUE, OperationType.COMPACTION))
            {
                if (maximal != null && !maximal.isEmpty())
                {
                    Collection<SSTableReader> finalOutputs = runTasks(maximal, taskExec, inputBytes,
                                                                      cumBytes, cumParts, cumRows,
                                                                      tracker, totalTasksDone);
                    emitted.addAll(finalOutputs);
                }
            }
            // Drain pending deletions one last time so the on-disk state matches the CFS's
            // tracker view by the time the validator opens scanners on it.
            LifecycleTransaction.waitForDeletions();
        }
        finally
        {
            taskExec.shutdownNow();
            stats.durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            stats.sstablesIn = initialInputs.size();
            // sstablesOut needs to be the LIVE final count, not the cumulative
            // sum of every reader emitted. Multi-pass UCS produces transient SSTables
            // that get obsoleted by later passes; their references stay in `emitted`
            // but they're no longer on disk. The live tracker is the source of truth.
            try
            {
                stats.sstablesOut = cfs.getLiveSSTables().size();
            }
            catch (Throwable t)
            {
                // Fallback: counting `emitted` over-reports under multi-pass strategies
                // but is non-zero, which is enough for the TUI to render something
                // sensible if the live-tracker query fails for some reason.
                stats.sstablesOut = emitted.size();
            }
        }

        return new ArrayList<>(emitted);
    }

    /**
     * Runs a batch of tasks (potentially concurrently, up to {@link #taskConcurrency})
     * and returns the collected output SSTables.
     *
     * <p>{@link CompactionTask} instances are executed via {@link DirectCompactionRunner}
     * which honours the configured backend.  Other {@link AbstractCompactionTask} subclasses
     * (e.g. LCS single-sstable upgrades) are run via their own
     * {@link AbstractCompactionTask#execute(ActiveCompactionsTracker)} method since they
     * have no equivalent under the cursor pipeline (they don't iterate partitions).
     *
     * <p>Non-{@code CompactionTask} subclasses are run serially on the caller's thread
     * (they're upgrade-style tasks that don't benefit from parallelism and don't fire
     * the per-task pipeline events the TUI tracks).
     */
    private Collection<SSTableReader> runTasks(Collection<AbstractCompactionTask> tasks,
                                               ExecutorService taskExec,
                                               long inputBytes,
                                               AtomicLong cumBytes,
                                               AtomicLong cumParts,
                                               AtomicLong cumRows,
                                               InFlightTracker tracker,
                                               AtomicLong totalTasksDone) throws Exception
    {
        // Use a synchronized collection — multiple concurrent tasks may addAll to it.
        Set<SSTableReader> outputs = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

        // Count only tasks that actually run through DirectCompactionRunner — those are the
        // ones the TUI cares about (taskIndex/tasksInBatch). Non-CompactionTask subclasses
        // are run via task.execute() and don't fire the start/end events.
        int batchSize = 0;
        for (AbstractCompactionTask t : tasks)
            if (t instanceof CompactionTask) batchSize++;
        final int batchSizeFinal = batchSize;

        int nextTaskIndex = 0;
        List<Future<?>> futures = new ArrayList<>(batchSize);
        for (AbstractCompactionTask task : tasks)
        {
            if (task instanceof CompactionTask)
            {
                final CompactionTask ct = (CompactionTask) task;
                nextTaskIndex++;
                final int taskIndex = nextTaskIndex;

                final List<SstableInfo> taskInputs = toInfoList(DirectCompactionRunner.inputSSTablesOf(ct));
                int probedLevel = -1;
                try
                {
                    probedLevel = DirectCompactionRunner.targetLevelOf(ct);
                }
                catch (Throwable ignored)
                {
                }
                final int targetLevel = probedLevel;

                final long taskInputBytes = taskInputs.stream().mapToLong(s -> s.sizeBytes).sum();

                // Submit the task to the pool. With taskConcurrency=1 the pool serialises
                // tasks (run one at a time); with N>1 up to N tasks run concurrently.
                futures.add(taskExec.submit(() -> {
                    // Register this task with the tracker BEFORE firing TaskStart so any
                    // progress tick that races the start event still finds a slot to update.
                    tracker.start(taskIndex, taskInputBytes);

                    if (reporter != null)
                    {
                        try { reporter.onCompactionTaskStart(backendLabel, taskInputs, targetLevel,
                                                             taskIndex, batchSizeFinal,
                                                             (int) totalTasksDone.get()); }
                        catch (Throwable ignored) {}
                    }

                    Collection<SSTableReader> produced;
                    try
                    {
                        DirectCompactionRunner.ProgressCallback cb = makeCallback(taskIndex, inputBytes,
                                                                                  cumBytes, cumParts, cumRows,
                                                                                  tracker);
                        produced = new DirectCompactionRunner(backend, cb, fixedNowInSec)
                                       .run(ct, ActiveCompactionsTracker.NOOP);
                    }
                    finally
                    {
                        // Fold this task's per-task counters into the cumulative offsets and
                        // remove it from the in-flight tracker. Doing this in finally keeps the
                        // accounting consistent even if the task throws — the cum* values
                        // capture whatever bytes/parts/rows it managed before failing.
                        InFlightTracker.Counters c = tracker.end(taskIndex);
                        if (c != null)
                        {
                            cumBytes.addAndGet(c.bytes.get());
                            cumParts.addAndGet(c.parts.get());
                            cumRows.addAndGet(c.rows.get());
                        }
                        totalTasksDone.incrementAndGet();
                    }

                    if (produced != null)
                        outputs.addAll(produced);

                    if (reporter != null)
                    {
                        try { reporter.onCompactionTaskEnd(backendLabel, taskIndex,
                                                           produced != null ? toInfoList(produced) : List.of()); }
                        catch (Throwable ignored) {}
                    }
                    return null;
                }));
            }
            else
            {
                // Non-pipeline task (e.g. SingleSSTableLCSTask). No backend choice applies
                // and these don't run through the pipeline at all, so they stay on the
                // caller's thread — parallelising them would only add executor overhead.
                logger.debug("CompactionDriver: executing non-CompactionTask {} via standard path", task);
                task.execute(ActiveCompactionsTracker.NOOP);
            }
        }

        // Wait for the whole round to finish. Any task that threw will surface here as an
        // ExecutionException; we unwrap so the caller sees the underlying compaction error
        // rather than a wrapping concurrent-execution exception.
        for (Future<?> f : futures)
        {
            try
            {
                f.get();
            }
            catch (ExecutionException ee)
            {
                Throwable cause = ee.getCause();
                if (cause instanceof Exception)
                    throw (Exception) cause;
                if (cause instanceof Error)
                    throw (Error) cause;
                throw ee;
            }
        }
        return outputs;
    }

    /**
     * Builds a per-task progress callback. Each callback writes into its task's slot in
     * the shared {@link InFlightTracker} and, when reporting, emits a snapshot that's the
     * sum of every in-flight task's contribution plus the cumulative completed-task offset.
     *
     * <p>Throughput (bps) is tracked per-callback because each task has its own perception
     * of "elapsed time since last report"; aggregating bps across tasks is meaningful only
     * at the aggregate level, so we send the aggregated snapshot but compute bps based on
     * this callback's own delta since its last tick.
     */
    private DirectCompactionRunner.ProgressCallback makeCallback(int taskIndex,
                                                                 long inputBytes,
                                                                 AtomicLong cumBytes,
                                                                 AtomicLong cumParts,
                                                                 AtomicLong cumRows,
                                                                 InFlightTracker tracker)
    {
        return new DirectCompactionRunner.ProgressCallback()
        {
            private long lastReportedBytes = 0L;
            private long lastReportNanos = 0L;

            @Override
            public void onProgress(long bytesScanned,
                                   long estimatedTotalBytes,
                                   long partitionsWritten,
                                   long rowsWritten)
            {
                tracker.update(taskIndex, bytesScanned, partitionsWritten, rowsWritten);

                long inFlightBytes = tracker.sumBytes();
                long inFlightParts = tracker.sumParts();
                long inFlightRows  = tracker.sumRows();

                long totalBytes = cumBytes.get() + inFlightBytes;
                long totalParts = cumParts.get() + inFlightParts;
                long totalRows  = cumRows.get()  + inFlightRows;
                stats.bytesProcessed = totalBytes;
                stats.partitionsProcessed = totalParts;
                stats.rowsProcessed = totalRows;

                if (reporter != null)
                {
                    long now = System.nanoTime();
                    long elapsedMillis = (now - lastReportNanos) / 1_000_000L;
                    double bps = 0.0;
                    if (lastReportNanos != 0L && elapsedMillis > 0L)
                        bps = (double) (totalBytes - lastReportedBytes) * 1000.0 / elapsedMillis;
                    lastReportedBytes = totalBytes;
                    lastReportNanos = now;
                    try
                    {
                        // Per-side bar shows the sum of in-flight progress / sum of in-flight inputs.
                        // With taskConcurrency=1 this is identical to the old single-task behaviour;
                        // with N>1 it correctly aggregates so the bar reflects "how much work is
                        // currently underway" rather than just one task's slice.
                        reporter.onCompactionProgress(backendLabel, totalBytes, inputBytes, totalParts, bps,
                                                      inFlightBytes, tracker.sumInputTotal());
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }

            @Override
            public void onSStableEmitted(String filename, long sizeBytes)
            {
                if (reporter != null)
                {
                    try
                    {
                        reporter.onSstableEmitted(backendLabel, filename, sizeBytes, /* level = */ 0);
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }
        };
    }

    /**
     * Shared per-driver tracker for in-flight task progress. Concurrent tasks register
     * themselves on start, update their slot from their ProgressCallback ticks, and
     * deregister on completion. Aggregate sums are O(active-task-count) which stays
     * small (bounded by {@code taskConcurrency}).
     */
    private static final class InFlightTracker
    {
        static final class Counters
        {
            final AtomicLong bytes = new AtomicLong(0L);
            final AtomicLong parts = new AtomicLong(0L);
            final AtomicLong rows  = new AtomicLong(0L);
            final long inputTotal;
            Counters(long inputTotal) { this.inputTotal = inputTotal; }
        }

        private final ConcurrentHashMap<Integer, Counters> active = new ConcurrentHashMap<>();

        void start(int taskIndex, long inputTotal)
        {
            active.put(taskIndex, new Counters(inputTotal));
        }

        /** Returns the just-removed slot so the caller can fold its contribution into a cumulative counter. */
        Counters end(int taskIndex)
        {
            return active.remove(taskIndex);
        }

        void update(int taskIndex, long bytes, long parts, long rows)
        {
            Counters c = active.get(taskIndex);
            if (c == null)
                return; // task ended already — ignore late tick
            c.bytes.set(bytes);
            c.parts.set(parts);
            c.rows.set(rows);
        }

        long sumBytes()
        {
            long sum = 0L;
            for (Counters c : active.values()) sum += c.bytes.get();
            return sum;
        }

        long sumParts()
        {
            long sum = 0L;
            for (Counters c : active.values()) sum += c.parts.get();
            return sum;
        }

        long sumRows()
        {
            long sum = 0L;
            for (Counters c : active.values()) sum += c.rows.get();
            return sum;
        }

        long sumInputTotal()
        {
            long sum = 0L;
            for (Counters c : active.values()) sum += c.inputTotal;
            return sum;
        }
    }

    /** Thread factory for the per-driver compaction task pool — daemon threads with a readable name. */
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

    /** Converts a Cassandra SSTableReader collection to the boundary-friendly SstableInfo list. */
    private static List<SstableInfo> toInfoList(Collection<SSTableReader> readers)
    {
        List<SstableInfo> out = new ArrayList<>(readers.size());
        for (SSTableReader r : readers)
            out.add(toInfo(r));
        return out;
    }

    private static SstableInfo toInfo(SSTableReader r)
    {
        String full = r.getFilename();
        int slash = Math.max(full.lastIndexOf('/'), full.lastIndexOf('\\'));
        String basename = slash >= 0 ? full.substring(slash + 1) : full;
        int level = 0;
        try { level = r.getSSTableLevel(); }
        catch (Throwable ignored) {}
        return new SstableInfo(basename, r.onDiskLength(), level);
    }
}
