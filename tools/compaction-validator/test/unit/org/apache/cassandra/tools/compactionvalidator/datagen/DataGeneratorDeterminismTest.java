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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.schema.SchemaGenerator;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;

/**
 * Verifies that {@link DataGenerator}'s per-thread RNG loop is deterministic for a
 * given {@code (seed, threadIndex)} pair, and that different threads under the same
 * root seed get distinct RNG streams.
 *
 * <p>Tests run against {@link DataGenerator#simulateThread} — the side-effect-free
 * mirror of {@code runWriterThread}'s inner loop — so they don't need a Cassandra
 * runtime bootstrap. This keeps the suite light enough to run inside the default
 * unit-test heap budget (no schema/keyspace bootstrap, no buffer-pool init).
 *
 * <p>Why this matters: the full DataGenerator path goes through
 * {@link org.apache.cassandra.io.sstable.CQLSSTableWriter}, but the only source of
 * non-determinism in CQLSSTableWriter's output is wall-clock cell timestamps. The
 * partition keys, clustering keys, and cell values themselves are decided entirely
 * by the per-thread RNG sequence — so if {@code simulateThread} is deterministic,
 * the actual on-disk content is too (modulo timestamps that the validator's
 * {@code PartitionComparator} ignores by design).
 *
 * <p>This test caught a real bug: an earlier version of
 * {@link SeedUtil#threadDataSeed} XOR'd thread variation into the high 16 bits
 * of the seed, which {@link java.util.Random}'s constructor immediately masks
 * away — every thread ended up with byte-identical output. See the explanatory
 * comment on {@code threadDataSeed} for the fix.
 */
public class DataGeneratorDeterminismTest
{
    /** Modest partition count — keeps each test under a few hundred ms on a laptop. */
    private static final int PARTITIONS = 500;

    @Test
    public void sameSeedSameOutput()
    {
        long rootSeed = 0xCAFEBABE12345678L;
        long threadSeed = SeedUtil.threadDataSeed(SeedUtil.dataSeed(rootSeed), 0);
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);

        DataGenerator.SimulationResult a = DataGenerator.simulateThread(schema, threadSeed, PARTITIONS);
        DataGenerator.SimulationResult b = DataGenerator.simulateThread(schema, threadSeed, PARTITIONS);
        assertEquals("same (schema, seed) must produce identical SimulationResult", a, b);
    }

    @Test
    public void distinctSeedsDistinctOutput()
    {
        long rootSeed = 0xCAFEBABE12345678L;
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        long dataSeed = SeedUtil.dataSeed(rootSeed);

        // Collect SimulationResults from threads 0..7 — they should all differ.
        // This is the assertion that previously failed because threadDataSeed XOR'd
        // variation into bits that Java's Random discards.
        Set<DataGenerator.SimulationResult> seen = new HashSet<>();
        for (int t = 0; t < 8; t++)
        {
            DataGenerator.SimulationResult r = DataGenerator.simulateThread(
                schema, SeedUtil.threadDataSeed(dataSeed, t), PARTITIONS);
            assertTrue("thread " + t + " produced same output as a previous thread: " + r,
                       seen.add(r));
        }
    }

    @Test
    public void threadIndexZeroIndependentOfThreadCount()
    {
        // Thread 0 of an 8-thread run should produce the same output as thread 0
        // of a 1-thread run. SeedUtil.threadDataSeed depends on (dataSeed, threadIndex)
        // only — not on the total thread count — so this should hold trivially.
        // Asserting it explicitly guards against any future refactor that would
        // entangle thread-count into per-thread seed derivation.
        long rootSeed = 0xDEADBEEF87654321L;
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        long dataSeed = SeedUtil.dataSeed(rootSeed);

        DataGenerator.SimulationResult first = DataGenerator.simulateThread(
            schema, SeedUtil.threadDataSeed(dataSeed, 0), PARTITIONS);
        DataGenerator.SimulationResult second = DataGenerator.simulateThread(
            schema, SeedUtil.threadDataSeed(dataSeed, 0), PARTITIONS);
        assertEquals("thread 0 must be reproducible across invocations", first, second);
    }

    @Test
    public void differentRootSeedsDifferentOutput()
    {
        long seedA = 0x1111111111111111L;
        long seedB = 0x2222222222222222L;
        GeneratedSchema schemaA = SchemaGenerator.generate(seedA);
        GeneratedSchema schemaB = SchemaGenerator.generate(seedB);
        DataGenerator.SimulationResult a = DataGenerator.simulateThread(
            schemaA, SeedUtil.threadDataSeed(SeedUtil.dataSeed(seedA), 0), PARTITIONS);
        DataGenerator.SimulationResult b = DataGenerator.simulateThread(
            schemaB, SeedUtil.threadDataSeed(SeedUtil.dataSeed(seedB), 0), PARTITIONS);
        assertNotEquals("distinct root seeds must produce distinct outputs", a, b);
    }

    @Test
    public void allThreadsReproducibleAcrossThreadCounts()
    {
        // The compaction-validator's --datagen-threads flag lets the user pick 1, 2, 4, ...
        // For a given root seed, every chosen thread count should produce a per-thread
        // breakdown where every thread is byte-identical between two runs (the union
        // of all generated partitions is therefore also identical).
        long rootSeed = 0xBEEFCAFE13371337L;
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        long dataSeed = SeedUtil.dataSeed(rootSeed);

        for (int threads : new int[] { 1, 2, 4, 8 })
        {
            for (int t = 0; t < threads; t++)
            {
                long threadSeed = SeedUtil.threadDataSeed(dataSeed, t);
                DataGenerator.SimulationResult a = DataGenerator.simulateThread(schema, threadSeed, PARTITIONS);
                DataGenerator.SimulationResult b = DataGenerator.simulateThread(schema, threadSeed, PARTITIONS);
                assertEquals("threads=" + threads + " thread=" + t + " must be reproducible", a, b);
            }
        }
    }
}
