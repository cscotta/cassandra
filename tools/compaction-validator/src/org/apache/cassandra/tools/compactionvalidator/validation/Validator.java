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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.SSTableReadsListener;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.big.BigTableReader;
import org.apache.cassandra.io.sstable.format.bti.BtiTableReader;

/**
 * Two-phase validator that compares the SSTables produced by the legacy and cursor
 * compaction backends and reports the first observable divergence (if any).
 *
 * <h2>Phase A: hash sweep (parallel)</h2>
 *
 * <p>The murmur3 token ring is split into {@link #validationThreads} equal-width
 * ranges and each worker scans both sides' SSTables restricted to its range. Each
 * pair of partitions is hashed via {@link PartitionHasher} and compared. If any
 * worker finds a mismatch it returns the offending key.
 *
 * <p>When multiple workers report mismatches, the one with the smallest token wins
 * — that's the deterministic "first divergence" in scan order. Re-runs with the
 * same seed therefore produce the same {@link MismatchReport} regardless of how
 * shards were scheduled.
 *
 * <h2>Phase B: deep comparison on mismatch</h2>
 *
 * <p>When Phase A finds a mismatch, both sides are re-scanned with their scanners
 * restricted to the offending partition's bounds.  A {@link PartitionComparator}
 * walks both iterators in lockstep and produces a detailed {@link MismatchReport}.
 *
 * <p>Phase B uses {@link Bounds} over {@link PartitionPosition} so the scanner only
 * loads data for that single partition; this keeps the deep-comparison phase fast
 * even for very large compaction outputs. It runs single-threaded — only one
 * partition needs to be re-scanned, so no parallelism opportunity.
 *
 * <p>If Phase A completes without any mismatch, this validator returns
 * {@link Optional#empty()}.  Any reported {@link MismatchReport} is guaranteed to
 * describe a real, observable divergence (Phase B re-verifies and returns a
 * descriptive report even on the unlikely case of a hash collision).
 */
public final class Validator
{
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    private final ColumnFamilyStore legacyCfs;
    private final ColumnFamilyStore cursorCfs;
    private final Collection<SSTableReader> legacySSTables;
    private final Collection<SSTableReader> cursorSSTables;
    private final ValidationStats stats;
    private final int validationThreads;
    private final ErrataChecker errataChecker;

    /**
     * @param legacyCfs       CFS associated with the legacy output
     * @param cursorCfs       CFS associated with the cursor output
     * @param legacySSTables  SSTables produced by the legacy compaction
     * @param cursorSSTables  SSTables produced by the cursor compaction
     * @param stats           mutable stats updated during validation
     */
    public Validator(ColumnFamilyStore legacyCfs,
                     ColumnFamilyStore cursorCfs,
                     Collection<SSTableReader> legacySSTables,
                     Collection<SSTableReader> cursorSSTables,
                     ValidationStats stats)
    {
        this(legacyCfs, cursorCfs, legacySSTables, cursorSSTables, stats, 1,
             java.util.Collections.emptySet());
    }

    /**
     * Variant that runs Phase A in parallel across {@code validationThreads}
     * equal-width token-range shards. Phase B always runs single-threaded against
     * the canonical mismatch partition. No errata are active.
     */
    public Validator(ColumnFamilyStore legacyCfs,
                     ColumnFamilyStore cursorCfs,
                     Collection<SSTableReader> legacySSTables,
                     Collection<SSTableReader> cursorSSTables,
                     ValidationStats stats,
                     int validationThreads)
    {
        this(legacyCfs, cursorCfs, legacySSTables, cursorSSTables, stats, validationThreads,
             java.util.Collections.emptySet());
    }

    /**
     * Full constructor: also takes the set of active {@link ErrataRule}s for the
     * {@code --ignore-errata} feature. When non-empty, a hash mismatch in Phase A
     * triggers a per-cell re-scan (bounds-restricted to the offending partition)
     * and {@link ErrataChecker} decides whether to suppress + record or surface
     * the divergence as a real {@link MismatchReport}.
     */
    public Validator(ColumnFamilyStore legacyCfs,
                     ColumnFamilyStore cursorCfs,
                     Collection<SSTableReader> legacySSTables,
                     Collection<SSTableReader> cursorSSTables,
                     ValidationStats stats,
                     int validationThreads,
                     java.util.Set<ErrataRule> activeErrata)
    {
        if (legacyCfs == null)
            throw new IllegalArgumentException("legacyCfs must not be null");
        if (cursorCfs == null)
            throw new IllegalArgumentException("cursorCfs must not be null");
        if (legacySSTables == null)
            throw new IllegalArgumentException("legacySSTables must not be null");
        if (cursorSSTables == null)
            throw new IllegalArgumentException("cursorSSTables must not be null");
        if (stats == null)
            throw new IllegalArgumentException("stats must not be null");
        if (validationThreads <= 0)
            throw new IllegalArgumentException("validationThreads must be positive");

        this.legacyCfs = legacyCfs;
        this.cursorCfs = cursorCfs;
        this.legacySSTables = legacySSTables;
        this.cursorSSTables = cursorSSTables;
        this.stats = stats;
        this.validationThreads = validationThreads;
        this.errataChecker = new ErrataChecker(activeErrata);
    }

    /**
     * Runs the validator end to end.
     *
     * <p>Phase A is always executed.  Phase B is executed only when Phase A finds a
     * mismatch.  The validator does not modify either side's SSTables.
     *
     * @return the {@link MismatchReport} for the first observed divergence, or
     *         {@link Optional#empty()} if no divergence was found
     * @throws Exception if scanner construction or iteration fails
     */
    public Optional<MismatchReport> validate() throws Exception
    {
        long startNanos = System.nanoTime();
        try
        {
            DecoratedKey mismatchKey = phaseAFindMismatch();

            // If forward-scan Phase A passed, run the depth-checks pass next.
            // This is a SINGLE combined walk that does both per-partition reverse
            // iteration (catches bug 2's broken {@code prevUnfilteredSize}) and
            // end-of-partition indexed point reads (catches bug 3's Index.db
            // final-block off-by-one). Sharded across the same {@link
            // #validationThreads} workers as Phase A so it saturates cores.
            if (mismatchKey == null)
                mismatchKey = phaseDepthChecksFindMismatch();

            if (mismatchKey == null)
                return Optional.empty();

            stats.partitionMismatches.incrementAndGet();
            MismatchReport report = phaseBDeepCompare(mismatchKey);
            return Optional.ofNullable(report);
        }
        finally
        {
            stats.durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        }
    }

    // ---- Phase A --------------------------------------------------------

    /**
     * Splits the ring into {@link #validationThreads} workers and returns the canonical
     * (smallest-token) mismatch across all of them, or {@code null} if every shard
     * finished without finding one.
     */
    private DecoratedKey phaseAFindMismatch() throws Exception
    {
        if (validationThreads == 1)
            return phaseAOneShard(null);

        IPartitioner partitioner = legacyCfs.getPartitioner();
        if (!(partitioner instanceof Murmur3Partitioner))
        {
            // Token-range sharding requires a numeric token space we can divide
            // evenly; Murmur3 is what the validator's bootstrap installs. Fall back
            // to single-threaded validation if some future code path switches
            // partitioners — better to validate slowly than to skip shards.
            logger.warn("Partitioner {} is not Murmur3 — Phase A falling back to single-threaded validation",
                        partitioner.getClass().getSimpleName());
            return phaseAOneShard(null);
        }

        List<AbstractBounds<PartitionPosition>> shards = splitMurmur3Ring(validationThreads);

        ExecutorService pool = Executors.newFixedThreadPool(validationThreads,
                                                            new NamedThreadFactory("compaction-validator-validate"));
        List<Future<DecoratedKey>> futures = new ArrayList<>(shards.size());
        try
        {
            for (AbstractBounds<PartitionPosition> bounds : shards)
            {
                final AbstractBounds<PartitionPosition> b = bounds;
                Callable<DecoratedKey> work = () -> phaseAOneShard(b);
                futures.add(pool.submit(work));
            }

            // Gather every shard's result rather than short-circuiting on the first.
            // Two reasons:
            //   1) Workers must finish so their AtomicLong increments are reflected in
            //      ValidationStats — short-circuiting would leave partitionsChecked
            //      undercounted.
            //   2) The canonical mismatch is the smallest-token result across all
            //      shards. We need to look at all results to pick it deterministically.
            DecoratedKey best = null;
            Exception failure = null;
            for (Future<DecoratedKey> f : futures)
            {
                try
                {
                    DecoratedKey k = f.get();
                    if (k != null && (best == null || k.compareTo(best) < 0))
                        best = k;
                }
                catch (ExecutionException ee)
                {
                    Throwable cause = ee.getCause();
                    Exception ex = (cause instanceof Exception) ? (Exception) cause : ee;
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null)
                throw failure;
            return best;
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    /**
     * Walks one token-range shard of both sides in lockstep and returns the partition
     * key of the first mismatched pair, or {@code null} if all partitions in this
     * range matched. A {@code null} {@code bounds} means "the whole ring" — used for
     * the single-thread fallback path.
     */
    private DecoratedKey phaseAOneShard(AbstractBounds<PartitionPosition> bounds) throws Exception
    {
        List<ISSTableScanner> legacyScanners = openScanners(legacySSTables, bounds);
        List<ISSTableScanner> cursorScanners = openScanners(cursorSSTables, bounds);

        try (UnfilteredPartitionIterator legacyIter = mergeScanners(legacyScanners);
             UnfilteredPartitionIterator cursorIter = mergeScanners(cursorScanners))
        {
            while (true)
            {
                boolean lHas = legacyIter.hasNext();
                boolean cHas = cursorIter.hasNext();
                if (!lHas && !cHas)
                    return null;

                if (lHas != cHas)
                {
                    // One side ended first; the next partition on the still-open side
                    // is the divergence point.  Return that key so Phase B re-scans both
                    // restricted to it; PartitionComparator will produce the detailed
                    // length-mismatch report.
                    UnfilteredRowIterator extra = lHas ? legacyIter.next() : cursorIter.next();
                    DecoratedKey extraKey;
                    try
                    {
                        extraKey = extra.partitionKey();
                    }
                    finally
                    {
                        extra.close();
                    }
                    return extraKey;
                }

                try (UnfilteredRowIterator legacyPart = legacyIter.next();
                     UnfilteredRowIterator cursorPart = cursorIter.next())
                {
                    // Sanity: the partition keys at this position should match — the
                    // merge iterators are sorted on the same partitioner.  If they don't,
                    // we have a divergence already; surface the legacy key.
                    if (!legacyPart.partitionKey().equals(cursorPart.partitionKey()))
                    {
                        return legacyPart.partitionKey();
                    }

                    long legacyHash;
                    long cursorHash;
                    long rowCount;
                    {
                        PartitionHasher.HashAndRowCount lhc = PartitionHasher.hashPartitionWithRowCount(legacyPart);
                        PartitionHasher.HashAndRowCount chc = PartitionHasher.hashPartitionWithRowCount(cursorPart);
                        legacyHash = lhc.hash;
                        cursorHash = chc.hash;
                        // Use legacy as the authoritative row count (matches "legacy is canonical" semantics).
                        rowCount = lhc.rowCount;
                    }

                    if (legacyHash != cursorHash)
                    {
                        DecoratedKey key = legacyPart.partitionKey();

                        // If --ignore-errata was passed, re-scan this single partition
                        // (cheap, bounded scanners) and consult ErrataChecker. If every
                        // difference fits an active rule, increment the per-rule counter
                        // and continue Phase A; otherwise return the key so the outer
                        // validate() runs Phase B's full report and halts the run.
                        if (errataChecker.isActive())
                        {
                            java.util.Set<ErrataRule> fired = checkErrataForPartition(key);
                            if (!fired.isEmpty())
                            {
                                for (ErrataRule rule : fired)
                                    stats.recordErrata(rule);
                                // Count the partition as "checked" — we did the work
                                // to verify its differences are known. Without this,
                                // the visible partitionsChecked would silently miss
                                // every errata partition.
                                stats.partitionsChecked.incrementAndGet();
                                stats.rowsChecked.addAndGet(rowCount);
                                continue;
                            }
                        }
                        return key;
                    }

                    stats.partitionsChecked.incrementAndGet();
                    stats.rowsChecked.addAndGet(rowCount);
                }
            }
        }
    }

    /**
     * Re-opens both sides' scanners restricted to a single partition and asks
     * {@link ErrataChecker} whether every observable difference is explainable
     * by one of the active rules.
     *
     * @return the rules that fired (empty if it's a real mismatch)
     */
    private java.util.Set<ErrataRule> checkErrataForPartition(DecoratedKey key) throws Exception
    {
        AbstractBounds<PartitionPosition> bounds = new Bounds<>(key, key);
        List<ISSTableScanner> legacyScanners = openScanners(legacySSTables, bounds);
        List<ISSTableScanner> cursorScanners = openScanners(cursorSSTables, bounds);
        try (UnfilteredPartitionIterator legacyIter = mergeScanners(legacyScanners);
             UnfilteredPartitionIterator cursorIter = mergeScanners(cursorScanners))
        {
            UnfilteredRowIterator legacyPart = findPartition(legacyIter, key);
            UnfilteredRowIterator cursorPart = findPartition(cursorIter, key);
            try
            {
                // If either side is missing the partition the check can't suppress —
                // a present-vs-absent divergence is a real mismatch every time.
                if (legacyPart == null || cursorPart == null)
                    return java.util.Collections.emptySet();
                return errataChecker.matches(legacyPart, cursorPart);
            }
            finally
            {
                if (legacyPart != null) legacyPart.close();
                if (cursorPart != null) cursorPart.close();
            }
        }
    }

    // ---- Phase Depth-Checks (reverse-scan + indexed point-read) ----------

    /**
     * Reverse-scan sampling stride. The pass opens one reverse iterator per
     * sampled partition on every SSTable on each side; for soak runs that
     * emit millions of partitions this becomes prohibitive (each open is a
     * B-tree lookup + ~100 µs of partition-read setup, multiplied by per-side
     * SSTable count). Compaction bug 2 affects {@em every} reverse iteration
     * uniformly, so checking 1% of partitions per run still catches it
     * essentially every time across a soak loop. Sampling determinism is
     * preserved by indexing the loop counter — same seed → same sampled
     * partitions on re-run within a given shard count.
     */
    private static final int REVERSE_SCAN_STRIDE = 100;

    /**
     * Indexed point-read sampling stride. Same motivation as the reverse-scan
     * stride: per-partition iterator opens are expensive at scale. Bug 3 only
     * manifests on partitions ≥ 64 KiB, so the sampling probability of
     * catching it scales with how many large partitions the run produces;
     * with the data generator's 5% wide-row + 1% large-cell knobs that's
     * still hundreds of large partitions per 1 GiB run, easily enough to
     * hit at the 1% sampling rate.
     */
    private static final int POINT_READ_STRIDE = 100;

    /**
     * Runs the combined depth-checks pass: a single forward walk that does
     * both per-partition reverse iteration and end-of-partition indexed
     * point reads. Sharded across {@link #validationThreads} workers using
     * the same token-range splitter Phase A uses.
     *
     * <p>Combining the two checks into one walk saves the second forward
     * pass and gives both checks the same warm scanner state per partition.
     * Sharding lets the pass saturate cores instead of running serially on
     * the orchestrator's thread — important because each per-partition
     * iterator open is a small synchronous I/O that doesn't otherwise yield.
     *
     * <p>Returns the partition key of the first divergence (by smallest
     * token, deterministic across shards), or {@code null} if no sampled
     * partition diverged on either check.
     */
    private DecoratedKey phaseDepthChecksFindMismatch() throws Exception
    {
        if (validationThreads == 1)
            return phaseDepthChecksOneShard(null);

        IPartitioner partitioner = legacyCfs.getPartitioner();
        if (!(partitioner instanceof Murmur3Partitioner))
        {
            // Token-range sharding requires a numeric token space we can divide
            // evenly; same constraint as Phase A. Fall back to single-threaded
            // walk so non-Murmur3 setups still get coverage, just serially.
            logger.warn("Partitioner {} is not Murmur3 — depth-checks falling back to single-threaded",
                        partitioner.getClass().getSimpleName());
            return phaseDepthChecksOneShard(null);
        }

        List<AbstractBounds<PartitionPosition>> shards = splitMurmur3Ring(validationThreads);
        ExecutorService pool = Executors.newFixedThreadPool(validationThreads,
                                                            new NamedThreadFactory("compaction-validator-depth"));
        List<Future<DecoratedKey>> futures = new ArrayList<>(shards.size());
        try
        {
            for (AbstractBounds<PartitionPosition> bounds : shards)
            {
                final AbstractBounds<PartitionPosition> b = bounds;
                futures.add(pool.submit(() -> phaseDepthChecksOneShard(b)));
            }

            // Collect every shard's result (don't short-circuit) so we pick the
            // smallest-token mismatch deterministically — same convention as
            // Phase A. Any failures across shards are merged via addSuppressed.
            DecoratedKey best = null;
            Exception failure = null;
            for (Future<DecoratedKey> f : futures)
            {
                try
                {
                    DecoratedKey k = f.get();
                    if (k != null && (best == null || k.compareTo(best) < 0))
                        best = k;
                }
                catch (ExecutionException ee)
                {
                    Throwable cause = ee.getCause();
                    Exception ex = (cause instanceof Exception) ? (Exception) cause : ee;
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null)
                throw failure;
            return best;
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    /**
     * Walks one shard's worth of partitions doing reverse-iter hash + indexed
     * point-read sampling. {@code bounds == null} means "the whole ring" —
     * used by the single-thread fallback path.
     *
     * <p>The forward walk enumerates partition keys (and, for indexed reads,
     * captures the partition's last clustering). Per partition the loop
     * decides — based on the shard-local iteration counter — whether to
     * spend the per-partition reverse-iter cost and/or the per-partition
     * point-read cost. Sampling makes per-iteration work bounded; sharding
     * makes the whole pass parallel.
     */
    private DecoratedKey phaseDepthChecksOneShard(AbstractBounds<PartitionPosition> bounds) throws Exception
    {
        ColumnFilter columnFilter = ColumnFilter.all(legacyCfs.metadata());
        SSTableReadsListener listener = new SSTableReadsListener() {};
        boolean hasClusteringKeys = !legacyCfs.metadata().clusteringColumns().isEmpty();

        long iterationIndex = 0;
        List<ISSTableScanner> ls = openScanners(legacySSTables, bounds);
        List<ISSTableScanner> cs = openScanners(cursorSSTables, bounds);
        try (UnfilteredPartitionIterator legacyIter = mergeScanners(ls);
             UnfilteredPartitionIterator cursorIter = mergeScanners(cs))
        {
            while (legacyIter.hasNext() && cursorIter.hasNext())
            {
                DecoratedKey key;
                Clustering<?> lastClustering;
                try (UnfilteredRowIterator lp = legacyIter.next();
                     UnfilteredRowIterator cp = cursorIter.next())
                {
                    if (!lp.partitionKey().equals(cp.partitionKey()))
                    {
                        // Forward Phase A would already have caught this — defensive
                        // bail so the depth-checks pass doesn't do extra work on a
                        // known mismatch.
                        return lp.partitionKey();
                    }
                    key = lp.partitionKey();
                    // Walk the legacy side forward to find the last clustering
                    // (used by the point-read check). Drain cursor side too so
                    // both merge iterators advance past this partition before the
                    // next loop iteration.
                    Clustering<?> tail = null;
                    while (lp.hasNext())
                    {
                        Unfiltered u = lp.next();
                        if (u.isRow())
                            tail = ((Row) u).clustering();
                    }
                    lastClustering = tail;
                    while (cp.hasNext()) cp.next();
                }

                boolean checkReverse   = (iterationIndex % REVERSE_SCAN_STRIDE) == 0;
                boolean checkPointRead = hasClusteringKeys
                                         && lastClustering != null
                                         && (iterationIndex % POINT_READ_STRIDE) == 0;
                iterationIndex++;

                if (checkReverse)
                {
                    long legacyHash = reverseHashPartition(key, legacySSTables, columnFilter, listener);
                    long cursorHash = reverseHashPartition(key, cursorSSTables, columnFilter, listener);
                    if (legacyHash != cursorHash)
                        return key;
                }

                if (checkPointRead)
                {
                    Slices slices = Slices.with(legacyCfs.metadata().comparator,
                                                Slice.make(lastClustering));
                    long legacyHash = pointReadHash(key, slices, legacySSTables, columnFilter, listener);
                    long cursorHash = pointReadHash(key, slices, cursorSSTables, columnFilter, listener);
                    if (legacyHash != cursorHash)
                        return key;
                }
            }
            // A length mismatch here would already have been flagged by Phase A.
        }
        return null;
    }

    /**
     * Opens a reversed {@link UnfilteredRowIterator} on every SSTable that may contain
     * {@code key}, merges them, and returns the hash of the merged reverse-stream.
     *
     * <p>Dispatches by SSTable format ({@code BigTableReader} or
     * {@code BtiTableReader}) since the per-partition reverse-iterator API isn't
     * abstract on the common {@link SSTableReader} parent — both subclasses define
     * the same signature independently. This lets the validator handle cross-format
     * output sets cleanly when the two sides specify different
     * {@code format:} values in the YAML.
     */
    private static long reverseHashPartition(DecoratedKey key,
                                             Collection<SSTableReader> sstables,
                                             ColumnFilter columnFilter,
                                             SSTableReadsListener listener)
    {
        List<UnfilteredRowIterator> iters = new ArrayList<>(sstables.size());
        try
        {
            for (SSTableReader r : sstables)
                iters.add(openRowIterator(r, key, Slices.ALL, columnFilter, /* reversed = */ true, listener));
            try (UnfilteredRowIterator merged = UnfilteredRowIterators.merge(iters))
            {
                return PartitionHasher.hashPartition(merged);
            }
        }
        catch (Throwable t)
        {
            // Close any opened iterators that the merge() call didn't take ownership
            // of (most paths do; this finally is for the throw-during-construction case).
            for (UnfilteredRowIterator it : iters)
            {
                try { it.close(); } catch (Throwable ignored) {}
            }
            throw t;
        }
    }

    /**
     * Opens a slice-restricted {@link UnfilteredRowIterator} on every SSTable that
     * may contain {@code key}, merges them, and returns the hash of the resulting
     * (forward) slice stream. Used by the indexed point-read check to compare what
     * each side returns when seeking into a specific clustering position.
     *
     * <p>Format-agnostic — see {@link #reverseHashPartition} for the same dispatch.
     */
    private static long pointReadHash(DecoratedKey key,
                                      Slices slices,
                                      Collection<SSTableReader> sstables,
                                      ColumnFilter columnFilter,
                                      SSTableReadsListener listener)
    {
        List<UnfilteredRowIterator> iters = new ArrayList<>(sstables.size());
        try
        {
            for (SSTableReader r : sstables)
                iters.add(openRowIterator(r, key, slices, columnFilter, /* reversed = */ false, listener));
            try (UnfilteredRowIterator merged = UnfilteredRowIterators.merge(iters))
            {
                return PartitionHasher.hashPartition(merged);
            }
        }
        catch (Throwable t)
        {
            for (UnfilteredRowIterator it : iters)
            {
                try { it.close(); } catch (Throwable ignored) {}
            }
            throw t;
        }
    }

    /**
     * Format-agnostic dispatch for the per-partition row-iterator API.
     *
     * <p>{@code rowIterator(key, slices, columns, reversed, listener)} is declared
     * on each format-specific subclass ({@link BigTableReader},
     * {@link BtiTableReader}) but not on the common {@link SSTableReader} parent,
     * so we have to dispatch by {@code instanceof} ourselves. Both subclasses
     * implement identical signatures so the rest of the validator's code path
     * stays format-agnostic.
     */
    private static UnfilteredRowIterator openRowIterator(SSTableReader reader,
                                                         DecoratedKey key,
                                                         Slices slices,
                                                         ColumnFilter columnFilter,
                                                         boolean reversed,
                                                         SSTableReadsListener listener)
    {
        if (reader instanceof BigTableReader)
            return ((BigTableReader) reader).rowIterator(key, slices, columnFilter, reversed, listener);
        if (reader instanceof BtiTableReader)
            return ((BtiTableReader) reader).rowIterator(key, slices, columnFilter, reversed, listener);
        throw new IllegalStateException(
            "Validator depth-checks pass requires BIG or BTI format; got: " + reader.getClass().getName());
    }

    // ---- Phase B --------------------------------------------------------

    /**
     * Re-opens both sides' scanners restricted to the offending partition and runs
     * {@link PartitionComparator} to produce a detailed report.
     *
     * <p>If for some reason the partition key cannot be located on one side (e.g.
     * the scanner produces no rows), a synthetic report is built that describes the
     * absence.
     */
    private MismatchReport phaseBDeepCompare(DecoratedKey key) throws Exception
    {
        AbstractBounds<PartitionPosition> bounds = new Bounds<>(key, key);

        List<ISSTableScanner> legacyScanners = openScannersForBounds(legacySSTables, bounds);
        List<ISSTableScanner> cursorScanners = openScannersForBounds(cursorSSTables, bounds);

        try (UnfilteredPartitionIterator legacyIter = mergeScanners(legacyScanners);
             UnfilteredPartitionIterator cursorIter = mergeScanners(cursorScanners))
        {
            UnfilteredRowIterator legacyPart = findPartition(legacyIter, key);
            UnfilteredRowIterator cursorPart = findPartition(cursorIter, key);

            try
            {
                // Snapshot partitionsChecked once for the report so all branches see the
                // same value (the AtomicLong might still be advancing on stragglers — see
                // the comment in Phase A about not short-circuiting).
                long partitionsChecked = stats.partitionsChecked.get();
                if (legacyPart == null && cursorPart == null)
                {
                    return new MismatchReport(hex(key),
                                              "phase B could not locate partition on either side after phase A flagged it",
                                              "(no partition)",
                                              "(no partition)",
                                              partitionsChecked);
                }
                if (legacyPart == null)
                {
                    return new MismatchReport(hex(key),
                                              "partition is missing on control side",
                                              "(no partition)",
                                              "(present)",
                                              partitionsChecked);
                }
                if (cursorPart == null)
                {
                    return new MismatchReport(hex(key),
                                              "partition is missing on experiment side",
                                              "(present)",
                                              "(no partition)",
                                              partitionsChecked);
                }

                return PartitionComparator.compare(legacyPart, cursorPart, partitionsChecked);
            }
            finally
            {
                if (legacyPart != null) legacyPart.close();
                if (cursorPart != null) cursorPart.close();
            }
        }
    }

    /**
     * Advances {@code iter} until it finds a partition whose key equals {@code target},
     * or returns {@code null} if iteration ends first.  Caller is responsible for
     * closing the returned iterator.
     */
    private static UnfilteredRowIterator findPartition(UnfilteredPartitionIterator iter, DecoratedKey target)
    {
        while (iter.hasNext())
        {
            UnfilteredRowIterator part = iter.next();
            if (part.partitionKey().equals(target))
                return part;
            // Skip past unrelated partitions; close to release scanner resources.
            part.close();
        }
        return null;
    }

    // ---- Token-range splitter -------------------------------------------

    /**
     * Splits the Murmur3 token ring {@code [MIN_VALUE+1, MAX_VALUE]} into
     * {@code shards} non-overlapping {@link Bounds} that together cover the whole
     * ring. Shard {@code i}'s left token is one greater than shard {@code i-1}'s
     * right token, so adjacent shards don't double-count any partition (negligible
     * since collisions on a 64-bit boundary token are astronomically unlikely, but
     * the disjoint construction means we don't have to depend on that).
     *
     * <p>Uses {@link BigInteger} arithmetic to avoid the overflow that would happen
     * computing {@code (MAX - MIN + 1) / shards} in 64-bit when the range is the
     * full {@code 2^64 - 1} span.
     */
    private static List<AbstractBounds<PartitionPosition>> splitMurmur3Ring(int shards)
    {
        final long MIN = Long.MIN_VALUE + 1; // skip the MIN_VALUE sentinel
        final long MAX = Long.MAX_VALUE;

        BigInteger total = BigInteger.valueOf(MAX).subtract(BigInteger.valueOf(MIN)).add(BigInteger.ONE);
        BigInteger segment = total.divide(BigInteger.valueOf(shards));

        List<AbstractBounds<PartitionPosition>> result = new ArrayList<>(shards);
        long leftTok = MIN;
        for (int i = 0; i < shards; i++)
        {
            long rightTok;
            if (i == shards - 1)
            {
                rightTok = MAX;
            }
            else
            {
                // boundary_(i+1) = MIN + segment * (i+1), expressed in BigInteger to avoid wrap
                rightTok = BigInteger.valueOf(MIN)
                                     .add(segment.multiply(BigInteger.valueOf(i + 1)))
                                     .longValueExact();
            }

            Token leftToken = new Murmur3Partitioner.LongToken(leftTok);
            Token rightToken = new Murmur3Partitioner.LongToken(rightTok);
            // minKeyBound: the smallest PartitionPosition for that token (sorts before any key).
            // maxKeyBound: the largest PartitionPosition for that token (sorts after any key).
            // Bounds is inclusive-inclusive: any partition whose token is in (leftTok-1, rightTok]
            // will lie within [leftToken.minKeyBound(), rightToken.maxKeyBound()].
            result.add(new Bounds<>(leftToken.minKeyBound(), rightToken.maxKeyBound()));

            // Next shard starts strictly after this shard's right token. The last shard
            // ends at MAX, so the +1 would overflow — but we handle the last shard above
            // so this line never runs with rightTok == MAX.
            leftTok = rightTok + 1;
        }
        return result;
    }

    // ---- Scanner helpers ------------------------------------------------

    private static List<ISSTableScanner> openScanners(Collection<SSTableReader> sstables,
                                                      AbstractBounds<PartitionPosition> bounds)
    {
        List<ISSTableScanner> scanners = new ArrayList<>(sstables.size());
        for (SSTableReader r : sstables)
            scanners.add(bounds == null ? r.getScanner() : r.getScanner(bounds));
        return scanners;
    }

    private static List<ISSTableScanner> openScannersForBounds(Collection<SSTableReader> sstables,
                                                               AbstractBounds<PartitionPosition> bounds)
    {
        return openScanners(sstables, bounds);
    }

    /**
     * Merges the supplied scanners into a single partition iterator.  When the
     * collection is empty (no SSTables on this side), an empty iterator is returned
     * directly so that Phase A can detect the length mismatch.
     */
    private UnfilteredPartitionIterator mergeScanners(List<ISSTableScanner> scanners)
    {
        if (scanners.isEmpty())
            return emptyIterator();
        return UnfilteredPartitionIterators.merge(scanners, UnfilteredPartitionIterators.MergeListener.NOOP);
    }

    private UnfilteredPartitionIterator emptyIterator()
    {
        return new UnfilteredPartitionIterator()
        {
            @Override
            public org.apache.cassandra.schema.TableMetadata metadata()
            {
                return legacyCfs.metadata();
            }

            @Override
            public boolean hasNext() { return false; }

            @Override
            public UnfilteredRowIterator next() { throw new java.util.NoSuchElementException(); }

            @Override
            public void close() { /* no-op */ }
        };
    }

    // ---- Utility --------------------------------------------------------

    private static String hex(DecoratedKey key)
    {
        return org.apache.cassandra.utils.ByteBufferUtil.bytesToHex(key.getKey());
    }

    /**
     * @return the unmodifiable view of the SSTable list this validator is checking
     *         on the legacy side; useful for diagnostics
     */
    public Collection<SSTableReader> legacySSTables()
    {
        return Collections.unmodifiableCollection(legacySSTables);
    }

    /**
     * @return the unmodifiable view of the SSTable list this validator is checking
     *         on the cursor side; useful for diagnostics
     */
    public Collection<SSTableReader> cursorSSTables()
    {
        return Collections.unmodifiableCollection(cursorSSTables);
    }

    /** Daemon thread factory for the per-validator Phase A pool. */
    private static final class NamedThreadFactory implements ThreadFactory
    {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        NamedThreadFactory(String prefix) { this.prefix = prefix; }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, prefix + '-' + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
