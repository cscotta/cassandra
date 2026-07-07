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
package org.apache.cassandra.io.uring.spi;

/**
 * SPI seam for observability. The ring core calls these hooks on the submission/completion path; the default
 * {@link #NOOP} implementation does nothing, so metrics collection is entirely opt-in and adds no cost when absent.
 * The Cassandra adapter is expected to supply an implementation that wires these into its metrics registry.
 *
 * <p>Implementations must be cheap and non-blocking: they are invoked from the ring's hot path, potentially from the
 * poller thread, and must not throw.
 */
public interface UringStatsListener
{
    /** Called once for each SQE handed to the kernel via {@code io_uring_enter}. */
    void onSubmit();

    /** Called once per reaped completion, with the wall-clock latency from submit to completion in nanoseconds. */
    void onComplete(long latencyNanos);

    /** Called when an operation cannot use the ring and is served by the blocking fallback path instead. */
    void onFallback(String reason);

    /** Reports the current number of operations submitted to but not yet completed by the kernel. */
    void inflightGauge(long inflight);

    /** Called when the completion queue overflowed (a CQE was dropped by the kernel). */
    void onOverflow();

    /** Shared no-op listener used when the caller does not care about statistics. */
    UringStatsListener NOOP = new Noop();

    /** The {@link #NOOP} implementation: every hook is an empty method. */
    final class Noop implements UringStatsListener
    {
        @Override
        public void onSubmit()
        {
        }

        @Override
        public void onComplete(long latencyNanos)
        {
        }

        @Override
        public void onFallback(String reason)
        {
        }

        @Override
        public void inflightGauge(long inflight)
        {
        }

        @Override
        public void onOverflow()
        {
        }
    }
}
