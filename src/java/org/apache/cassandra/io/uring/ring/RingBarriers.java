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
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The <em>single</em> place acquire/release memory ordering is open-coded for the ring counters shared with the
 * kernel. These modes are the exact FFM equivalents of liburing's {@code io_uring_smp_load_acquire} /
 * {@code io_uring_smp_store_release} (verified on JDK 25.0.3) and generate precisely the barriers the shared-memory
 * protocol needs &mdash; without the full StoreLoad fence a {@code volatile} access would impose, which matters on
 * aarch64's weak memory model.
 *
 * <p>The four load-bearing points (get one wrong and you get silent corruption or lost completions):
 * <ul>
 *   <li>SQ {@code tail}: producer store after filling the SQE and sq_array &mdash; {@link #publishSqTail} (release).</li>
 *   <li>SQ {@code head}: kernel-owned, load to compute free space &mdash; {@link #loadSqHead} (acquire).</li>
 *   <li>CQ {@code tail}: kernel-owned, load to find new CQEs &mdash; {@link #loadCqTail} (acquire).</li>
 *   <li>CQ {@code head}: producer store after consuming CQEs &mdash; {@link #advanceCqHead} (release).</li>
 * </ul>
 * Never open-code any of these elsewhere. Our own cursors (that the kernel does not read) use {@link #loadPlain}.
 */
public final class RingBarriers
{
    /** One handle for all 32-bit ring counters; coordinates are {@code (MemorySegment, long byteOffset)}. */
    private static final VarHandle U32 = JAVA_INT.varHandle();

    private RingBarriers() {}

    /** {@code smp_store_release(&sq.tail, newTail)} &mdash; publishes SQE + sq_array writes before the kernel sees the tail. */
    public static void publishSqTail(MemorySegment sqRing, long tailOff, int newTail)
    {
        U32.setRelease(sqRing, tailOff, newTail);
    }

    /** {@code smp_load_acquire(&sq.head)} &mdash; observes the kernel's SQE consumption before we reuse slots. */
    public static int loadSqHead(MemorySegment sqRing, long headOff)
    {
        return (int) U32.getAcquire(sqRing, headOff);
    }

    /** {@code smp_load_acquire(&cq.tail)} &mdash; makes the kernel's CQE writes visible once we observe the tail. */
    public static int loadCqTail(MemorySegment cqRing, long tailOff)
    {
        return (int) U32.getAcquire(cqRing, tailOff);
    }

    /** {@code smp_store_release(&cq.head, newHead)} &mdash; frees CQ slots only after we have read them. */
    public static void advanceCqHead(MemorySegment cqRing, long headOff, int newHead)
    {
        U32.setRelease(cqRing, headOff, newHead);
    }

    /**
     * Plain load of a 32-bit ring word we own or read only diagnostically. Used for our own cached head/tail and,
     * after an {@link VarHandle#acquireFence()}, for the SQPOLL {@code kflags} read.
     */
    public static int loadPlain(MemorySegment ring, long off)
    {
        return (int) U32.get(ring, off);
    }

    /** Plain store of a 32-bit ring word we own (e.g. the sq_array index table, ordered before the tail release). */
    public static void storePlain(MemorySegment ring, long off, int value)
    {
        U32.set(ring, off, value);
    }

    /** Full acquire fence, required before reading the SQPOLL {@code kflags} word. */
    public static void acquireFence()
    {
        VarHandle.acquireFence();
    }
}
