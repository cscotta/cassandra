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

import java.lang.ref.Reference;
import java.util.function.Consumer;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.abi.CqeFlags;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.ring.CqeConsumer;
import org.apache.cassandra.io.uring.spi.UringLogger;
import org.apache.cassandra.io.uring.spi.UringStatsListener;

/**
 * Reaps and decodes completions on the poller thread, translating the CQE {@code res} contract into future outcomes
 * (§5.5, §9 H9):
 * <ul>
 *   <li>{@code res < 0} &mdash; {@code -errno}: fail the future with an {@link IoUringException}.</li>
 *   <li>{@code res == 0} &mdash; EOF (read), success (fsync), or zero transfer: complete with the bytes done so far.</li>
 *   <li>{@code 0 < res < remaining} &mdash; short read/write: advance the cursor and resubmit the remainder (the op
 *       stays in the registry and keeps its permit; only one SQE per token is ever in flight).</li>
 *   <li>{@code res == remaining} &mdash; complete with the full byte count.</li>
 * </ul>
 * On a terminal outcome it removes the registry entry (releasing the buffer keep-alive), releases the backpressure
 * permit, records latency, completes the future, and then issues a {@link Reference#reachabilityFence} on the buffer
 * as belt-and-braces against the JIT treating it as dead before this point (§6.5). The eventfd-wakeup completion
 * (token {@code wakeupToken}) is routed to {@code onWakeup} and never touches the registry.
 */
final class CompletionDispatcher implements CqeConsumer
{
    private final InflightRegistry registry;
    private final Backpressure backpressure;
    private final UringStatsListener stats;
    private final UringLogger log;
    private final Consumer<PendingOp> resubmitSink;
    private final Runnable onWakeup;
    private final long wakeupToken;

    CompletionDispatcher(InflightRegistry registry, Backpressure backpressure, UringStatsListener stats,
                         UringLogger log, Consumer<PendingOp> resubmitSink, Runnable onWakeup, long wakeupToken)
    {
        this.registry = registry;
        this.backpressure = backpressure;
        this.stats = stats;
        this.log = log;
        this.resubmitSink = resubmitSink;
        this.onWakeup = onWakeup;
        this.wakeupToken = wakeupToken;
    }

    @Override
    public void accept(long userData, int res, int flags)
    {
        if (userData == wakeupToken)
        {
            onWakeup.run();   // eventfd fired; the loop will re-arm. Not a registry op.
            return;
        }
        if ((flags & CqeFlags.MORE) != 0)
            return;           // multishot: more CQEs coming, do not retire (unused by storage ops, handled defensively)

        PendingOp op = registry.get(userData);
        if (op == null)
        {
            log.warn("io_uring: stray completion user_data={} res={} (already retired?)", userData, res);
            return;
        }
        try
        {
            if (res < 0)
                fail(op, -res);
            else if (res == 0)
                complete(op, op.bytesDone);           // EOF / fsync success / zero transfer
            else
            {
                op.advance(res);
                if (op.remaining == 0)
                    complete(op, op.bytesDone);
                else
                    resubmitSink.accept(op);          // short read/write: keep entry + permit, resubmit remainder
            }
        }
        catch (Throwable t)
        {
            registry.remove(userData);
            backpressure.release();
            op.future.completeExceptionally(new IoUringException("io_uring completion dispatch failed", t));
        }
    }

    private void complete(PendingOp op, int bytes)
    {
        registry.remove(op.userData);
        stats.onComplete(System.nanoTime() - op.submitNanos); // checkstyle: permit system clock
        backpressure.release();
        try
        {
            op.future.complete(bytes);
        }
        finally
        {
            Reference.reachabilityFence(op.buffer);
        }
    }

    private void fail(PendingOp op, int errno)
    {
        registry.remove(op.userData);
        stats.onComplete(System.nanoTime() - op.submitNanos); // checkstyle: permit system clock
        backpressure.release();
        try
        {
            String what = errno == Errno.ECANCELED ? "io_uring op cancelled" : "io_uring op failed";
            op.future.completeExceptionally(new IoUringException(what, errno));
        }
        finally
        {
            Reference.reachabilityFence(op.buffer);
        }
    }
}
