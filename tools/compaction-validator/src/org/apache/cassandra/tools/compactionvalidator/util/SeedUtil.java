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
package org.apache.cassandra.tools.compactionvalidator.util;

/**
 * Utility methods for deterministic seed derivation used throughout the compaction validator.
 *
 * All seed-derivation functions are pure and deterministic: the same root seed always produces
 * the same schema and data, enabling reproducible test runs.
 */
public final class SeedUtil
{
    private SeedUtil()
    {
    }

    /**
     * Derives the schema-generation seed from the root seed.
     *
     * @param root the root RNG seed for this run
     * @return a seed suitable for driving {@code SchemaGenerator}
     */
    public static long schemaSeed(long root)
    {
        return root ^ 0x1111111111111111L;
    }

    /**
     * Derives the data-generation seed from the root seed.
     *
     * @param root the root RNG seed for this run
     * @return a seed suitable for passing to {@code DataGenerator}
     */
    public static long dataSeed(long root)
    {
        return root ^ 0x2222222222222222L;
    }

    /**
     * Derives a per-thread data seed so that each writer thread produces a different sequence
     * of random values while remaining fully deterministic.
     *
     * <p>Uses a SplitMix64 finalizer so the seed distinguishability survives Java's
     * {@code Random(long)} constructor — which discards the high 16 bits via
     * {@code & ((1L << 48) - 1)}. A naive {@code dataSeed ^ (threadIndex << K)} mix
     * with K ≥ 48 produces identical {@code Random} state across every thread index
     * (the variation is entirely in bits that get masked out), and every thread ends
     * up generating byte-identical partitions — empirically reproduced in
     * {@code DataGeneratorDeterminismDriver} and breaks the validator's ability to
     * measure cursor-vs-legacy compaction performance.
     *
     * <p>The SplitMix64 finalizer spreads variation across all 64 output bits, so the
     * low 48 bits (which is what {@code Random} actually uses) differ between thread
     * indices.
     *
     * @param dataSeed    the run-level data seed (from {@link #dataSeed})
     * @param threadIndex zero-based thread index
     * @return a seed unique to this thread, with variation distributed across all 64 bits
     */
    public static long threadDataSeed(long dataSeed, int threadIndex)
    {
        // SplitMix64 finalizer applied to (dataSeed + index * golden-ratio). Constants
        // are the standard SplitMix64 values; output is well-distributed across all 64 bits.
        long z = dataSeed + (long) (threadIndex + 1) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Formats a seed as a fixed-width 16-digit upper-case hex string prefixed with {@code 0x}.
     *
     * @param seed the seed to format
     * @return e.g. {@code "0x0123456789ABCDEF"}
     */
    public static String toHex(long seed)
    {
        return String.format("0x%016X", seed);
    }

    /**
     * Derives a short, human-readable keyspace name that is stable across runs for the same root
     * seed but unique enough to avoid collisions in practice.
     *
     * The name uses the lower 32 bits of {@code rootSeed} formatted as exactly 8 lower-case hex
     * digits, e.g. {@code "cvtest_0a1b2c3d"}.
     *
     * @param rootSeed the root RNG seed for this run
     * @return a valid CQL keyspace identifier
     */
    public static String keyspaceName(long rootSeed)
    {
        return "cvtest_" + String.format("%08x", rootSeed & 0xFFFFFFFFL);
    }
}
