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
 * Tracks a throughput rate using an exponential moving average (EMA).
 *
 * <p>Thread-safe: {@link #record} and {@link #ratePerSecond} may be called from any thread.
 *
 * <p>The EMA smoothing factor {@code alpha} is derived from the window size:
 * {@code alpha = 1.0 - exp(-2.0 / windowSeconds)}.  Smaller windows react faster to bursts;
 * larger windows produce smoother long-term averages.
 */
public class RateTracker
{
    private final double alpha;

    // All three guarded by synchronized(this)
    private double emaRate = 0.0;
    private long lastNanos = 0L;
    private boolean initialized = false;

    /**
     * Creates a new {@code RateTracker}.
     *
     * @param windowSeconds smoothing window in seconds (must be positive).  A value of {@code 5}
     *                      is appropriate for short-term display; {@code 60} for long-term trends.
     */
    public RateTracker(int windowSeconds)
    {
        if (windowSeconds <= 0)
            throw new IllegalArgumentException("windowSeconds must be positive, got: " + windowSeconds);
        // alpha calculated so that events outside `windowSeconds` have weight < ~14 %
        this.alpha = 1.0 - Math.exp(-2.0 / windowSeconds);
    }

    /**
     * Records that {@code count} items (bytes, rows, etc.) were observed at the current wall-clock
     * time and updates the running EMA rate.
     *
     * @param count number of items observed (must be ≥ 0)
     */
    public synchronized void record(long count)
    {
        if (count < 0)
            throw new IllegalArgumentException("count must be non-negative, got: " + count);

        long nowNanos = System.nanoTime();

        if (!initialized)
        {
            emaRate = 0.0;
            lastNanos = nowNanos;
            initialized = true;
            // Don't update EMA on the very first observation — we have no elapsed time yet.
            return;
        }

        long elapsedNanos = nowNanos - lastNanos;
        if (elapsedNanos <= 0)
        {
            // Two calls within the same nanosecond tick; add to the running rate without resetting.
            return;
        }

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double instantRate = count / elapsedSeconds;

        // Exponential moving average update
        emaRate = alpha * instantRate + (1.0 - alpha) * emaRate;
        lastNanos = nowNanos;
    }

    /**
     * Returns the current EMA-smoothed throughput rate in items per second.
     *
     * <p>Returns {@code 0.0} until at least one call to {@link #record} has been made.
     *
     * @return items per second (non-negative)
     */
    public synchronized double ratePerSecond()
    {
        return emaRate;
    }
}
