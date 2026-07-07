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
package org.apache.cassandra.io.uring.ring;

import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.abi.EnterFlags;
import org.apache.cassandra.io.uring.abi.SqCqFlags;
import org.apache.cassandra.io.uring.linux.Errno;

/**
 * Composes {@link SubmissionQueue#flushSq()} with {@code io_uring_enter}, mirroring liburing's
 * {@code __io_uring_submit}: flush the SQ, decide whether an enter is required, OR in
 * {@link EnterFlags#GETEVENTS} when waiting or when the kernel has backlogged CQ overflow, then enter with
 * EINTR/EAGAIN/EBUSY handling. All calls must run on the ring's owning thread.
 */
public final class RingSubmitter
{
    /** Safety cap on enter retries so a pathological errno loop cannot spin forever. */
    private static final int MAX_ENTER_ATTEMPTS = 1 << 20;

    private RingSubmitter() {}

    /** Submit all prepared SQEs without waiting for completions. Returns the number the kernel consumed. */
    public static int submit(RingHandle ring)
    {
        return enterLoop(ring, 0, false);
    }

    /**
     * Submit all prepared SQEs and block until at least {@code waitNr} completions are available (then reap them via
     * {@link CompletionQueue}). Returns the number of SQEs the kernel consumed.
     */
    public static int submitAndWait(RingHandle ring, int waitNr)
    {
        return enterLoop(ring, waitNr, false);
    }

    /**
     * Submit any prepared SQEs and enter with {@code GETEVENTS} but {@code min_complete=0} &mdash; a <b>non-blocking</b>
     * flush that runs pending task-work and surfaces any completed CQEs without ever parking in the kernel. Used by the
     * teardown drain so shutdown cannot hang on a stuck in-flight op.
     */
    public static int submitAndReapNonBlocking(RingHandle ring)
    {
        return enterLoop(ring, 0, true);
    }

    private static int enterLoop(RingHandle ring, int waitNr, boolean forceGetEvents)
    {
        SubmissionQueue sq = ring.sq();
        int submitted = sq.flushSq();

        boolean needEnter = sq.sqRingNeedsEnter(submitted);
        int flags = sq.extraEnterFlags();
        if (waitNr > 0)
        {
            flags |= EnterFlags.GETEVENTS;
            needEnter = true;
        }
        if (forceGetEvents)
        {
            flags |= EnterFlags.GETEVENTS;   // min_complete stays waitNr(0): run task-work + flush ready CQEs, do not block
            needEnter = true;
        }
        if ((ring.sqFlags() & SqCqFlags.CQ_OVERFLOW) != 0)
        {
            flags |= EnterFlags.GETEVENTS;
            needEnter = true;
        }
        if (!needEnter)
            return submitted;

        int toSubmit = submitted;
        MemorySegment cap = ring.captureSegment();
        for (int attempt = 0; attempt < MAX_ENTER_ATTEMPTS; attempt++)
        {
            int ret = ring.enter(toSubmit, waitNr, flags);
            if (ret >= 0)
                return submitted;

            int errno = Errno.of(cap);
            switch (errno)
            {
                case Errno.EINTR:
                    // Interrupted before consuming; retry with the same submission.
                    break;
                case Errno.EAGAIN:
                    Thread.onSpinWait();
                    break;
                case Errno.EBUSY:
                    // CQ-overflow backlog / too many in flight: a GETEVENTS enter flushes it; do not resubmit.
                    flags |= EnterFlags.GETEVENTS;
                    toSubmit = 0;
                    break;
                case Errno.ETIME:
                    // A timeout wait fired: success, not an error.
                    return submitted;
                default:
                    throw new IoUringException("io_uring_enter failed", errno);
            }
        }
        throw new IoUringException("io_uring_enter did not make progress after " + MAX_ENTER_ATTEMPTS + " attempts");
    }
}
