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

import java.util.concurrent.Semaphore; // checkstyle: permit this import

/**
 * SPI seam for bounding the number of in-flight operations against a ring. The core acquires a permit before it hands
 * an SQE to the kernel and releases it when the matching completion is reaped, so the gate keeps submissions from
 * outrunning the completion queue's capacity. Factoring it out lets the Cassandra adapter substitute a fair, metered,
 * or interruptible gate without touching the core.
 */
public interface PermitGate
{
    /** Block until a permit is available, then take it. */
    void acquire();

    /** Take a permit if one is immediately available; return {@code true} iff a permit was taken. */
    boolean tryAcquire();

    /** Return a single permit previously taken via {@link #acquire()} or {@link #tryAcquire()}. */
    void release();

    /** The number of permits currently available; intended for gauges and diagnostics only. */
    int availablePermits();

    /**
     * Default implementation backed by a {@link Semaphore}. Note that {@link #acquire()} blocks
     * <em>uninterruptibly</em>: the permit is always taken before it returns, so a spurious interrupt cannot leave a
     * submission un-gated. A caller needing interruptible acquisition should supply its own {@link PermitGate}.
     */
    final class SemaphorePermitGate implements PermitGate
    {
        private final Semaphore semaphore;

        public SemaphorePermitGate(int permits)
        {
            this.semaphore = new Semaphore(permits); // checkstyle: permit this instantiation
        }

        @Override
        public void acquire()
        {
            semaphore.acquireUninterruptibly();
        }

        @Override
        public boolean tryAcquire()
        {
            return semaphore.tryAcquire();
        }

        @Override
        public void release()
        {
            semaphore.release();
        }

        @Override
        public int availablePermits()
        {
            return semaphore.availablePermits();
        }
    }
}
