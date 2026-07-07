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

import java.util.function.Consumer;

import org.agrona.collections.Long2ObjectHashMap;

/**
 * The in-flight op table and the library's central memory-safety mechanism (§9 H1/H5). Every submitted op is keyed by a
 * <b>monotonic, never-recycled 64-bit</b> {@code user_data} token; the entry owns a strong reference to the op's I/O
 * buffer, so nothing frees or reuses the buffer until the terminal CQE removes the entry. Because tokens are never
 * recycled, a late or duplicate CQE can never be misrouted to a different op (H5).
 *
 * <p><b>Single-threaded by contract:</b> only the ring's poller thread touches this map (foreign submitters hand ops
 * off via the event loop's MPSC queue), so it needs no synchronization. {@link #isEmpty()} is the drained-empty gate
 * that {@link IoUringEventLoop} waits on before tearing the ring down (H10).
 *
 * <p>The token counter starts at 1 and increments; {@code Long.MIN_VALUE} is reserved (never handed out here) for the
 * event loop's eventfd-wakeup op so its completions are trivially distinguishable from real ops.
 */
final class InflightRegistry
{
    private final Long2ObjectHashMap<PendingOp> inflight = new Long2ObjectHashMap<>();
    private long nextToken = 1L;

    /** Assign the next monotonic token to {@code op}, record it, and return the token. */
    long register(PendingOp op)
    {
        long token = nextToken++;
        op.userData = token;
        inflight.put(token, op);
        return token;
    }

    /** Look up an in-flight op by its token, or {@code null} if not present (stale/duplicate CQE). */
    PendingOp get(long userData)
    {
        return inflight.get(userData);
    }

    /** Remove and return the op for {@code userData} once its terminal CQE has been reaped, or {@code null}. */
    PendingOp remove(long userData)
    {
        return inflight.remove(userData);
    }

    int size()
    {
        return inflight.size();
    }

    boolean isEmpty()
    {
        return inflight.isEmpty();
    }

    /**
     * Apply {@code action} to every remaining op &mdash; used only on abnormal teardown, where the caller both fails
     * the op's future and releases its backpressure permit &mdash; then clear the table.
     */
    void failAll(Consumer<PendingOp> action)
    {
        for (PendingOp op : inflight.values())
            action.accept(op);
        inflight.clear();
    }
}
