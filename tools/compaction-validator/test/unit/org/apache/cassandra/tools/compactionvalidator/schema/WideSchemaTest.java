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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link SchemaGenerator} reliably produces wide-shape schemas at
 * (roughly) the configured 10% probability, and that wide schemas satisfy the
 * structural properties that compaction bug 4's sparse-row encoding path needs:
 * ≥ 64 regular columns and a per-cell NULL/omit probability ≥ 0.70.
 *
 * <p>The probability assertion is sample-size-bounded — across 1000 seeds the
 * empirical wide-shape rate sits inside [0.05, 0.20] with very high probability
 * (Chebyshev / binomial CI). Tightening the bound below this would make the
 * test flaky on rare RNG draws.
 */
public class WideSchemaTest
{
    private static final int SAMPLES = 1000;
    private static final double EXPECTED_WIDE_RATE = 0.10;
    private static final double WIDE_RATE_TOLERANCE = 0.05;

    @Test
    public void wideShapeProbabilityWithinExpectedBand()
    {
        int wide = 0;
        int narrow = 0;
        for (int i = 0; i < SAMPLES; i++)
        {
            // Stir the seed so each sample is an independent SchemaGenerator roll.
            long seed = 0xC0FFEEL ^ (long) i * 0x9E3779B97F4A7C15L;
            GeneratedSchema schema = SchemaGenerator.generate(seed);
            if (schema.getShape() == SchemaShape.WIDE) wide++;
            else if (schema.getShape() == SchemaShape.NARROW) narrow++;
        }
        assertEquals("every schema should roll exactly one of NARROW or WIDE",
                     SAMPLES, wide + narrow);
        double observed = (double) wide / SAMPLES;
        assertTrue("wide-shape rate " + observed + " outside [" + (EXPECTED_WIDE_RATE - WIDE_RATE_TOLERANCE)
                   + ", " + (EXPECTED_WIDE_RATE + WIDE_RATE_TOLERANCE) + "]",
                   Math.abs(observed - EXPECTED_WIDE_RATE) <= WIDE_RATE_TOLERANCE);
    }

    @Test
    public void wideSchemaHasAtLeast64RegularColumns()
    {
        // Find a seed that rolls WIDE and assert its regular-column count is in band.
        // We try up to 200 seeds; with 10% probability we'd expect ~20 hits in 200
        // tries, so the test trivially completes unless the probability calibration
        // is severely broken.
        //
        // Note: we MUST mix the iteration counter into the seed via the golden-ratio
        // constant rather than just adding it. Adjacent seeds share most of their
        // 48 bits, and Java's Random masks the seed to 48 bits in its constructor —
        // so {@code new Random(seed).nextDouble()} produces highly correlated first
        // doubles across {@code seed, seed+1, seed+2, ...}. Stirring with the
        // golden ratio decorrelates successive seeds.
        boolean foundWide = false;
        for (int i = 0; i < 200 && !foundWide; i++)
        {
            long seed = 0xBEEFCAFE12340000L ^ (long) i * 0x9E3779B97F4A7C15L;
            GeneratedSchema schema = SchemaGenerator.generate(seed);
            if (schema.getShape() != SchemaShape.WIDE)
                continue;
            foundWide = true;
            assertTrue("wide schema must have ≥64 regular columns: got "
                       + schema.getRegularColumns().size(),
                       schema.getRegularColumns().size() >= 64);
            assertTrue("wide schema must have ≤200 regular columns: got "
                       + schema.getRegularColumns().size(),
                       schema.getRegularColumns().size() <= 200);
            assertTrue("wide schema's NULL fraction must be ≥0.70 to drive sparse encoding: "
                       + schema.getNullFraction(),
                       schema.getNullFraction() >= 0.70);
            assertTrue("wide schema's NULL fraction must be ≤0.95: " + schema.getNullFraction(),
                       schema.getNullFraction() <= 0.95);
        }
        assertTrue("expected to roll at least one WIDE schema across 200 seeds", foundWide);
    }

    @Test
    public void narrowSchemaHasReasonableColumnCount()
    {
        // Narrow schemas keep the historical 3-8 regular column band so the dense
        // common-path encoding stays exercised.
        boolean foundNarrow = false;
        for (int i = 0; i < 50 && !foundNarrow; i++)
        {
            // Same golden-ratio stir as wideSchemaHasAtLeast64RegularColumns —
            // see that test's comment for why nearby seeds correlate badly.
            long seed = 0x12345678BABEFACEL ^ (long) i * 0x9E3779B97F4A7C15L;
            GeneratedSchema schema = SchemaGenerator.generate(seed);
            if (schema.getShape() != SchemaShape.NARROW)
                continue;
            foundNarrow = true;
            assertTrue("narrow schema must have ≥3 regular columns: got "
                       + schema.getRegularColumns().size(),
                       schema.getRegularColumns().size() >= 3);
            assertTrue("narrow schema must have ≤8 regular columns: got "
                       + schema.getRegularColumns().size(),
                       schema.getRegularColumns().size() <= 8);
            assertTrue("narrow schema's NULL fraction must be in [0.05, 0.50]: "
                       + schema.getNullFraction(),
                       schema.getNullFraction() >= 0.05 && schema.getNullFraction() <= 0.50);
        }
        assertTrue("expected to roll at least one NARROW schema across 50 seeds", foundNarrow);
    }

    @Test
    public void shapeIsDeterministicForSameSeed()
    {
        for (long seed : new long[] { 0xAAAAL, 0xBBBBL, 0xCCCCL, 0xDDDDL })
        {
            GeneratedSchema a = SchemaGenerator.generate(seed);
            GeneratedSchema b = SchemaGenerator.generate(seed);
            assertEquals("same seed must roll same shape: seed=" + Long.toHexString(seed),
                         a.getShape(), b.getShape());
            assertEquals("same seed must roll same NULL fraction: seed=" + Long.toHexString(seed),
                         a.getNullFraction(), b.getNullFraction(), 1e-12);
            assertEquals("same seed must produce same regular column count",
                         a.getRegularColumns().size(), b.getRegularColumns().size());
            assertEquals("same seed must roll same gc_grace_seconds",
                         a.getGcGraceSeconds(), b.getGcGraceSeconds());
        }
    }

    @Test
    public void gcGraceSecondsIsOneOfTheRotatedValues()
    {
        // Every rolled schema must use one of the four canonical GCGS values from
        // the rotation table — no value should leak through that we didn't choose.
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        allowed.add(0);
        allowed.add(3_600);
        allowed.add(86_400);
        allowed.add(864_000);

        java.util.Map<Integer, Integer> hits = new java.util.HashMap<>();
        for (int allow : allowed) hits.put(allow, 0);

        int samples = 1000;
        for (int i = 0; i < samples; i++)
        {
            // Stir with golden ratio so adjacent iterations get decorrelated seeds —
            // see WideSchemaTest::wideSchemaHasAtLeast64RegularColumns for why.
            long seed = 0xACAB1F00DCAFE000L ^ (long) i * 0x9E3779B97F4A7C15L;
            int gcs = SchemaGenerator.generate(seed).getGcGraceSeconds();
            assertTrue("rolled GCGS=" + gcs + " not in allowed set " + allowed,
                       allowed.contains(gcs));
            hits.merge(gcs, 1, Integer::sum);
        }

        // Every value should be hit at least a handful of times across 1000 seeds —
        // even the rarest band (10% = ~100 hits) trivially clears 5.
        for (int allow : allowed)
            assertTrue("GCGS=" + allow + " never rolled across " + samples + " seeds (hits=" + hits + ")",
                       hits.get(allow) > 5);
    }

    @Test
    public void gcGraceSecondsAppearsInAssembledCql()
    {
        // The generator must always emit the option in the CQL. Cassandra falls back
        // to its default if the option is omitted; we want every test run's GCGS to
        // be loud-and-clear in the CREATE TABLE statement so post-mortem reads of
        // the run log can see the exact value used.
        for (long seed : new long[] { 0x1L, 0x2L, 0x3L, 0x4L, 0xFEEDL })
        {
            GeneratedSchema schema = SchemaGenerator.generate(seed);
            String cql = schema.getCql();
            String expected = "gc_grace_seconds = " + schema.getGcGraceSeconds();
            assertTrue("CQL must contain '" + expected + "' but was:\n" + cql,
                       cql.contains(expected));
        }
    }
}
