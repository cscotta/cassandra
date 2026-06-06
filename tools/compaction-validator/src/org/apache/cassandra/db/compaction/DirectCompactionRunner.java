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
package org.apache.cassandra.db.compaction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.util.concurrent.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.TimeUUID;
import org.apache.cassandra.utils.concurrent.Refs;

/**
 * Replicates the core compaction execution loop from {@link CompactionTask#runMayThrow()} but accepts an explicit
 * {@link PipelineSelector.Backend} so the caller can force either the cursor-based or iterator-based pipeline
 * without touching the global {@code DatabaseDescriptor.cursorCompactionEnabled()} flag.
 *
 * <p>This class lives in {@code org.apache.cassandra.db.compaction} so that it can access the
 * {@code protected} fields of {@link AbstractCompactionTask} and {@link CompactionTask} and the
 * package-private pipeline constructors exposed via {@link PipelineSelector}.
 */
public final class DirectCompactionRunner
{
    private static final Logger logger = LoggerFactory.getLogger(DirectCompactionRunner.class);

    private static final long PROGRESS_INTERVAL_BYTES = 1_048_576L; // 1 MiB

    /**
     * Callback interface invoked during a compaction run to report progress and SSTable emissions.
     */
    public interface ProgressCallback
    {
        /**
         * Called approximately every {@value DirectCompactionRunner#PROGRESS_INTERVAL_BYTES} bytes of
         * uncompressed data scanned.
         *
         * @param bytesScanned        total uncompressed bytes scanned so far
         * @param estimatedTotalBytes estimated total uncompressed bytes to scan (from pipeline)
         * @param partitionsWritten   total partition keys written so far
         * @param rowsWritten         total source CQL rows processed so far
         */
        void onProgress(long bytesScanned, long estimatedTotalBytes, long partitionsWritten, long rowsWritten);

        /**
         * Called once each time a new SSTable file is finalized and added to the output set.
         *
         * @param filename  the absolute path of the new SSTable data file
         * @param sizeBytes the on-disk size of the new SSTable in bytes
         */
        void onSStableEmitted(String filename, long sizeBytes);
    }

    private final PipelineSelector.Backend backend;
    private final ProgressCallback callback;
    private final Long fixedNowInSecOverride;

    /**
     * Creates a runner that will use the given backend and invoke {@code callback} during execution.
     *
     * <p>Each compaction will compute its own {@code nowInSec} from {@link FBUtilities#nowInSeconds()}.
     * Use {@link #DirectCompactionRunner(PipelineSelector.Backend, ProgressCallback, Long)} to
     * pin a fixed {@code nowInSec} across multiple parallel compactions — important for the
     * compaction-validator's A/B comparison where the two pipelines must make identical
     * tombstone-GC and TTL-purge decisions.
     *
     * @param backend  which pipeline implementation to use
     * @param callback receives progress and emission events
     */
    public DirectCompactionRunner(PipelineSelector.Backend backend, ProgressCallback callback)
    {
        this(backend, callback, null);
    }

    /**
     * Creates a runner that uses {@code fixedNowInSec} for all in-pipeline GC / TTL
     * decisions, instead of reading the wall clock at compaction-start time.
     *
     * <p>Pinning the value across two parallel runs (the iterator side and the cursor
     * side of the validator's A/B comparison) guarantees that both pipelines see the
     * same input data {@em and} make the same decision about which expired cells and
     * tombstones to drop. Without this, a 1-2-second skew between when the two threads
     * enter their pipeline loop could let one side drop a near-expiry cell that the
     * other still considers live, surfacing as a spurious post-compaction divergence
     * even though both pipelines are correct.
     *
     * @param backend           which pipeline implementation to use
     * @param callback          receives progress and emission events
     * @param fixedNowInSec     forced "now" value in seconds since epoch, or
     *                          {@code null} to fall back to {@link FBUtilities#nowInSeconds()}
     */
    public DirectCompactionRunner(PipelineSelector.Backend backend,
                                  ProgressCallback callback,
                                  Long fixedNowInSec)
    {
        this.backend = backend;
        this.callback = callback;
        this.fixedNowInSecOverride = fixedNowInSec;
    }

    /**
     * Runs the compaction described by {@code task} under the given {@code tracker}, using the backend
     * supplied at construction time.
     *
     * <p>The method follows the same contract as {@link CompactionTask#runMayThrow()}:
     * <ul>
     *   <li>Fully-expired SSTables are removed from the active set via
     *       {@code task.transaction.obsolete()}.</li>
     *   <li>The rate limiter from {@link CompactionManager#getRateLimiter()} is honoured.</li>
     *   <li>{@code tracker.beginCompaction}/{@code finishCompaction} bracket the pipeline loop.</li>
     * </ul>
     *
     * @param task    the compaction task to execute; must have a live transaction
     * @param tracker the active-compactions tracker to register this run with
     * @return the collection of newly written SSTables, or an empty collection if there was nothing
     *         to compact (all inputs were empty or fully expired)
     * @throws Exception if the pipeline or any I/O operation fails
     */
    public Collection<SSTableReader> run(CompactionTask task, ActiveCompactionsTracker tracker) throws Exception
    {
        // Guard: nothing to compact
        if (task.inputSSTables().isEmpty())
            return Collections.emptyList();

        try (CompactionController controller = task.getCompactionController(task.inputSSTables()))
        {
            Set<SSTableReader> actuallyCompact = new HashSet<>(task.inputSSTables());

            // Remove fully-expired SSTables from the active compaction set, marking them obsolete
            // so the lifecycle transaction knows they can be dropped without rewriting.
            Set<SSTableReader> fullyExpiredSSTables = controller.getFullyExpiredSSTables();
            if (!fullyExpiredSSTables.isEmpty())
            {
                logger.debug("DirectCompactionRunner dropping fully-expired SSTables: {}", fullyExpiredSSTables);
                fullyExpiredSSTables.forEach(task.transaction::obsolete);
                actuallyCompact.removeAll(fullyExpiredSSTables);
            }

            if (actuallyCompact.isEmpty())
                return Collections.emptyList();

            // Refresh overlaps if we changed the set (mirrors CompactionTask behaviour).
            if (!fullyExpiredSSTables.isEmpty())
                controller.refreshOverlaps();

            TimeUUID taskId = task.transaction.opId();
            // Honour the fixed-nowInSec override when set; this is what keeps the iterator
            // and cursor sides of the validator's A/B comparison making identical GC / TTL
            // purge decisions even when their pipeline loops start a second or two apart.
            long nowInSec = fixedNowInSecOverride != null ? fixedNowInSecOverride : FBUtilities.nowInSeconds();

            RateLimiter limiter = CompactionManager.instance.getRateLimiter();

            try (Refs<SSTableReader> refs = Refs.ref(actuallyCompact);
                 AbstractCompactionStrategy.ScannerList scanners = task.cfs.getCompactionStrategyManager()
                                                                           .getScanners(actuallyCompact, null);
                 AbstractCompactionPipeline pipeline = PipelineSelector.create(
                         backend, task, task.compactionType, scanners, controller, nowInSec, taskId))
            {
                double compressionRatio = scanners.getCompressionRatio();
                if (compressionRatio == MetadataCollector.NO_COMPRESSION_RATIO)
                    compressionRatio = 1.0;

                long estimatedTotalBytes = scanners.getTotalCompressedSize();

                tracker.beginCompaction(pipeline);
                try (AutoCloseable writerResource = task.getCompactionAwareWriter(actuallyCompact, pipeline))
                {
                    long lastBytesScanned = 0;
                    long estimatedKeys = pipeline.estimatedKeys();

                    while (pipeline.processNextPartitionKey())
                    {
                        long bytesScanned = pipeline.getTotalBytesScanned();

                        if (bytesScanned - lastBytesScanned > PROGRESS_INTERVAL_BYTES)
                        {
                            // Honour the global compaction rate limiter before reporting progress.
                            CompactionManager.instance.compactionRateLimiterAcquire(
                                    limiter, bytesScanned, lastBytesScanned, compressionRatio);

                            callback.onProgress(bytesScanned,
                                                estimatedTotalBytes,
                                                pipeline.getTotalKeysWritten(),
                                                pipeline.getTotalSourceCQLRows());

                            lastBytesScanned = bytesScanned;
                        }
                    }

                    // Drain any leftover bytes through the rate limiter.
                    long finalBytesScanned = pipeline.getTotalBytesScanned();
                    if (finalBytesScanned != lastBytesScanned)
                    {
                        CompactionManager.instance.compactionRateLimiterAcquire(
                                limiter, finalBytesScanned, lastBytesScanned, compressionRatio);
                    }

                    // Finalise: write SSTable metadata, update the lifecycle transaction, etc.
                    Collection<SSTableReader> newSSTables = task.finish(pipeline);

                    for (SSTableReader sstable : newSSTables)
                        callback.onSStableEmitted(sstable.getFilename(), sstable.onDiskLength());

                    return newSSTables;
                }
                finally
                {
                    tracker.finishCompaction(pipeline);
                }
            }
        }
    }

    /**
     * Same-package accessor for {@link CompactionTask#inputSSTables()}, which is {@code protected}.
     * Lets tool code in other packages observe the input set of a task without subclassing.
     */
    public static Set<SSTableReader> inputSSTablesOf(CompactionTask task)
    {
        return task.inputSSTables();
    }

    /**
     * Same-package accessor for {@link CompactionTask#getLevel()}, which is {@code protected}.
     * Returns the task's target output level, or whatever the strategy assigns; meaning is
     * strategy-specific (UCS doesn't really use levels; LCS does).
     */
    public static int targetLevelOf(CompactionTask task)
    {
        return task.getLevel();
    }
}
