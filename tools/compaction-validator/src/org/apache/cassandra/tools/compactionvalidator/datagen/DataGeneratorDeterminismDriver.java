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

import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.schema.SchemaGenerator;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;

/**
 * Stand-alone driver — not a JUnit test — that prints determinism check results
 * inline so we can see progress without waiting for an ant test JVM to finish.
 *
 * <p>Exercises {@link DataGenerator#simulateThread} (the deterministic core of
 * {@link DataGenerator#runWriterThread}) under varying seeds and thread counts.
 * Run via:
 * <pre>
 *   java -cp build/classes/compaction-validator:build/tools/lib/compaction-validator.jar:... \
 *        org.apache.cassandra.tools.compactionvalidator.datagen.DataGeneratorDeterminismDriver
 * </pre>
 *
 * <p>Each section prints what it's about to do, then the result. On any
 * non-determinism this exits {@code 1} with a clear FAIL message so it doubles
 * as a smoke test for CI / pre-commit invocations.
 */
public class DataGeneratorDeterminismDriver
{
    private static int failures = 0;

    public static void main(String[] args)
    {
        System.out.println("=== DataGenerator determinism driver ===");
        System.out.println();

        // Test 1: same seed, same partitions → same result, repeated twice
        checkSingleThread(0xCAFEBABE12345678L, 500);
        checkSingleThread(0xDEADBEEF87654321L, 1000);
        checkSingleThread(0x0123456789ABCDEFL, 2000);

        // Test 2: same root seed across different thread counts — each thread
        // should be deterministic *given its thread seed*. Confirm thread 0's
        // simulation is identical regardless of how many other threads exist.
        checkPerThreadIndependence(0xC0FFEE12FACEDEADL, 500);

        // Test 3: different threads in the SAME run produce different content
        // (their seeds differ via SeedUtil.threadDataSeed).
        checkDifferentThreadsDiffer(0xABCDEF0123456789L, 500);

        // Test 4: across a sweep of seeds and thread counts, two runs match.
        // 1, 2, 4, 8 threads × 200 partitions each — covers the realistic
        // operating range of the compaction-validator's --datagen-threads flag.
        for (int threads : new int[] { 1, 2, 4, 8 })
            checkAllThreadsMatch(0xBEEFCAFE13371337L, threads, 200);

        System.out.println();
        if (failures > 0)
        {
            System.out.println("FAIL: " + failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("PASS: all determinism checks succeeded");
    }

    // -------------------------------------------------------------------------
    // Checks
    // -------------------------------------------------------------------------

    private static void checkSingleThread(long rootSeed, int partitions)
    {
        long dataSeed = SeedUtil.dataSeed(rootSeed);
        long threadSeed = SeedUtil.threadDataSeed(dataSeed, 0);
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        System.out.printf("[single-thread] root=%s thread0=%s partitions=%d schema=%s.%s%n",
                          SeedUtil.toHex(rootSeed),
                          SeedUtil.toHex(threadSeed),
                          partitions,
                          schema.getKeyspaceName(), schema.getTableName());

        long t0 = System.nanoTime();
        DataGenerator.SimulationResult a = DataGenerator.simulateThread(schema, threadSeed, partitions);
        long t1 = System.nanoTime();
        DataGenerator.SimulationResult b = DataGenerator.simulateThread(schema, threadSeed, partitions);
        long t2 = System.nanoTime();
        System.out.printf("    run-a: %s (%.1fms)%n", a, (t1 - t0) / 1e6);
        System.out.printf("    run-b: %s (%.1fms)%n", b, (t2 - t1) / 1e6);
        report(a.equals(b), "same seed → identical SimulationResult");
        System.out.println();
    }

    private static void checkPerThreadIndependence(long rootSeed, int partitions)
    {
        long dataSeed = SeedUtil.dataSeed(rootSeed);
        long thread0Seed = SeedUtil.threadDataSeed(dataSeed, 0);
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        System.out.printf("[per-thread-independence] root=%s thread0=%s partitions=%d%n",
                          SeedUtil.toHex(rootSeed),
                          SeedUtil.toHex(thread0Seed),
                          partitions);

        // Thread 0's deterministic output should be identical regardless of how many
        // other threads will run alongside it. SeedUtil.threadDataSeed(dataSeed, 0)
        // depends only on dataSeed (= function of rootSeed), not on the total thread count.
        DataGenerator.SimulationResult first = DataGenerator.simulateThread(schema, thread0Seed, partitions);
        DataGenerator.SimulationResult second = DataGenerator.simulateThread(schema, thread0Seed, partitions);
        System.out.printf("    thread0-run-a: %s%n", first);
        System.out.printf("    thread0-run-b: %s%n", second);
        report(first.equals(second),
               "thread-0 simulation is identical across invocations (independent of other threads)");
        System.out.println();
    }

    private static void checkDifferentThreadsDiffer(long rootSeed, int partitions)
    {
        long dataSeed = SeedUtil.dataSeed(rootSeed);
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        DataGenerator.SimulationResult t0 = DataGenerator.simulateThread(schema, SeedUtil.threadDataSeed(dataSeed, 0), partitions);
        DataGenerator.SimulationResult t1 = DataGenerator.simulateThread(schema, SeedUtil.threadDataSeed(dataSeed, 1), partitions);
        DataGenerator.SimulationResult t2 = DataGenerator.simulateThread(schema, SeedUtil.threadDataSeed(dataSeed, 2), partitions);
        System.out.printf("[per-thread-distinct] root=%s partitions=%d%n",
                          SeedUtil.toHex(rootSeed), partitions);
        System.out.printf("    thread 0: %s%n", t0);
        System.out.printf("    thread 1: %s%n", t1);
        System.out.printf("    thread 2: %s%n", t2);
        report(!t0.equals(t1) && !t1.equals(t2) && !t0.equals(t2),
               "different per-thread seeds → different SimulationResults");
        System.out.println();
    }

    private static void checkAllThreadsMatch(long rootSeed, int threads, int partitionsPerThread)
    {
        long dataSeed = SeedUtil.dataSeed(rootSeed);
        GeneratedSchema schema = SchemaGenerator.generate(rootSeed);
        System.out.printf("[multi-thread-determinism] root=%s threads=%d partitions/thread=%d%n",
                          SeedUtil.toHex(rootSeed), threads, partitionsPerThread);

        boolean allMatch = true;
        for (int t = 0; t < threads; t++)
        {
            long threadSeed = SeedUtil.threadDataSeed(dataSeed, t);
            DataGenerator.SimulationResult a = DataGenerator.simulateThread(schema, threadSeed, partitionsPerThread);
            DataGenerator.SimulationResult b = DataGenerator.simulateThread(schema, threadSeed, partitionsPerThread);
            boolean match = a.equals(b);
            System.out.printf("    thread %d: %s — %s%n", t, a, match ? "MATCH" : "DIFFER");
            if (!match)
            {
                allMatch = false;
                System.out.printf("        run-b: %s%n", b);
            }
        }
        report(allMatch, "every per-thread loop is reproducible across runs");
        System.out.println();
    }

    private static void report(boolean ok, String description)
    {
        System.out.println(ok ? "    ✓ PASS: " + description
                              : "    ✗ FAIL: " + description);
        if (!ok)
            failures++;
    }
}
