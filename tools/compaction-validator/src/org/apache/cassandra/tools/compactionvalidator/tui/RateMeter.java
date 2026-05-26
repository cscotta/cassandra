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
package org.apache.cassandra.tools.compactionvalidator.tui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding-window rate meter used by the panels to display moving averages
 * (e.g. partitions/sec, bytes/sec) without long-tail influence from earlier
 * phases of a run.
 *
 * <p>The window is a wall-clock duration in milliseconds.  Each call to
 * {@link #record(long)} timestamps the recorded count with the current
 * monotonic clock; events older than the window are evicted on read.
 *
 * <p>Thread-safe.  Designed to be cheap enough to update from a hot path.
 */
public class RateMeter
{
    private static final class Sample
    {
        final long nanos;
        final long count;

        Sample(long nanos, long count)
        {
            this.nanos = nanos;
            this.count = count;
        }
    }

    private final long windowNanos;

    // Guarded by `this`
    private final Deque<Sample> samples = new ArrayDeque<>();
    private long total = 0L;

    /**
     * @param windowMillis size of the sliding window in milliseconds (must be positive)
     */
    public RateMeter(long windowMillis)
    {
        if (windowMillis <= 0)
            throw new IllegalArgumentException("windowMillis must be positive: " + windowMillis);
        this.windowNanos = windowMillis * 1_000_000L;
    }

    /**
     * Records {@code count} items observed at the current monotonic instant.
     *
     * @param count number of items observed (must be ≥ 0)
     */
    public synchronized void record(long count)
    {
        if (count < 0)
            throw new IllegalArgumentException("count must be non-negative: " + count);
        long now = System.nanoTime();
        samples.addLast(new Sample(now, count));
        total += count;
        evictOldLocked(now);
    }

    /**
     * Returns the current rate in items per second, computed over the current
     * sliding window.  Returns {@code 0.0} until at least two samples have
     * been recorded.
     */
    public synchronized double ratePerSecond()
    {
        long now = System.nanoTime();
        evictOldLocked(now);
        if (samples.isEmpty())
            return 0.0;

        long windowSpanNanos = now - samples.peekFirst().nanos;
        if (windowSpanNanos <= 0)
            return 0.0;

        // Use full window if we've been running long enough; otherwise the
        // span between earliest sample and now.
        long denominatorNanos = Math.min(windowSpanNanos, windowNanos);
        if (denominatorNanos <= 0)
            return 0.0;

        return total / (denominatorNanos / 1_000_000_000.0);
    }

    /** Resets the meter, discarding all samples. */
    public synchronized void reset()
    {
        samples.clear();
        total = 0L;
    }

    private void evictOldLocked(long now)
    {
        long cutoff = now - windowNanos;
        while (!samples.isEmpty() && samples.peekFirst().nanos < cutoff)
        {
            Sample s = samples.removeFirst();
            total -= s.count;
            if (total < 0)
                total = 0; // defensive
        }
    }
}
