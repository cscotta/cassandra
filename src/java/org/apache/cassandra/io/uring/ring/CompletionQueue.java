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

import org.apache.cassandra.io.uring.abi.Cqe;
import org.apache.cassandra.io.uring.abi.Layouts;

/**
 * The completion side of one ring. <b>Single-consumer, single-threaded</b>: all methods must run on the ring's
 * owning thread. The CQ {@code tail} is loaded with acquire (making the kernel's CQE writes visible), each ready
 * CQE is decoded and dispatched, and the CQ {@code head} is advanced with a release store (freeing the slots only
 * after they have been read) &mdash; via {@link RingBarriers}.
 */
public final class CompletionQueue
{
    private final MemorySegment cqRing;   // CQ ring mmap (shared with SQ under SINGLE_MMAP)
    private final long headOff;           // byte offset of the app-owned head
    private final long tailOff;           // byte offset of the kernel-owned tail
    private final long cqesOff;           // byte offset of the cqe[] array within cqRing
    private final int ringEntries;
    private final int ringMask;

    private int cqHead;                   // app-private mirror of the CQ head

    CompletionQueue(MemorySegment cqRing, long headOff, long tailOff, long cqesOff, int ringEntries, int ringMask)
    {
        this.cqRing = cqRing;
        this.headOff = headOff;
        this.tailOff = tailOff;
        this.cqesOff = cqesOff;
        this.ringEntries = ringEntries;
        this.ringMask = ringMask;
        this.cqHead = RingBarriers.loadPlain(cqRing, headOff);
    }

    /** Number of CQEs ready to reap right now (kernel tail minus our head). */
    public int cqReady()
    {
        return RingBarriers.loadCqTail(cqRing, tailOff) - cqHead;
    }

    /**
     * Reaps up to {@code max} ready CQEs, invoking {@code consumer} for each in order, then advances the CQ head with
     * a single release store. The consumer runs before the head is released, so it may safely read the CQE it is
     * handed. Returns the number of CQEs consumed.
     */
    public int forEachCqe(CqeConsumer consumer, int max)
    {
        int tail = RingBarriers.loadCqTail(cqRing, tailOff);
        int head = cqHead;
        int n = 0;
        while (head != tail && n < max)
        {
            long slot = head & ringMask;
            MemorySegment cqe = cqRing.asSlice(cqesOff + slot * Layouts.CQE_BYTES, Layouts.CQE_BYTES);
            consumer.accept(Cqe.userData(cqe), Cqe.res(cqe), Cqe.flags(cqe));
            head++;
            n++;
        }
        if (n > 0)
        {
            RingBarriers.advanceCqHead(cqRing, headOff, head);
            cqHead = head;
        }
        return n;
    }

    /**
     * Returns a 16-byte slice of the head CQE without consuming it, or {@code null} if none are ready. Pair with
     * {@link #advance(int)} after reading. Prefer {@link #forEachCqe} for the common batch-reap path.
     */
    public MemorySegment peekCqe()
    {
        int tail = RingBarriers.loadCqTail(cqRing, tailOff);
        if (cqHead == tail)
            return null;
        long slot = cqHead & ringMask;
        return cqRing.asSlice(cqesOff + slot * Layouts.CQE_BYTES, Layouts.CQE_BYTES);
    }

    /** Advances the CQ head by {@code n} consumed CQEs (release store). */
    public void advance(int n)
    {
        if (n <= 0)
            return;
        cqHead += n;
        RingBarriers.advanceCqHead(cqRing, headOff, cqHead);
    }

    public int ringEntries()
    {
        return ringEntries;
    }
}
