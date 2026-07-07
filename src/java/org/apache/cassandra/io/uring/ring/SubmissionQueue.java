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

import org.apache.cassandra.io.uring.abi.EnterFlags;
import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.SetupFlags;
import org.apache.cassandra.io.uring.abi.SqCqFlags;

/**
 * The submission side of one ring. <b>Single-producer, single-threaded</b>: all methods must run on the ring's
 * owning thread. Reproduces liburing's {@code get_sqe} / {@code __io_uring_flush_sq} / {@code sq_ring_needs_enter}
 * exactly (see the internal-contract research report), with the SQ {@code tail} published through
 * {@link RingBarriers#publishSqTail} as the single ordering point.
 */
public final class SubmissionQueue
{
    private final MemorySegment sqRing;   // SQ ring mmap (shared with CQ under SINGLE_MMAP)
    private final MemorySegment sqes;     // SQE array mmap
    private final long headOff;           // byte offset into sqRing of the kernel-owned head
    private final long tailOff;           // byte offset of the app-owned tail
    private final long flagsOff;          // byte offset of the SQ flags word
    private final long arrayOff;          // byte offset of the sq_array[] index table
    private final int ringEntries;
    private final int ringMask;
    private final boolean sqpoll;

    // App-private cursors (not shared with the kernel).
    private int sqeHead;                  // next SQE to flush
    private int sqeTail;                  // next SQE to allocate
    private int ktailLocal;               // mirror of the published SQ tail (we are the sole writer)

    // Set by sqRingNeedsEnter() when an SQPOLL poll thread must be woken; consumed by the submitter.
    private boolean wakeupNeeded;

    SubmissionQueue(MemorySegment sqRing, MemorySegment sqes,
                    long headOff, long tailOff, long flagsOff, long arrayOff,
                    int ringEntries, int ringMask, int setupFlags)
    {
        this.sqRing = sqRing;
        this.sqes = sqes;
        this.headOff = headOff;
        this.tailOff = tailOff;
        this.flagsOff = flagsOff;
        this.arrayOff = arrayOff;
        this.ringEntries = ringEntries;
        this.ringMask = ringMask;
        this.sqpoll = (setupFlags & SetupFlags.SQPOLL) != 0;
        this.ktailLocal = RingBarriers.loadPlain(sqRing, tailOff);
        this.sqeHead = ktailLocal;
        this.sqeTail = ktailLocal;
    }

    /**
     * Returns a 64-byte slice for the next SQE, or {@code null} if the ring is full (the kernel has not yet consumed
     * enough SQEs). The capacity check is against the kernel-owned SQ head (acquire) so a slot the kernel still owns
     * is never reused. The returned slice's contents are stale and must be {@code clear}ed before filling.
     */
    public MemorySegment getSqe()
    {
        int khead = RingBarriers.loadSqHead(sqRing, headOff);
        if ((sqeTail - khead) >= ringEntries)   // int subtraction is wrap-safe for these bounded counters
            return null;
        int slot = sqeTail & ringMask;
        MemorySegment sqe = sqes.asSlice((long) slot * Layouts.SQE_BYTES, Layouts.SQE_BYTES);
        sqeTail++;
        return sqe;
    }

    /**
     * Fills the sq_array index table for every prepared-but-unflushed SQE and publishes the new SQ tail with a
     * release store (the single point that makes the SQE and array writes visible to the kernel). Returns the number
     * of SQEs handed to the kernel by this flush.
     */
    public int flushSq()
    {
        int toSubmit = sqeTail - sqeHead;
        if (toSubmit == 0)
            return 0;
        int ktail = ktailLocal;
        for (int i = 0; i < toSubmit; i++)
        {
            RingBarriers.storePlain(sqRing, arrayOff + ((long) (ktail & ringMask)) * Integer.BYTES, sqeHead & ringMask);
            ktail++;
            sqeHead++;
        }
        RingBarriers.publishSqTail(sqRing, tailOff, ktail);
        ktailLocal = ktail;
        return toSubmit;
    }

    /**
     * Whether an {@code io_uring_enter} is required to hand {@code submitted} SQEs to the kernel. Non-SQPOLL always
     * enters. Under SQPOLL, enters only if the poll thread has parked ({@link SqCqFlags#NEED_WAKEUP}); when it has,
     * records that {@link EnterFlags#SQ_WAKEUP} must be OR'd into the enter flags.
     */
    public boolean sqRingNeedsEnter(int submitted)
    {
        wakeupNeeded = false;
        if (!sqpoll)
            return submitted > 0;
        RingBarriers.acquireFence();
        int flags = RingBarriers.loadPlain(sqRing, flagsOff);
        if ((flags & SqCqFlags.NEED_WAKEUP) != 0)
        {
            wakeupNeeded = true;
            return true;
        }
        return submitted > 0;
    }

    /** The extra enter flags implied by the most recent {@link #sqRingNeedsEnter(int)} call. */
    public int extraEnterFlags()
    {
        return wakeupNeeded ? EnterFlags.SQ_WAKEUP : 0;
    }

    /** SQEs prepared/flushed but not yet consumed by the kernel. */
    public int sqReady()
    {
        return sqeTail - RingBarriers.loadSqHead(sqRing, headOff);
    }

    /** Free SQE slots available to {@link #getSqe()} right now. */
    public int sqSpaceLeft()
    {
        return ringEntries - sqReady();
    }

    public int ringEntries()
    {
        return ringEntries;
    }
}
