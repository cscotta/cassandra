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
package org.apache.cassandra.io.uring.cassandra;

import java.util.SortedMap;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.MetricNameFactory;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit test for {@link CassandraUringMetrics}: the SPI hooks update the registered meters/timer/gauge, and
 * {@link CassandraUringMetrics#release()} deregisters them. Requires no io_uring / kernel; JDK-25-only by package.
 */
public class CassandraUringMetricsTest
{
    @BeforeClass
    public static void setup()
    {
        DatabaseDescriptor.daemonInitialization();
    }

    @Test
    public void hooksUpdateRegisteredMetricsAndReleaseDeregisters()
    {
        MetricNameFactory factory = new DefaultNameFactory(CassandraUringMetrics.TYPE_NAME);
        CassandraMetricsRegistry.MetricName submissions = factory.createMetricName("Submissions");
        CassandraMetricsRegistry.MetricName completions = factory.createMetricName("Completions");
        CassandraMetricsRegistry.MetricName latency = factory.createMetricName("CompletionLatency");
        CassandraMetricsRegistry.MetricName fallbacks = factory.createMetricName("Fallbacks");
        CassandraMetricsRegistry.MetricName overflows = factory.createMetricName("Overflows");
        CassandraMetricsRegistry.MetricName inflight = factory.createMetricName("Inflight");

        CassandraUringMetrics metrics = new CassandraUringMetrics();
        try
        {
            metrics.onSubmit();
            metrics.onSubmit();
            metrics.onComplete(1_000L);
            metrics.onComplete(3_000L);
            metrics.onComplete(5_000L);
            metrics.onFallback("probe");
            metrics.onOverflow();
            metrics.inflightGauge(7L);

            SortedMap<String, Meter> meters = Metrics.getMeters();
            assertEquals(2, meters.get(submissions.getMetricName()).getCount());
            assertEquals(3, meters.get(completions.getMetricName()).getCount());
            assertEquals(1, meters.get(fallbacks.getMetricName()).getCount());
            assertEquals(1, meters.get(overflows.getMetricName()).getCount());

            SortedMap<String, Timer> timers = Metrics.getTimers();
            assertEquals(3, timers.get(latency.getMetricName()).getCount());

            Gauge<?> inflightGauge = Metrics.getGauges().get(inflight.getMetricName());
            assertEquals(Long.valueOf(7L), inflightGauge.getValue());
        }
        finally
        {
            metrics.release();
        }

        assertFalse(Metrics.getMeters().containsKey(submissions.getMetricName()));
        assertFalse(Metrics.getMeters().containsKey(completions.getMetricName()));
        assertFalse(Metrics.getTimers().containsKey(latency.getMetricName()));
        assertFalse(Metrics.getMeters().containsKey(fallbacks.getMetricName()));
        assertFalse(Metrics.getMeters().containsKey(overflows.getMetricName()));
        assertFalse(Metrics.getGauges().containsKey(inflight.getMetricName()));
    }
}
