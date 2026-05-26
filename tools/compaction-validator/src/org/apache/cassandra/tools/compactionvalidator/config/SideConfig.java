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
package org.apache.cassandra.tools.compactionvalidator.config;

/**
 * Configuration for one side of an A/B comparison run. The {@code control} side is
 * the baseline / known-good configuration; {@code experiment} is the variation
 * under test. Both sides must produce byte-/cell-identical compaction output for
 * the run to PASS — any divergence is reported with the offending side's
 * configuration so the developer knows which axis perturbed the result.
 *
 * <p>Any field left {@code null} inherits a per-seed-randomised value picked
 * once and applied to BOTH sides — i.e. omitting an attribute means "I'm not
 * varying this; let the seed decide". The orchestrator computes the inherited
 * values and substitutes them into both sides before constructing CQL.
 */
public final class SideConfig
{
    /** Human-readable label used in TUI panels, log entries, and mismatch reports. */
    public String name;

    /**
     * Compaction pipeline implementation. Maps to
     * {@link org.apache.cassandra.db.compaction.PipelineSelector.Backend}.
     * {@code null} → randomised per seed.
     */
    public String pipeline; // ITERATOR | CURSOR

    /** Compaction strategy spec — class name + per-strategy options. {@code null} → randomised. */
    public CompactionSpec compaction;

    /** Compression codec spec — class name + options. {@code null} → randomised. */
    public CompressionSpec compression;

    /**
     * Cassandra {@code disk_access_mode} value for this side. {@code null} → use
     * the JVM default.
     *
     * <p>Validator constraint for the first cut: the two sides MUST specify the
     * same value (or both omit it). Per-side I/O modes require either subprocess
     * isolation or per-CFS overrides that Cassandra doesn't expose; serialised
     * back-to-back runs let you compare modes meaningfully today.
     */
    public String ioMode;

    public SideConfig() {}

    public String getName() { return name; }
    public String getPipeline() { return pipeline; }
    public CompactionSpec getCompaction() { return compaction; }
    public CompressionSpec getCompression() { return compression; }
    public String getIoMode() { return ioMode; }

    /** SnakeYAML uses snake_case in YAML; map `io_mode` → ioMode. */
    public void setIoMode(String ioMode) { this.ioMode = ioMode; }
    public void setIo_mode(String ioMode) { this.ioMode = ioMode; }

    @Override
    public String toString()
    {
        return "SideConfig{name=" + name + ", pipeline=" + pipeline
               + ", compaction=" + compaction + ", compression=" + compression
               + ", ioMode=" + ioMode + '}';
    }
}
