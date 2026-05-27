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
package org.apache.cassandra.tools.compactionvalidator.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.cassandra.tools.compactionvalidator.config.CompactionSpec;
import org.apache.cassandra.tools.compactionvalidator.config.CompressionSpec;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;

/**
 * Generates a random but fully deterministic Cassandra {@code CREATE TABLE} schema from a seed.
 *
 * <p>All schemas produced are compatible with cursor-based compaction:
 * <ul>
 *   <li>BIG SSTable format (set externally via {@code DatabaseDescriptor.setSelectedSSTableFormat})</li>
 *   <li>Murmur3 partitioner (set externally)</li>
 *   <li>No secondary indexes</li>
 *   <li>{@code UnifiedCompactionStrategy} with randomly chosen {@code target_sstable_size}
 *       and {@code base_shard_count}</li>
 *   <li>LZ4 compression with randomly chosen {@code chunk_length_in_kb}</li>
 *   <li>No TTL / tombstone-compaction-interval settings that would interfere</li>
 * </ul>
 *
 * <p>The same {@code rootSeed} always produces the same schema, enabling reproducibility.
 */
public final class SchemaGenerator
{
    // ── Partition-key types (Int32Type, LongType, UUIDType, AsciiType, UTF8Type) ──────────────
    private static final String[] PK_TYPES = { "int", "bigint", "uuid", "ascii", "text" };

    // ── Clustering-key types (all PK types + timestamp + boolean) ────────────────────────────
    private static final String[] CK_TYPES = { "int", "bigint", "uuid", "ascii", "text", "timestamp", "boolean" };

    // ── Regular / static column types ────────────────────────────────────────────────────────
    private static final String[] REG_TYPES = {
        "int", "bigint", "uuid", "ascii", "text", "timestamp", "boolean",
        "float", "double", "blob", "varint", "decimal",
        // Collections must be frozen because cursor compaction's CursorCompactor.unsupportedSchema
        // currently rejects non-frozen ("complex") columns. Tuples are implicitly frozen.
        "frozen<set<text>>", "frozen<list<int>>", "frozen<map<text, int>>", "tuple<int, text>"
    };

    // ── UnifiedCompactionStrategy parameter choices ───────────────────────────────────────────
    private static final int[] TARGET_SSTABLE_SIZES_MIB = { 64, 128, 256, 512 };
    private static final int[] BASE_SHARD_COUNTS         = { 1, 2, 4 };

    // ── LZ4 compression chunk sizes ───────────────────────────────────────────────────────────
    private static final int[] LZ4_CHUNK_SIZES_KB = { 4, 16, 64, 256, 1024 };

    /**
     * Probability that a single seed rolls a {@link SchemaShape#WIDE} schema.
     * Kept low (10%) so the bulk of soak-run iterations stay in the narrow common
     * path; the wide path exists specifically to exercise cursor-compaction's
     * "column subset" encoding (which only kicks in when {@code regularColumnCount}
     * is large enough that a row can plausibly omit most cells), so we don't need
     * it on every run.
     */
    private static final double WIDE_SHAPE_PROBABILITY = 0.10;

    // Per-shape NULL-fraction bands. Sampled uniformly from each band when the
    // schema is rolled so successive seeds explore the whole range. Wide schemas
    // require ≥0.70 to make most rows trip the column-subset encoding path
    // (the non-empty-cell count must be much smaller than regularColumnCount for
    // that path to fire); narrow schemas use ≤0.50 so the dense common path is
    // still exercised on most runs.
    private static final double NARROW_NULL_FRACTION_MIN = 0.05;
    private static final double NARROW_NULL_FRACTION_MAX = 0.50;
    private static final double WIDE_NULL_FRACTION_MIN   = 0.70;
    private static final double WIDE_NULL_FRACTION_MAX   = 0.95;

    /**
     * {@code gc_grace_seconds} rotation. The Cassandra default (864000 = 10d) is
     * useful for production but means our test data — written seconds before
     * compaction — never enters the tombstone-purge code path. Rotating across
     * a small set keeps the existing 10d coverage on a minority of runs while
     * spending most cycles on values where compaction actively makes purge
     * decisions; both pipelines must agree on those decisions and any
     * divergence is the kind of bug this tool exists to catch.
     *
     * <p>Drawn as a paired {@code (cdf-threshold, gcGraceSeconds)} table indexed
     * by a uniform {@code rng.nextDouble()}; the thresholds give ~40% / 30% /
     * 20% / 10% across the four values listed.
     */
    private static final double[] GC_GRACE_CDF_THRESHOLDS = { 0.40, 0.70, 0.90, 1.00 };
    private static final int[]    GC_GRACE_VALUES_SECONDS = {
        0,          // 40% — aggressive purge: every tombstone eligible
        3_600,      // 30% — 1 hour: nothing in our run window is eligible, but
                    //                 the GC machinery still gates correctly
        86_400,     // 20% — 1 day: production-ish setting
        864_000     // 10% — Cassandra default; preserves existing soak coverage
    };

    private SchemaGenerator()
    {
    }

    /**
     * Generates a random Cassandra table schema from {@code rootSeed}.
     *
     * <p>Most seeds (~90%) roll the {@link SchemaShape#NARROW} shape with the historical
     * column-count bands:
     * <ul>
     *   <li>Partition-key columns: 1–2</li>
     *   <li>Clustering-key columns: 0–2</li>
     *   <li>Static columns: 0–2</li>
     *   <li>Regular columns: 3–8</li>
     * </ul>
     * The remaining ~10% roll {@link SchemaShape#WIDE}: 64–200 regular columns plus
     * 0–32 static columns. Wide schemas exercise cursor compaction's "column subset"
     * encoding path which only fires when the table has enough columns that a sparse
     * row can plausibly omit most of them.
     *
     * <p>The shape decision and the per-run NULL fraction are baked into the returned
     * {@link GeneratedSchema} so the data generator can read them directly without a
     * second seed-derivation step.
     *
     * @param rootSeed the root RNG seed for this validation run
     * @return a fully-specified, cursor-compaction-compatible {@link GeneratedSchema}
     */
    public static GeneratedSchema generate(long rootSeed)
    {
        Random rng = new Random(SeedUtil.schemaSeed(rootSeed));

        String keyspaceName = SeedUtil.keyspaceName(rootSeed);
        String tableName = "t";

        // ── Shape ────────────────────────────────────────────────────────────────────────────
        SchemaShape shape = rng.nextDouble() < WIDE_SHAPE_PROBABILITY
                            ? SchemaShape.WIDE
                            : SchemaShape.NARROW;

        // ── Column counts ────────────────────────────────────────────────────────────────────
        int pkCount      = 1 + rng.nextInt(2);          // 1 or 2
        int ckCount      = rng.nextInt(3);               // 0, 1, or 2
        int staticCount;
        int regularCount;
        if (shape == SchemaShape.WIDE)
        {
            // Wide shape: 0-32 statics + 64-200 regulars.
            // Bug 4's column-subset encoding fires when the row's present-cell count
            // is much smaller than regularColumnCount. ≥64 columns is the threshold
            // below which Columns.serialize() takes the dense (all-columns) fast path
            // even on sparse rows; above 64 the subset encoding kicks in. We aim
            // squarely above that line.
            staticCount  = rng.nextInt(33);              // 0..32
            regularCount = 64 + rng.nextInt(137);        // 64..200
        }
        else
        {
            staticCount  = rng.nextInt(3);               // 0, 1, or 2
            regularCount = 3 + rng.nextInt(6);           // 3 to 8
        }

        // Cassandra disallows STATIC columns on tables without clustering keys.
        // If we picked statics but ckCount==0, drop the statics (cheaper than re-rolling ckCount,
        // and keeps later runs deterministic by not consuming additional rng state).
        if (ckCount == 0)
            staticCount = 0;

        // ── Per-run NULL fraction ────────────────────────────────────────────────────────────
        // Rolled once and stored on the schema; the data generator reads it via
        // GeneratedSchema.getNullFraction() so every writer thread uses the same fraction.
        double nullFractionMin = shape == SchemaShape.WIDE ? WIDE_NULL_FRACTION_MIN : NARROW_NULL_FRACTION_MIN;
        double nullFractionMax = shape == SchemaShape.WIDE ? WIDE_NULL_FRACTION_MAX : NARROW_NULL_FRACTION_MAX;
        double nullFraction    = nullFractionMin + rng.nextDouble() * (nullFractionMax - nullFractionMin);

        // ── Build column lists ───────────────────────────────────────────────────────────────
        List<ColumnInfo> partitionKeys  = new ArrayList<>(pkCount);
        List<ColumnInfo> clusteringKeys = new ArrayList<>(ckCount);
        List<ColumnInfo> staticColumns  = new ArrayList<>(staticCount);
        List<ColumnInfo> regularColumns = new ArrayList<>(regularCount);

        for (int i = 0; i < pkCount; i++)
            partitionKeys.add(new ColumnInfo("pk" + i, pick(rng, PK_TYPES)));

        for (int i = 0; i < ckCount; i++)
        {
            boolean desc = rng.nextBoolean();
            clusteringKeys.add(new ColumnInfo("ck" + i, pick(rng, CK_TYPES), desc));
        }

        for (int i = 0; i < staticCount; i++)
            staticColumns.add(new ColumnInfo("s" + i, pick(rng, REG_TYPES)));

        for (int i = 0; i < regularCount; i++)
            regularColumns.add(new ColumnInfo("c" + i, pick(rng, REG_TYPES)));

        // ── Compaction defaults (seed-picked) ────────────────────────────────────────────────
        int targetSizeMib  = pick(rng, TARGET_SSTABLE_SIZES_MIB);
        int baseShardCount = pick(rng, BASE_SHARD_COUNTS);
        int lz4ChunkKb     = pick(rng, LZ4_CHUNK_SIZES_KB);

        Map<String, String> compactionOpts = new LinkedHashMap<>();
        compactionOpts.put("target_sstable_size", targetSizeMib + "MiB");
        compactionOpts.put("base_shard_count", String.valueOf(baseShardCount));
        CompactionSpec defaultCompaction = new CompactionSpec("UnifiedCompactionStrategy", compactionOpts);

        Map<String, String> compressionOpts = new LinkedHashMap<>();
        compressionOpts.put("chunk_length_in_kb", String.valueOf(lz4ChunkKb));
        CompressionSpec defaultCompression = new CompressionSpec("LZ4Compressor", compressionOpts);

        // ── gc_grace_seconds (per-run rotation) ──────────────────────────────────────────────
        // Picked from a weighted set — see GC_GRACE_VALUES_SECONDS doc for why.
        double gcRoll = rng.nextDouble();
        int gcGraceSeconds = GC_GRACE_VALUES_SECONDS[GC_GRACE_VALUES_SECONDS.length - 1];
        for (int i = 0; i < GC_GRACE_CDF_THRESHOLDS.length; i++)
        {
            if (gcRoll < GC_GRACE_CDF_THRESHOLDS[i])
            {
                gcGraceSeconds = GC_GRACE_VALUES_SECONDS[i];
                break;
            }
        }

        // ── Assemble CQL ─────────────────────────────────────────────────────────────────────
        String cql = assembleCql(keyspaceName, tableName,
                                 partitionKeys, clusteringKeys, staticColumns, regularColumns,
                                 defaultCompaction, defaultCompression, gcGraceSeconds);

        return new GeneratedSchema(cql, keyspaceName, tableName,
                                   partitionKeys, clusteringKeys, staticColumns, regularColumns,
                                   defaultCompaction, defaultCompression,
                                   shape, nullFraction, gcGraceSeconds);
    }

    // ── CQL builder ──────────────────────────────────────────────────────────────────────────

    /**
     * Assembles a {@code CREATE TABLE} CQL string from a column list and a pair of
     * {@link CompactionSpec} / {@link CompressionSpec} (which together determine the
     * {@code WITH compaction = ...} and {@code WITH compression = ...} clauses).
     *
     * <p>Package-visible so {@link GeneratedSchema#buildCqlForSide} can call it with
     * per-side overrides — both call paths run through the same string-builder so a
     * change to the CQL shape (e.g. clustering-order rules) lands in one place.
     */
    static String assembleCql(String keyspace,
                               String table,
                               List<ColumnInfo> pks,
                               List<ColumnInfo> cks,
                               List<ColumnInfo> statics,
                               List<ColumnInfo> regulars,
                               CompactionSpec compaction,
                               CompressionSpec compression,
                               int gcGraceSeconds)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE ").append(keyspace).append('.').append(table).append(" (\n");

        for (ColumnInfo col : pks)
            sb.append("  ").append(col.name).append(' ').append(col.cqlType).append(",\n");

        for (ColumnInfo col : cks)
            sb.append("  ").append(col.name).append(' ').append(col.cqlType).append(",\n");

        for (ColumnInfo col : statics)
            sb.append("  ").append(col.name).append(' ').append(col.cqlType).append(" STATIC,\n");

        for (ColumnInfo col : regulars)
            sb.append("  ").append(col.name).append(' ').append(col.cqlType).append(",\n");

        sb.append("  PRIMARY KEY (");
        if (pks.size() == 1)
        {
            sb.append(pks.get(0).name);
        }
        else
        {
            sb.append('(');
            for (int i = 0; i < pks.size(); i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(pks.get(i).name);
            }
            sb.append(')');
        }
        for (ColumnInfo ck : cks)
            sb.append(", ").append(ck.name);
        sb.append(")\n");

        sb.append(") WITH compaction = ").append(compaction.toCqlOptions());
        sb.append("\n  AND compression = ").append(compression.toCqlOptions());
        // Always emit gc_grace_seconds so the table option is explicit in the CQL —
        // helps post-mortem analysis (a soak-run reading the run log can see the
        // exact value used) and avoids any chance of silently inheriting the
        // Cassandra default from a future schema change.
        sb.append("\n  AND gc_grace_seconds = ").append(gcGraceSeconds);

        // CLUSTERING ORDER BY — only when there are clustering keys and at least one is DESC
        boolean anyDesc = false;
        for (ColumnInfo ck : cks)
        {
            if (ck.isDesc)
            {
                anyDesc = true;
                break;
            }
        }
        if (!cks.isEmpty() && anyDesc)
        {
            sb.append("\n  AND CLUSTERING ORDER BY (");
            for (int i = 0; i < cks.size(); i++)
            {
                if (i > 0) sb.append(", ");
                ColumnInfo ck = cks.get(i);
                sb.append(ck.name).append(' ').append(ck.isDesc ? "DESC" : "ASC");
            }
            sb.append(')');
        }

        sb.append(';');
        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    private static <T> T pick(Random rng, T[] array)
    {
        return array[rng.nextInt(array.length)];
    }

    private static int pick(Random rng, int[] array)
    {
        return array[rng.nextInt(array.length)];
    }
}
