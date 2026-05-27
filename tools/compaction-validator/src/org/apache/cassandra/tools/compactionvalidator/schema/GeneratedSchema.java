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

import java.util.Collections;
import java.util.List;

import org.apache.cassandra.tools.compactionvalidator.config.CompactionSpec;
import org.apache.cassandra.tools.compactionvalidator.config.CompressionSpec;
import org.apache.cassandra.tools.compactionvalidator.config.SideConfig;

/**
 * The result of {@link SchemaGenerator#generate}: a fully-specified Cassandra table schema
 * together with decomposed column metadata that the data generator can use without re-parsing
 * the CQL string.
 *
 * <p>Holds two kinds of information:
 * <ul>
 *   <li>The <em>shared</em> column structure (partition keys, clustering keys, statics,
 *       regulars). Identical across both sides of an A/B comparison run.</li>
 *   <li>The <em>seed-picked default</em> compaction + compression specs. Used as the
 *       fallback for any side that doesn't pin those attributes in its
 *       {@link SideConfig}, AND used unmodified for the data-generation phase (which
 *       writes one neutral SSTable set that's then hard-linked into both sides).</li>
 * </ul>
 *
 * <p>{@link #buildCqlForSide(String, SideConfig)} composes the per-side {@code CREATE
 * TABLE} CQL by overlaying a side's overrides on the seed-picked defaults — this is
 * what lets the validator hold the schema constant while varying compaction strategy
 * or compression codec across sides.
 *
 * <p>Instances are immutable (the lists are wrapped in unmodifiable views).
 */
public final class GeneratedSchema
{
    /**
     * The complete {@code CREATE TABLE} CQL statement using the seed-picked defaults.
     * Used by the data-generation path (CQLSSTableWriter only needs to know the column
     * structure; the compaction options in this CQL aren't actually exercised at write
     * time, they just need to be syntactically valid).
     */
    private final String cql;

    private final String keyspaceName;
    private final String tableName;
    private final List<ColumnInfo> partitionKeys;
    private final List<ColumnInfo> clusteringKeys;
    private final List<ColumnInfo> staticColumns;
    private final List<ColumnInfo> regularColumns;
    private final CompactionSpec defaultCompaction;
    private final CompressionSpec defaultCompression;
    private final SchemaShape shape;
    private final double nullFraction;
    private final int gcGraceSeconds;

    /**
     * Legacy constructor that defaults the shape to {@link SchemaShape#NARROW},
     * the null fraction to {@code 0.10}, and {@code gc_grace_seconds} to the
     * Cassandra default (864000 = 10 days). Kept so existing tests that build
     * a {@code GeneratedSchema} directly without a generator round-trip don't
     * have to be touched. New code should call the full constructor.
     */
    public GeneratedSchema(String cql,
                           String keyspaceName,
                           String tableName,
                           List<ColumnInfo> partitionKeys,
                           List<ColumnInfo> clusteringKeys,
                           List<ColumnInfo> staticColumns,
                           List<ColumnInfo> regularColumns,
                           CompactionSpec defaultCompaction,
                           CompressionSpec defaultCompression)
    {
        this(cql, keyspaceName, tableName,
             partitionKeys, clusteringKeys, staticColumns, regularColumns,
             defaultCompaction, defaultCompression,
             SchemaShape.NARROW, 0.10, 864_000);
    }

    /**
     * Full constructor including the shape tag, per-run null fraction, and
     * per-run {@code gc_grace_seconds}.
     *
     * @param cql                 full {@code CREATE TABLE} CQL with seed-picked defaults
     * @param keyspaceName        keyspace name
     * @param tableName           table name
     * @param partitionKeys       partition-key columns (must not be null)
     * @param clusteringKeys      clustering-key columns (must not be null)
     * @param staticColumns       static columns (must not be null)
     * @param regularColumns      regular columns (must not be null)
     * @param defaultCompaction   seed-picked compaction spec — used by sides that omit theirs
     * @param defaultCompression  seed-picked compression spec — used by sides that omit theirs
     * @param shape               structural shape rolled for this schema
     * @param nullFraction        probability that a non-PK / non-CK column is written
     *                            as {@code NULL} in any given row. Wide schemas roll
     *                            high (≥0.70) so most rows trip the column-subset
     *                            encoding path; narrow schemas roll low (≤0.50) so
     *                            most rows are dense.
     * @param gcGraceSeconds      seed-picked {@code gc_grace_seconds} table option.
     *                            Drives whether the compaction tombstone-purge code
     *                            path runs at all: at the Cassandra default of
     *                            10 days nothing in our test data is purgeable, so
     *                            both pipelines skip the purge logic; at 0 every
     *                            tombstone is eligible and the iterator and cursor
     *                            sides must agree on the resulting purge decisions.
     */
    public GeneratedSchema(String cql,
                           String keyspaceName,
                           String tableName,
                           List<ColumnInfo> partitionKeys,
                           List<ColumnInfo> clusteringKeys,
                           List<ColumnInfo> staticColumns,
                           List<ColumnInfo> regularColumns,
                           CompactionSpec defaultCompaction,
                           CompressionSpec defaultCompression,
                           SchemaShape shape,
                           double nullFraction,
                           int gcGraceSeconds)
    {
        if (shape == null)
            throw new IllegalArgumentException("shape must not be null");
        if (Double.isNaN(nullFraction) || nullFraction < 0.0 || nullFraction > 1.0)
            throw new IllegalArgumentException("nullFraction must be in [0,1]: " + nullFraction);
        if (gcGraceSeconds < 0)
            throw new IllegalArgumentException("gcGraceSeconds must be non-negative: " + gcGraceSeconds);
        this.cql = cql;
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
        this.partitionKeys = Collections.unmodifiableList(partitionKeys);
        this.clusteringKeys = Collections.unmodifiableList(clusteringKeys);
        this.staticColumns = Collections.unmodifiableList(staticColumns);
        this.regularColumns = Collections.unmodifiableList(regularColumns);
        this.defaultCompaction = defaultCompaction;
        this.defaultCompression = defaultCompression;
        this.shape = shape;
        this.nullFraction = nullFraction;
        this.gcGraceSeconds = gcGraceSeconds;
    }

    /** @return full CREATE TABLE CQL with seed-picked compaction/compression defaults */
    public String getCql()
    {
        return cql;
    }

    public String getKeyspaceName() { return keyspaceName; }
    public String getTableName() { return tableName; }
    public List<ColumnInfo> getPartitionKeys() { return partitionKeys; }
    public List<ColumnInfo> getClusteringKeys() { return clusteringKeys; }
    public List<ColumnInfo> getStaticColumns() { return staticColumns; }
    public List<ColumnInfo> getRegularColumns() { return regularColumns; }

    /** @return seed-picked compaction defaults — useful for run-summary output */
    public CompactionSpec getDefaultCompaction()  { return defaultCompaction; }
    /** @return seed-picked compression defaults */
    public CompressionSpec getDefaultCompression() { return defaultCompression; }
    /** @return the structural shape rolled for this schema */
    public SchemaShape getShape() { return shape; }
    /**
     * @return per-run NULL probability for non-PK / non-CK columns. Wide schemas
     *         roll a high value to force sparse-row encoding; narrow schemas roll
     *         a low-to-moderate value to keep most rows dense.
     */
    public double getNullFraction() { return nullFraction; }
    /**
     * @return per-run {@code gc_grace_seconds} table option. Drives whether the
     *         compaction tombstone-purge path runs at all. Logged so soak-run
     *         post-mortem can correlate divergences with whether GC was active.
     */
    public int getGcGraceSeconds() { return gcGraceSeconds; }

    /**
     * Builds a per-side {@code CREATE TABLE} CQL: same column structure as
     * {@link #getCql()}, but with the keyspace renamed and the compaction +
     * compression clauses replaced by whatever the side specified (with the
     * seed-picked defaults filling in for any null fields).
     *
     * @param keyspaceOverride  keyspace to embed in the CQL (typically
     *                          {@code <baseKs>_<sideName>})
     * @param side              the side being prepared (either control or experiment)
     * @return CQL ready to register as the side's keyspace.table
     */
    public String buildCqlForSide(String keyspaceOverride, SideConfig side)
    {
        CompactionSpec compaction = (side != null && side.compaction != null && side.compaction.className != null)
                                    ? side.compaction
                                    : defaultCompaction;
        CompressionSpec compression = (side != null && side.compression != null && side.compression.className != null)
                                      ? side.compression
                                      : defaultCompression;
        return SchemaGenerator.assembleCql(keyspaceOverride, tableName,
                                           partitionKeys, clusteringKeys,
                                           staticColumns, regularColumns,
                                           compaction, compression, gcGraceSeconds);
    }

    @Override
    public String toString()
    {
        return "GeneratedSchema{keyspace='" + keyspaceName + "', table='" + tableName + "', "
               + "shape=" + shape
               + ", pkCols=" + partitionKeys.size()
               + ", ckCols=" + clusteringKeys.size()
               + ", staticCols=" + staticColumns.size()
               + ", regularCols=" + regularColumns.size()
               + ", nullFraction=" + String.format("%.2f", nullFraction)
               + ", gcGraceSeconds=" + gcGraceSeconds
               + ", defaultCompaction=" + defaultCompaction
               + ", defaultCompression=" + defaultCompression
               + "}";
    }
}
