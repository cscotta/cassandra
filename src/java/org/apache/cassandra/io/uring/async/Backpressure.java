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
package org.apache.cassandra.io.uring.async;

import org.apache.cassandra.io.uring.spi.PermitGate;

/**
 * Bounds the number of logical ops between {@code submit} and completion to the ring's queue depth (§9 H4, §10). A
 * foreign submitter acquires one permit <em>before</em> its op is enqueued and the poller releases it when the op's
 * terminal CQE is reaped. Because the ring's SQ is sized to {@code queueDepth + 1} (the {@code +1} reserved for the
 * eventfd-wakeup op) and its CQ to at least twice the SQ, capping in-flight ops at {@code queueDepth} makes CQ overflow
 * impossible in steady state.
 *
 * <p>Delegates to the {@link PermitGate} SPI so the Cassandra adapter can substitute a fair/interruptible gate.
 */
final class Backpressure
{
    private final PermitGate gate;

    Backpressure(PermitGate gate)
    {
        this.gate = gate;
    }

    /** Block (on a foreign submitter thread, never the poller) until a permit is free, then take it. */
    void acquire()
    {
        gate.acquire();
    }

    /** Release one permit; called by the poller when an op reaches its terminal CQE. */
    void release()
    {
        gate.release();
    }

    int available()
    {
        return gate.availablePermits();
    }
}
