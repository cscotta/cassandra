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
package org.apache.cassandra.tools.compactionvalidator.datagen;

import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cassandra.io.sstable.CQLSSTableWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.tools.compactionvalidator.schema.ColumnInfo;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.schema.SchemaShape;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Writes random SSTables to {@code outputDir} using {@link CQLSSTableWriter} until
 * at least {@code targetBytes} of on-disk data has been produced.
 *
 * <p>Each thread operates a separate {@link CQLSSTableWriter} instance (the writer is not
 * thread-safe, but multiple independent instances writing to the same directory is supported).
 * All randomness is derived deterministically from the root seed so that the same seed always
 * produces the same data, enabling reproducible comparisons.
 *
 * <p>Progress is tracked via {@link DataGenStats}. The {@code bytesWritten} counter is
 * updated whenever an SSTable is finalised through the
 * {@link CQLSSTableWriter.Builder#withSSTableProducedListener} callback.
 *
 * <h2>Generated row variety</h2>
 *
 * <p>The generator emits a mix of cell shapes that exercises every reconciliation path
 * cursor compaction is responsible for. Rolled per row from the seed:
 * <ul>
 *   <li>Live cell — a typed value of the configured CQL type.</li>
 *   <li>Tombstone cell — written by passing {@code null} for the column, which CQL
 *       interprets as a per-cell deletion at the row's timestamp.</li>
 *   <li>Omitted cell — written by passing {@link ByteBufferUtil#UNSET_BYTE_BUFFER},
 *       which {@code CQLSSTableWriter} translates into "no operation for this column"
 *       so the resulting row's encoded cell set genuinely omits it. Wide-shape
 *       schemas drive the omit fraction high so most rows trip cursor compaction's
 *       sparse-row encoding path (the bug-4 trigger).</li>
 *   <li>TTL'd cell — emitted by sending a non-zero TTL via the row-level
 *       {@code USING TTL ?} bind, with TTLs drawn from {@code [7 days, 30 days]} so
 *       no cell can possibly expire during a single run. The bug-1B trigger only
 *       requires that a cell carries {@code IS_EXPIRING_MASK} in its flags, not that
 *       it actually expire — so long TTLs avoid clock-skew false positives between
 *       the two compaction sides while still exercising the tombstone-vs-expiring
 *       reconciliation path.</li>
 * </ul>
 */
public final class DataGenerator
{
    /** Minimum per-thread SSTable buffer size (MiB). */
    private static final int MIN_SSTABLE_MIB = 1;
    /** Maximum per-thread SSTable buffer size (MiB). */
    private static final int MAX_SSTABLE_MIB = 512;

    /**
     * Default per-cell NULL fraction used by the legacy single-arg helpers and
     * {@link GeneratedSchema}'s legacy constructor. New schemas roll their own
     * fraction (and store it on the schema) via {@code SchemaGenerator.generate}.
     */
    private static final double DEFAULT_NULL_FRACTION = 0.10;

    /**
     * Per-row probability of attaching a non-zero TTL. Kept low — the bug-1B
     * trigger fires when ANY pair of cells with same (partition, column,
     * timestamp) ends up with one tombstone-style and one expiring-style; with
     * collisions on microsecond timestamps already rare, even 10% of rows
     * carrying a TTL is enough to surface the reconciliation bug given our
     * thread-overlapping partition keys.
     */
    private static final double TTL_PROBABILITY = 0.10;

    /**
     * Minimum / maximum TTL value used when a row IS rolled with a TTL. Long
     * enough that no cell can expire during a single run regardless of how
     * long compaction or validation takes — avoids false-positive mismatches
     * caused by the cursor and iterator pipelines evaluating expiry at
     * slightly different wall-clock times. The bug-1B trigger only depends on
     * cells carrying the {@code IS_EXPIRING_MASK} flag, not on their actually
     * expiring.
     */
    private static final int TTL_MIN_SECONDS =  7 * 24 * 3600;  // 7 days
    private static final int TTL_MAX_SECONDS = 30 * 24 * 3600;  // 30 days

    /**
     * Per-partition probability of rolling the "wide-row" shape (100..1000
     * rows). The 95% remainder roll the historical 1..16 shape. Wide rows in
     * combination with the larger cell sizes below let some partitions exceed
     * 64 KiB — the threshold above which the BIG format starts emitting an
     * Index.db block per ~64 KiB of partition data. Bug 3's off-by-one fires
     * only on partitions that crossed at least one block boundary, so we need
     * occasional wide partitions in the rotation.
     */
    private static final double WIDE_ROW_PROBABILITY = 0.05;
    private static final int WIDE_ROW_MIN = 100;
    private static final int WIDE_ROW_MAX = 1000;
    private static final int NARROW_ROW_MIN = 1;
    private static final int NARROW_ROW_MAX = 16;

    /**
     * Power-law probability of rolling a "large" variable-length value. The
     * remaining 99% of cells stay in the historical small range so generation
     * time stays bounded. The 1% large-value rate is enough that, combined
     * with wide-row partitions, partitions reliably cross the 64 KiB index
     * block boundary while a 10 GiB run completes in ~minutes.
     */
    private static final double LARGE_VALUE_PROBABILITY = 0.01;
    private static final int BLOB_SMALL_MIN = 1;
    private static final int BLOB_SMALL_MAX = 256;
    private static final int BLOB_LARGE_MIN = 1024;
    private static final int BLOB_LARGE_MAX = 1024 * 1024;     // 1 MiB
    private static final int TEXT_SMALL_MIN = 1;
    private static final int TEXT_SMALL_MAX = 64;
    private static final int TEXT_LARGE_MIN = 256;
    private static final int TEXT_LARGE_MAX = 16 * 1024;       // 16 KiB

    // ── Alphanumeric alphabet for random strings ─────────────────────────────────────────────
    private static final String ALNUM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // ── Configuration ─────────────────────────────────────────────────────────────────────────
    private final GeneratedSchema schema;
    private final File outputDir;
    private final long targetBytes;
    private final int threads;
    private final long rootSeed;
    /**
     * Live, mutable stats updated by the writer threads during {@link #generate()}.
     * Exposed via {@link #getStats()} so a separate ticker thread (e.g. in the run
     * orchestrator) can observe progress and publish events while data gen is in flight.
     */
    private final DataGenStats stats = new DataGenStats();

    /**
     * Creates a {@code DataGenerator}.
     *
     * @param schema      the table schema that controls which columns to write
     * @param outputDir   existing writable directory that receives the SSTable files
     * @param targetBytes approximate total on-disk bytes to produce before stopping
     * @param threads     number of concurrent writer threads
     * @param rootSeed    root RNG seed (passed straight through to {@link SeedUtil#dataSeed})
     */
    public DataGenerator(GeneratedSchema schema,
                         File outputDir,
                         long targetBytes,
                         int threads,
                         long rootSeed)
    {
        if (schema == null)     throw new IllegalArgumentException("schema must not be null");
        if (outputDir == null)  throw new IllegalArgumentException("outputDir must not be null");
        if (!outputDir.isDirectory()) throw new IllegalArgumentException("outputDir does not exist: " + outputDir);
        if (targetBytes <= 0)   throw new IllegalArgumentException("targetBytes must be positive");
        if (threads <= 0)       throw new IllegalArgumentException("threads must be positive");

        this.schema      = schema;
        this.outputDir   = outputDir;
        this.targetBytes = targetBytes;
        this.threads     = threads;
        this.rootSeed    = rootSeed;
    }

    /**
     * Returns the live stats object updated by the writer threads. Safe to read
     * concurrently with {@link #generate()} — counters are {@link java.util.concurrent.atomic.AtomicLong}-backed.
     * The returned reference is the same object {@code generate()} eventually returns.
     */
    public DataGenStats getStats()
    {
        return stats;
    }

    /** The configured target byte count. Useful for ticker threads computing progress fraction. */
    public long getTargetBytes()
    {
        return targetBytes;
    }

    /**
     * Blocks until at least {@link #targetBytes} of SSTable data has been written, then returns
     * cumulative statistics.
     *
     * <p>Each thread runs to its own private byte quota ({@code ceil(targetBytes / threads)})
     * rather than checking a shared {@code done} flag against a global counter. The earlier
     * shared-flag approach raced on AtomicLong updates from per-thread SSTable-flush listeners,
     * so the same {@code (seed, threads, targetBytes)} could produce slightly different
     * per-thread partition counts on each invocation. With per-thread quotas, every thread's
     * loop is fully deterministic given its thread seed, and the union of all generated data
     * is identical across runs.
     *
     * @return a {@link DataGenStats} snapshot (all fields final after this call returns)
     * @throws Exception if any writer thread encounters a fatal error
     */
    public DataGenStats generate() throws Exception
    {
        long dataSeed = SeedUtil.dataSeed(rootSeed);

        // Build INSERT statement for this schema. Always includes USING TTL ? as the
        // last bind variable; rows that don't want a TTL pass 0 (which CQL maps back
        // to "no TTL" — the cell is written without IS_EXPIRING_MASK).
        String insertCql = buildInsertCql();

        // Per-thread quota: ceil(target / threads) so the total target is met or slightly
        // exceeded by the natural overshoot at SSTable flush boundaries.
        long perThreadTarget = (targetBytes + threads - 1) / threads;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>(threads);

        for (int t = 0; t < threads; t++)
        {
            final int threadIndex = t;
            final long threadSeed = SeedUtil.threadDataSeed(dataSeed, threadIndex);

            futures.add(executor.submit(() -> {
                runWriterThread(threadSeed, insertCql, stats, perThreadTarget);
                return null;
            }));
        }

        executor.shutdown();
        // Wait until all threads finish; individual threads exit when their own quota is reached.
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        // Propagate any exception from worker threads
        for (Future<?> f : futures)
            f.get(); // rethrows as ExecutionException if the thread threw

        return stats;
    }

    // ── Worker thread ─────────────────────────────────────────────────────────────────────────

    private void runWriterThread(long seed, String insertCql, DataGenStats stats, long perThreadTarget)
    {
        Random rng = new Random(seed);

        // Each thread picks its own random SSTable flush size so produced SSTables vary in size
        int maxSstableMib = MIN_SSTABLE_MIB + rng.nextInt(MAX_SSTABLE_MIB - MIN_SSTABLE_MIB + 1);

        // Per-thread byte counter and stop flag — both updated only by this thread (via its own
        // CQLSSTableWriter listener), so no inter-thread race can affect when this thread stops.
        // The global `stats` counter is still updated for UI reporting.
        final java.util.concurrent.atomic.AtomicLong threadBytes =
            new java.util.concurrent.atomic.AtomicLong(0L);
        final AtomicBoolean threadDone = new AtomicBoolean(false);

        while (!threadDone.get())
        {
            try
            {
                CQLSSTableWriter writer = CQLSSTableWriter.builder()
                    .inDirectory(outputDir.getAbsolutePath())
                    .forTable(schema.getCql())
                    .using(insertCql)
                    .withMaxSSTableSizeInMiB(maxSstableMib)
                    .openSSTableOnProduced()
                    .withSSTableProducedListener((Collection<SSTableReader> produced) -> {
                        long totalOnDisk = 0L;
                        for (SSTableReader sstable : produced)
                        {
                            if (sstable == null) continue;
                            try
                            {
                                totalOnDisk += sstable.onDiskLength();
                            }
                            finally
                            {
                                // We don't keep the readers alive; close immediately to free file descriptors.
                                try { sstable.selfRef().release(); } catch (Throwable ignored) {}
                            }
                        }
                        long perThreadTotal = threadBytes.addAndGet(totalOnDisk);
                        // Update the global stats too so a UI ticker can report progress
                        // across all threads. This add is racy with other threads, but it
                        // doesn't affect this thread's stopping condition.
                        stats.recordBytes(totalOnDisk);
                        if (perThreadTotal >= perThreadTarget)
                            threadDone.set(true);
                    })
                    .build();

                try
                {
                    writePartitions(writer, rng, stats, threadDone);
                }
                finally
                {
                    writer.close();
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("SSTable writer thread failed", e);
            }
        }
    }

    /**
     * Writes partitions to {@code writer} until the {@code done} flag is raised or we have
     * produced enough data.
     *
     * <p>The per-partition row count semantics depend on whether the schema has
     * clustering columns:
     * <ul>
     *   <li>{@code ckCount >= 1}: the partition key is generated <em>once</em>
     *       per outer iteration and reused across {@code rowsPerPartition} rows.
     *       Each row gets a fresh clustering value, so the result is genuinely
     *       a multi-row partition (the cassandra-native notion of partition).
     *       {@code recordPartition()} is called once per outer iteration.</li>
     *   <li>{@code ckCount == 0}: every row is logically its own partition (CQL
     *       semantics with no clustering: multiple writes to the same PK merge
     *       into one row, so emitting multi-row "partitions" with the same PK
     *       would just be redundant overwrites). Each row generates a fresh PK
     *       and bumps {@code recordPartition()} per row.</li>
     * </ul>
     *
     * <p>This is what makes the wide-row probability and large-cell power-law
     * actually meaningful: with the previous "fresh PK per row" behaviour the
     * generator emitted only single-row partitions, so partitions never crossed
     * the 64 KiB index-block boundary that compaction bug 3 needs, and the
     * sparse-row encoding path that compaction bug 4 needs never fired (each
     * partition had at most one row, no column-subset variance across rows).
     */
    private void writePartitions(CQLSSTableWriter writer,
                                  Random rng,
                                  DataGenStats stats,
                                  AtomicBoolean done)
    throws Exception
    {
        SchemaShape shape = schema.getShape();
        double nullFraction = schema.getNullFraction();
        boolean hasClusteringKeys = !schema.getClusteringKeys().isEmpty();

        while (!done.get())
        {
            // Most partitions roll the historical 1..16 row count; a small fraction roll
            // the wide-row shape (100..1000 rows). Wide rows + large cells drive total
            // partition size past the 64 KiB Index.db block boundary that Bug 3 needs.
            int rowsPerPartition;
            if (rng.nextDouble() < WIDE_ROW_PROBABILITY)
                rowsPerPartition = WIDE_ROW_MIN + rng.nextInt(WIDE_ROW_MAX - WIDE_ROW_MIN + 1);
            else
                rowsPerPartition = NARROW_ROW_MIN + rng.nextInt(NARROW_ROW_MAX - NARROW_ROW_MIN + 1);

            if (hasClusteringKeys)
            {
                // Multi-row partition: pre-generate the PK once and reuse across rows.
                stats.recordPartition();
                Object[] sharedPk = generatePartitionKey(schema, rng);
                for (int r = 0; r < rowsPerPartition; r++)
                {
                    Object[] values = buildRowValuesWithTtlSharingPk(schema, rng, shape, nullFraction, sharedPk);
                    writer.addRow(values);
                    stats.recordRows(1);
                }
            }
            else
            {
                // 0-CK schemas: each row is structurally its own partition. Count per row
                // so {@code partitionsWritten} actually matches the unique partition count
                // the validator will see post-compaction. The rowsPerPartition loop just
                // controls how many partitions this outer iteration emits.
                for (int r = 0; r < rowsPerPartition; r++)
                {
                    stats.recordPartition();
                    Object[] values = buildRowValuesWithTtl(schema, rng, shape, nullFraction);
                    writer.addRow(values);
                    stats.recordRows(1);
                }
            }
        }
    }

    /**
     * Generates the partition-key value array for a fresh partition. Consumes one
     * {@code rng.next*} per PK column. Returned so it can be reused across all
     * rows of a multi-row partition.
     */
    private static Object[] generatePartitionKey(GeneratedSchema schema, Random rng)
    {
        List<ColumnInfo> pks = schema.getPartitionKeys();
        Object[] out = new Object[pks.size()];
        for (int i = 0; i < pks.size(); i++)
            out[i] = randomValue(pks.get(i).cqlType, rng, false);
        return out;
    }

    // ── Row-value builder ─────────────────────────────────────────────────────────────────────

    /**
     * Builds row values for the legacy (no TTL bind, no shape-aware sparsity) test path.
     * Uses {@link #DEFAULT_NULL_FRACTION} and the NARROW shape semantics — kept stable so
     * existing tests that don't roll a real schema still see deterministic output.
     */
    static Object[] buildRowValuesStatic(GeneratedSchema schema, Random rng)
    {
        return buildRowValuesStatic(schema, rng, SchemaShape.NARROW, DEFAULT_NULL_FRACTION);
    }

    /**
     * Static, side-effect-free row-value generator: same semantics as the per-row work
     * inside {@link #writePartitions} but parameterised on the schema so it can be
     * called from tests without a {@code DataGenerator} instance. Pure function of
     * {@code (schema, rng-state, shape, nullFraction)} — the RNG is advanced as columns
     * are filled in, so callers can chain successive calls to build a deterministic
     * row stream.
     *
     * <p>The {@code shape} parameter selects between two semantics for {@code nullFraction}:
     * <ul>
     *   <li>{@link SchemaShape#NARROW}: fraction is the per-cell probability of a NULL
     *       (tombstone). All columns are present; some cells are tombstones.</li>
     *   <li>{@link SchemaShape#WIDE}: fraction is the per-cell probability of OMIT
     *       (sparse encoding trigger). Within columns that ARE present, a fixed 10%
     *       chance of NULL handles the tombstone-density requirement.</li>
     * </ul>
     */
    static Object[] buildRowValuesStatic(GeneratedSchema schema, Random rng,
                                         SchemaShape shape, double nullFraction)
    {
        List<ColumnInfo> pks      = schema.getPartitionKeys();
        List<ColumnInfo> cks      = schema.getClusteringKeys();
        List<ColumnInfo> statics  = schema.getStaticColumns();
        List<ColumnInfo> regulars = schema.getRegularColumns();

        int total = pks.size() + cks.size() + statics.size() + regulars.size();
        Object[] values = new Object[total];
        int idx = 0;

        // Partition keys — never null
        for (ColumnInfo col : pks)
            values[idx++] = randomValue(col.cqlType, rng, false);

        // Clustering keys — never null
        for (ColumnInfo col : cks)
            values[idx++] = randomValue(col.cqlType, rng, false);

        // Static + regular columns: rolled per-shape to either OMIT, NULL, or LIVE.
        for (ColumnInfo col : statics)
            values[idx++] = pickColumnValue(col.cqlType, rng, shape, nullFraction);

        for (ColumnInfo col : regulars)
            values[idx++] = pickColumnValue(col.cqlType, rng, shape, nullFraction);

        return values;
    }

    /**
     * Like {@link #buildRowValuesStatic(GeneratedSchema, Random, SchemaShape, double)} but
     * appends one extra trailing slot for the row-level {@code USING TTL ?} bind — the
     * value is {@code Integer 0} for "no TTL" or a positive seconds count.
     *
     * <p>Most rows roll TTL=0 (no expiry); a small fraction roll a non-zero TTL drawn
     * from {@code [TTL_MIN_SECONDS, TTL_MAX_SECONDS]}. The TTLs are intentionally long
     * enough that no cell can expire during a single validator run regardless of how
     * long compaction or validation takes — that avoids false-positive mismatches
     * caused by the cursor and iterator pipelines evaluating expiry at slightly
     * different wall-clock times.
     */
    static Object[] buildRowValuesWithTtl(GeneratedSchema schema, Random rng,
                                          SchemaShape shape, double nullFraction)
    {
        Object[] base = buildRowValuesStatic(schema, rng, shape, nullFraction);
        return appendTtl(base, rng);
    }

    /**
     * Builds a row for a multi-row partition: the PK array is supplied (so all rows
     * in the partition share it) and only the clustering / static / regular columns
     * + TTL are freshly generated. The returned array has the same shape as
     * {@link #buildRowValuesWithTtl} so it can be passed directly to
     * {@link CQLSSTableWriter#addRow}.
     *
     * <p>RNG consumption order matches {@link #buildRowValuesWithTtl} from the CK
     * slot onward — the PK slot is just pre-populated instead of generated — so
     * tests that hash the value sequence get a determinism-preserving result.
     */
    static Object[] buildRowValuesWithTtlSharingPk(GeneratedSchema schema, Random rng,
                                                   SchemaShape shape, double nullFraction,
                                                   Object[] sharedPk)
    {
        List<ColumnInfo> pks      = schema.getPartitionKeys();
        List<ColumnInfo> cks      = schema.getClusteringKeys();
        List<ColumnInfo> statics  = schema.getStaticColumns();
        List<ColumnInfo> regulars = schema.getRegularColumns();

        if (sharedPk.length != pks.size())
            throw new IllegalArgumentException(
                "sharedPk length " + sharedPk.length + " != schema PK count " + pks.size());

        int total = pks.size() + cks.size() + statics.size() + regulars.size();
        Object[] values = new Object[total];
        int idx = 0;

        // Partition keys — reused from the caller (one set per partition).
        for (Object pkValue : sharedPk)
            values[idx++] = pkValue;

        // Clustering keys — fresh per row; this is what makes each row distinct
        // within the partition.
        for (ColumnInfo col : cks)
            values[idx++] = randomValue(col.cqlType, rng, false);

        // Static + regular columns: rolled per-shape to either OMIT, NULL, or LIVE.
        for (ColumnInfo col : statics)
            values[idx++] = pickColumnValue(col.cqlType, rng, shape, nullFraction);
        for (ColumnInfo col : regulars)
            values[idx++] = pickColumnValue(col.cqlType, rng, shape, nullFraction);

        return appendTtl(values, rng);
    }

    /**
     * Appends the trailing TTL bind slot to a value array — extracted from
     * {@link #buildRowValuesWithTtl} so {@link #buildRowValuesWithTtlSharingPk}
     * uses the identical TTL-distribution code path.
     */
    private static Object[] appendTtl(Object[] base, Random rng)
    {
        Object[] withTtl = new Object[base.length + 1];
        System.arraycopy(base, 0, withTtl, 0, base.length);
        boolean wantTtl = rng.nextDouble() < TTL_PROBABILITY;
        int ttl = wantTtl
                  ? TTL_MIN_SECONDS + rng.nextInt(TTL_MAX_SECONDS - TTL_MIN_SECONDS + 1)
                  : 0;
        withTtl[base.length] = ttl;
        return withTtl;
    }

    /**
     * Decides what to write for a single static or regular column. Returns one of:
     * <ul>
     *   <li>{@link ByteBufferUtil#UNSET_BYTE_BUFFER} — column is OMITTED (no cell
     *       written). Causes the row to be encoded with a smaller "present columns"
     *       set, exercising cursor compaction's sparse-row encoding path. Only
     *       returned for {@link SchemaShape#WIDE} schemas.</li>
     *   <li>{@code null} — column is set to NULL, which CQL serialises as a per-cell
     *       tombstone at the row's timestamp.</li>
     *   <li>A typed Java value — a live cell of the column's type.</li>
     * </ul>
     */
    private static Object pickColumnValue(String cqlType, Random rng,
                                          SchemaShape shape, double nullFraction)
    {
        if (shape == SchemaShape.WIDE)
        {
            // For wide schemas: roll for OMIT first. The remaining "present" cells get
            // a moderate (10%) tombstone fraction so each row has a mix of live cells
            // and tombstones — useful for the bug-1B trigger and for tombstone-cell
            // reconciliation in general.
            if (rng.nextDouble() < nullFraction)
                return ByteBufferUtil.UNSET_BYTE_BUFFER;
            boolean tombstone = rng.nextDouble() < 0.10;
            return randomValue(cqlType, rng, tombstone);
        }
        else
        {
            // Narrow shape: the per-run nullFraction IS the tombstone probability.
            // No omits — all columns are written (live or tombstone).
            boolean tombstone = rng.nextDouble() < nullFraction;
            return randomValue(cqlType, rng, tombstone);
        }
    }

    /**
     * Test-only simulation of {@link #runWriterThread}'s deterministic per-thread RNG
     * loop, decoupled from {@link CQLSSTableWriter}. Reproduces the exact RNG draws a
     * single writer thread would make against the supplied {@code schema}'s shape +
     * NULL fraction.
     *
     * <p>The hash returned is a Java-32-bit-fold over the value sequence's
     * {@link Arrays#deepHashCode} — it's deterministic across JVM runs because
     * {@code Arrays.deepHashCode} is specified to be deterministic on Strings,
     * boxed numerics, and byte arrays.
     *
     * <p>Used by {@code DataGeneratorDeterminismTest} to verify the per-thread RNG
     * loop is byte-for-byte reproducible given {@code (schema, seed)}. The full
     * end-to-end path through {@link CQLSSTableWriter} carries the same guarantee
     * by construction (the only non-deterministic input is wall-clock-derived cell
     * timestamps, which don't affect the row values themselves).
     *
     * @param schema          schema describing the table layout (column counts/types)
     * @param threadSeed      per-thread RNG seed (typically {@link SeedUtil#threadDataSeed})
     * @param partitionCount  number of partitions to simulate (real loop stops on byte
     *                        quota; this helper uses an explicit count so it's
     *                        time-bounded for unit tests)
     * @return snapshot of partition/row counts plus a content fingerprint
     */
    public static SimulationResult simulateThread(GeneratedSchema schema, long threadSeed, int partitionCount)
    {
        SchemaShape shape = schema.getShape();
        double nullFraction = schema.getNullFraction();
        boolean hasClusteringKeys = !schema.getClusteringKeys().isEmpty();
        Random rng = new Random(threadSeed);
        int maxSstableMib = MIN_SSTABLE_MIB + rng.nextInt(MAX_SSTABLE_MIB - MIN_SSTABLE_MIB + 1);
        long partitions = 0L;
        long rows = 0L;
        long fingerprint = 1469598103934665603L; // FNV-64 offset basis — arbitrary 64-bit seed
        for (int p = 0; p < partitionCount; p++)
        {
            int rowsPerPartition;
            if (rng.nextDouble() < WIDE_ROW_PROBABILITY)
                rowsPerPartition = WIDE_ROW_MIN + rng.nextInt(WIDE_ROW_MAX - WIDE_ROW_MIN + 1);
            else
                rowsPerPartition = NARROW_ROW_MIN + rng.nextInt(NARROW_ROW_MAX - NARROW_ROW_MIN + 1);

            if (hasClusteringKeys)
            {
                partitions++;
                Object[] sharedPk = generatePartitionKey(schema, rng);
                for (int r = 0; r < rowsPerPartition; r++)
                {
                    Object[] values = buildRowValuesWithTtlSharingPk(schema, rng, shape, nullFraction, sharedPk);
                    rows++;
                    long h = Arrays.deepHashCode(values) & 0xFFFFFFFFL;
                    fingerprint ^= h;
                    fingerprint *= 1099511628211L; // FNV-64 prime
                }
            }
            else
            {
                for (int r = 0; r < rowsPerPartition; r++)
                {
                    partitions++;
                    Object[] values = buildRowValuesWithTtl(schema, rng, shape, nullFraction);
                    rows++;
                    long h = Arrays.deepHashCode(values) & 0xFFFFFFFFL;
                    fingerprint ^= h;
                    fingerprint *= 1099511628211L; // FNV-64 prime
                }
            }
        }
        return new SimulationResult(partitions, rows, fingerprint, maxSstableMib);
    }

    /**
     * Snapshot returned by {@link #simulateThread}: counts and content fingerprint that
     * together let a test confirm two runs produced byte-identical row sequences.
     */
    public static final class SimulationResult
    {
        public final long partitionsGenerated;
        public final long rowsGenerated;
        public final long contentFingerprint;
        public final int maxSstableMib;

        SimulationResult(long partitionsGenerated, long rowsGenerated,
                         long contentFingerprint, int maxSstableMib)
        {
            this.partitionsGenerated = partitionsGenerated;
            this.rowsGenerated = rowsGenerated;
            this.contentFingerprint = contentFingerprint;
            this.maxSstableMib = maxSstableMib;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof SimulationResult)) return false;
            SimulationResult r = (SimulationResult) o;
            return partitionsGenerated == r.partitionsGenerated
                   && rowsGenerated == r.rowsGenerated
                   && contentFingerprint == r.contentFingerprint
                   && maxSstableMib == r.maxSstableMib;
        }

        @Override
        public int hashCode()
        {
            return Long.hashCode(contentFingerprint) ^ Long.hashCode(rowsGenerated);
        }

        @Override
        public String toString()
        {
            return "SimulationResult{partitions=" + partitionsGenerated
                   + ", rows=" + rowsGenerated
                   + ", fingerprint=" + Long.toHexString(contentFingerprint)
                   + ", maxSstableMib=" + maxSstableMib + '}';
        }
    }

    /**
     * Generates a random Java value compatible with {@code cqlType} as expected by
     * {@link CQLSSTableWriter#addRow(Object...)}.
     *
     * <p>Returns {@code null} when {@code allowNull} is {@code true}.
     */
    private static Object randomValue(String cqlType, Random rng, boolean allowNull)
    {
        if (allowNull)
            return null;

        switch (cqlType)
        {
            case "int":
                return rng.nextInt();

            case "bigint":
                return rng.nextLong();

            case "varint":
                return java.math.BigInteger.valueOf(rng.nextLong());

            case "uuid":
                return randomUuid(rng);

            case "text":
            case "ascii":
                // 99% of cells are short alphanumeric; the rare 1% large cell drives
                // partition size past the 64 KiB Index.db block boundary needed by Bug 3.
                if (rng.nextDouble() < LARGE_VALUE_PROBABILITY)
                    return randomString(rng, TEXT_LARGE_MIN, TEXT_LARGE_MAX);
                return randomString(rng, TEXT_SMALL_MIN, TEXT_SMALL_MAX);

            case "timestamp":
                return new Date(rng.nextLong() & Long.MAX_VALUE);

            case "boolean":
                return rng.nextBoolean();

            case "float":
                return rng.nextFloat();

            case "double":
                return rng.nextDouble();

            case "blob":
                if (rng.nextDouble() < LARGE_VALUE_PROBABILITY)
                    return randomBytes(rng, BLOB_LARGE_MIN, BLOB_LARGE_MAX);
                return randomBytes(rng, BLOB_SMALL_MIN, BLOB_SMALL_MAX);

            case "decimal":
                return new BigDecimal(java.math.BigInteger.valueOf(rng.nextInt(10000)), rng.nextInt(5));

            case "set<text>":
            case "frozen<set<text>>":
            {
                Set<String> s = new HashSet<>();
                s.add("a" + rng.nextInt());
                s.add("b" + rng.nextInt());
                return s;
            }

            case "list<int>":
            case "frozen<list<int>>":
                return Arrays.asList(rng.nextInt(), rng.nextInt());

            case "map<text, int>":
            case "frozen<map<text, int>>":
            {
                Map<String, Integer> m = new HashMap<>();
                int entries = 1 + rng.nextInt(3);
                for (int i = 0; i < entries; i++)
                    m.put("k" + rng.nextInt(1000), rng.nextInt());
                return m;
            }

            case "tuple<int, text>":
                // CQLSSTableWriter does not directly support java.util.List for tuple values
                // in all Cassandra versions; pass null to avoid codec resolution issues.
                // A future milestone can wire up TupleValue once the codec layer is confirmed.
                return null;

            default:
                // Unknown / unsupported type: pass null rather than crashing the run
                return null;
        }
    }

    // ── CQL builder ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds the INSERT CQL string with one bind variable per column, in the order:
     * partition keys, clustering keys, static columns, regular columns. The trailing
     * {@code USING TTL ?} bind is appended unconditionally — rows that don't want a
     * TTL bind {@code 0}, which CQL maps back to "no TTL" (the cell is written
     * without {@code IS_EXPIRING_MASK}).
     */
    private String buildInsertCql()
    {
        List<ColumnInfo> pks      = schema.getPartitionKeys();
        List<ColumnInfo> cks      = schema.getClusteringKeys();
        List<ColumnInfo> statics  = schema.getStaticColumns();
        List<ColumnInfo> regulars = schema.getRegularColumns();

        StringBuilder cols = new StringBuilder();
        StringBuilder binds = new StringBuilder();

        for (ColumnInfo col : pks)
        {
            if (cols.length() > 0) { cols.append(", "); binds.append(", "); }
            cols.append(col.name);
            binds.append('?');
        }
        for (ColumnInfo col : cks)
        {
            cols.append(", "); binds.append(", ");
            cols.append(col.name);
            binds.append('?');
        }
        for (ColumnInfo col : statics)
        {
            cols.append(", "); binds.append(", ");
            cols.append(col.name);
            binds.append('?');
        }
        for (ColumnInfo col : regulars)
        {
            cols.append(", "); binds.append(", ");
            cols.append(col.name);
            binds.append('?');
        }

        return "INSERT INTO " + schema.getKeyspaceName() + '.' + schema.getTableName()
               + " (" + cols + ") VALUES (" + binds + ") USING TTL ?";
    }

    // ── Random-value helpers ──────────────────────────────────────────────────────────────────

    private static UUID randomUuid(Random rng)
    {
        // Construct a version-4 UUID from two random longs
        long msb = rng.nextLong();
        long lsb = rng.nextLong();
        // Set version bits to 4
        msb = (msb & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000004000L;
        // Set variant bits
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    private static String randomString(Random rng, int minLen, int maxLen)
    {
        int len = minLen + rng.nextInt(maxLen - minLen + 1);
        char[] chars = new char[len];
        for (int i = 0; i < len; i++)
            chars[i] = ALNUM.charAt(rng.nextInt(ALNUM.length()));
        return new String(chars);
    }

    private static ByteBuffer randomBytes(Random rng, int minLen, int maxLen)
    {
        int len = minLen + rng.nextInt(maxLen - minLen + 1);
        byte[] bytes = new byte[len];
        rng.nextBytes(bytes);
        return ByteBuffer.wrap(bytes);
    }
}
