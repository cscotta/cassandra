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
package org.apache.cassandra.tools.compactionvalidator;

/**
 * Seed derivation helper for a single compaction-validator run.
 *
 * <p>Given a 64-bit root seed, deterministic per-phase sub-seeds are derived by
 * XOR-ing with fixed constants so that each phase sees an independent stream of
 * random values while still being fully reproducible from the root seed alone.
 */
public final class RunSeed
{
    /** The root seed supplied by the user or generated randomly. */
    public final long rootSeed;

    /** Seed used by the schema generator for this run. */
    public final long schemaSeed;

    /** Seed used by the primary (single-threaded) data generator for this run. */
    public final long dataSeed;

    /**
     * Derives all sub-seeds from the given root seed.
     *
     * @param rootSeed 64-bit root seed
     */
    public RunSeed(long rootSeed)
    {
        this.rootSeed = rootSeed;
        this.schemaSeed = rootSeed ^ 0x1111111111111111L;
        this.dataSeed = rootSeed ^ 0x2222222222222222L;
    }

    /**
     * Returns a per-thread data seed derived from the run's {@code dataSeed}.
     *
     * <p>Thread indices are 0-based; the derivation shifts the index into the
     * top byte so that adjacent thread seeds are maximally separated in the
     * 64-bit space.
     *
     * @param threadIndex 0-based thread index
     * @return deterministic seed for that thread's data generator
     */
    public long threadDataSeed(int threadIndex)
    {
        return dataSeed ^ ((long) (threadIndex + 1) * 0x1000000000000000L);
    }

    /**
     * Returns the root seed for the <em>next</em> run given the current seed and
     * the just-completed run number.
     *
     * <p>Uses SplitMix64-style avalanche mixing so successive seeds look fully
     * decorrelated — every bit of {@code currentSeed} influences every bit of the
     * result. The {@code runNumber} is folded in via Weyl-sequence addition with
     * the golden-ratio constant so that even pathologically similar inputs
     * produce well-separated outputs. This avoids the previous behaviour where
     * a small XOR delta only flipped a few low bits and consecutive seeds
     * appeared identical at a glance (e.g. {@code 0x968664DE...} → {@code 0x968664DF...}).
     *
     * @param currentSeed the root seed that was just used
     * @param runNumber   the run number that just completed (1-based)
     * @return root seed for the next run
     */
    public static long nextSeed(long currentSeed, int runNumber)
    {
        // SplitMix64 finalizer (Stafford Mix13). Public-domain constants from
        // https://prng.di.unimi.it/splitmix64.c — well-mixed avalanche.
        long x = currentSeed + 0x9E3779B97F4A7C15L * (runNumber + 1);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x = x ^ (x >>> 31);
        return x;
    }
}
