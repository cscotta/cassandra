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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;

import org.apache.cassandra.io.uring.spi.UringStatsListener;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.SnapshottingTimer;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * The Cassandra-side {@link UringStatsListener}: forwards the ring's hot-path callbacks into
 * {@link CassandraMetricsRegistry}, which exposes them over JMX under the {@code IoUring} metric type. Every hook is a
 * cheap, non-throwing meter/timer/gauge update, honouring the SPI contract that listeners run on the poller thread and
 * must not block or throw.
 *
 * <p>Metrics: {@code Submissions}/{@code Completions}/{@code Fallbacks}/{@code Overflows} (meters), {@code
 * CompletionLatency} (a timer of submit&rarr;completion latency), and {@code Inflight} (a gauge of ops submitted to but
 * not yet reaped by the kernel, bridged from the push-style {@link #inflightGauge} callback via an {@link AtomicLong}).
 * {@link #release()} deregisters them so a shutdown/re-open cycle does not leak MBeans.
 */
public final class CassandraUringMetrics implements UringStatsListener
{
    public static final String TYPE_NAME = "IoUring";

    private final CassandraMetricsRegistry.MetricName submissionsName;
    private final CassandraMetricsRegistry.MetricName completionsName;
    private final CassandraMetricsRegistry.MetricName completionLatencyName;
    private final CassandraMetricsRegistry.MetricName fallbacksName;
    private final CassandraMetricsRegistry.MetricName overflowsName;
    private final CassandraMetricsRegistry.MetricName inflightName;

    private final Meter submissions;
    private final Meter completions;
    private final SnapshottingTimer completionLatency;
    private final Meter fallbacks;
    private final Meter overflows;
    private final AtomicLong inflight = new AtomicLong();

    public CassandraUringMetrics()
    {
        MetricNameFactory factory = new DefaultNameFactory(TYPE_NAME);
        submissionsName = factory.createMetricName("Submissions");
        completionsName = factory.createMetricName("Completions");
        completionLatencyName = factory.createMetricName("CompletionLatency");
        fallbacksName = factory.createMetricName("Fallbacks");
        overflowsName = factory.createMetricName("Overflows");
        inflightName = factory.createMetricName("Inflight");

        submissions = Metrics.meter(submissionsName);
        completions = Metrics.meter(completionsName);
        completionLatency = Metrics.timer(completionLatencyName);
        fallbacks = Metrics.meter(fallbacksName);
        overflows = Metrics.meter(overflowsName);
        Metrics.register(inflightName, (Gauge<Long>) inflight::get);
    }

    @Override
    public void onSubmit()
    {
        submissions.mark();
    }

    @Override
    public void onComplete(long latencyNanos)
    {
        completions.mark();
        completionLatency.update(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void onFallback(String reason)
    {
        fallbacks.mark();
    }

    @Override
    public void inflightGauge(long inflight)
    {
        this.inflight.set(inflight);
    }

    @Override
    public void onOverflow()
    {
        overflows.mark();
    }

    /** Deregisters every metric (and its JMX MBean), so a later re-open re-registers cleanly rather than leaking. */
    public void release()
    {
        Metrics.remove(submissionsName);
        Metrics.remove(completionsName);
        Metrics.remove(completionLatencyName);
        Metrics.remove(fallbacksName);
        Metrics.remove(overflowsName);
        Metrics.remove(inflightName);
    }
}
