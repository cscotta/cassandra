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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.abi.FeatureFlags;
import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.MmapOffsets;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.LibC;
import org.apache.cassandra.io.uring.linux.Syscalls;

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

/**
 * A live io_uring: the ring fd, the mmap'd SQ/CQ/SQE regions (two mmaps when {@code IORING_FEAT_SINGLE_MMAP} is
 * present &mdash; always true at the 5.6 kernel floor &mdash; otherwise three), and the {@link SubmissionQueue} /
 * {@link CompletionQueue} views over them. All ring memory is owned by one {@link Arena}; {@link #close()} closes
 * the ring fd and then closes the arena (running {@code munmap}), so it must only be called once the caller has
 * ensured no I/O is in flight against this ring's buffers.
 *
 * <p>This low-level {@code Ring} is single-threaded by contract: submit and reap must happen on the thread that
 * drives it. The higher-level async facade layers the event loop, in-flight registry and drain-before-close on top.
 */
public final class Ring implements RingHandle
{
    private final int fd;
    private final Arena arena;
    private final MemorySegment cap;      // reused errno capture segment for control syscalls on this ring
    private final MemorySegment sqRing;   // needed for the CQ-overflow flag read
    private final long sqFlagsOff;
    private final RingParams params;
    private final SubmissionQueue sq;
    private final CompletionQueue cq;

    private boolean closed;

    private Ring(int fd, Arena arena, MemorySegment cap, MemorySegment sqRing, long sqFlagsOff,
                 RingParams params, SubmissionQueue sq, CompletionQueue cq)
    {
        this.fd = fd;
        this.arena = arena;
        this.cap = cap;
        this.sqRing = sqRing;
        this.sqFlagsOff = sqFlagsOff;
        this.params = params;
        this.sq = sq;
        this.cq = cq;
    }

    /** Opens a ring with default (baseline) setup flags. */
    public static Ring open(int entries)
    {
        return open(entries, 0);
    }

    /**
     * Sets up a ring of {@code entries} SQEs with the given {@code IORING_SETUP_*} flags, maps its regions and reads
     * back the granted geometry and feature set. Throws {@link IoUringException} (carrying errno) on any failure.
     */
    public static Ring open(int entries, int setupFlags)
    {
        Arena arena = Arena.ofShared();
        int fd = -1;
        try
        {
            MemorySegment cap = arena.allocate(Errno.CAPTURE);
            MemorySegment paramsSeg = arena.allocate(Layouts.PARAMS);
            paramsSeg.set(JAVA_INT_UNALIGNED, Layouts.PARAMS_FLAGS_OFF, setupFlags);

            fd = Syscalls.setup(cap, entries, paramsSeg);
            if (fd < 0)
                throw new IoUringException("io_uring_setup failed", Errno.of(cap));

            int sqEntries = paramsSeg.get(JAVA_INT_UNALIGNED, Layouts.PARAMS_SQ_ENTRIES_OFF);
            int cqEntries = paramsSeg.get(JAVA_INT_UNALIGNED, Layouts.PARAMS_CQ_ENTRIES_OFF);
            int features = paramsSeg.get(JAVA_INT_UNALIGNED, Layouts.PARAMS_FEATURES_OFF);

            // sq_off/cq_off values are byte offsets into their respective ring mmaps.
            long sqHeadOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_HEAD);
            long sqTailOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_TAIL);
            long sqMaskOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_RING_MASK);
            long sqEntriesOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_RING_ENTRIES);
            long sqFlagsOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_FLAGS);
            long sqArrayOff = u32(paramsSeg, Layouts.PARAMS_SQ_OFF + Layouts.SQOFF_ARRAY);

            long cqHeadOff = u32(paramsSeg, Layouts.PARAMS_CQ_OFF + Layouts.CQOFF_HEAD);
            long cqTailOff = u32(paramsSeg, Layouts.PARAMS_CQ_OFF + Layouts.CQOFF_TAIL);
            long cqMaskOff = u32(paramsSeg, Layouts.PARAMS_CQ_OFF + Layouts.CQOFF_RING_MASK);
            long cqesOff = u32(paramsSeg, Layouts.PARAMS_CQ_OFF + Layouts.CQOFF_CQES);

            long sqRingBytes = sqArrayOff + (long) sqEntries * Integer.BYTES;
            long cqRingBytes = cqesOff + (long) cqEntries * Layouts.CQE_BYTES;
            long sqesBytes = (long) sqEntries * Layouts.SQE_BYTES;
            boolean singleMmap = FeatureFlags.has(features, FeatureFlags.SINGLE_MMAP);

            MemorySegment sqRing;
            MemorySegment cqRing;
            if (singleMmap)
            {
                long ringBytes = Math.max(sqRingBytes, cqRingBytes);
                sqRing = map(fd, cap, arena, MmapOffsets.SQ_RING, ringBytes);
                cqRing = sqRing;
            }
            else
            {
                sqRing = map(fd, cap, arena, MmapOffsets.SQ_RING, sqRingBytes);
                cqRing = map(fd, cap, arena, MmapOffsets.CQ_RING, cqRingBytes);
            }
            MemorySegment sqes = map(fd, cap, arena, MmapOffsets.SQES, sqesBytes);

            int sqRingMask = sqRing.get(JAVA_INT_UNALIGNED, sqMaskOff);
            int sqRingEntries = sqRing.get(JAVA_INT_UNALIGNED, sqEntriesOff);
            int cqRingMask = cqRing.get(JAVA_INT_UNALIGNED, cqMaskOff);

            SubmissionQueue sq = new SubmissionQueue(sqRing, sqes, sqHeadOff, sqTailOff, sqFlagsOff, sqArrayOff,
                                                     sqRingEntries, sqRingMask, setupFlags);
            CompletionQueue cq = new CompletionQueue(cqRing, cqHeadOff, cqTailOff, cqesOff, cqEntries, cqRingMask);
            RingParams params = new RingParams(sqEntries, cqEntries, features, setupFlags, singleMmap);

            return new Ring(fd, arena, cap, sqRing, sqFlagsOff, params, sq, cq);
        }
        catch (RuntimeException | Error e)
        {
            if (fd >= 0)
            {
                try
                {
                    LibC.close(arena.allocate(Errno.CAPTURE), fd);
                }
                catch (RuntimeException | Error ignored)
                {
                    // best effort; the arena close below reclaims everything
                }
            }
            arena.close();
            throw e;
        }
    }

    private static MemorySegment map(int fd, MemorySegment cap, Arena arena, long offset, long len)
    {
        MemorySegment m = LibC.mmap(cap, len, LibC.PROT_READ | LibC.PROT_WRITE,
                                    LibC.MAP_SHARED | LibC.MAP_POPULATE, fd, offset);
        if (m.address() == LibC.MAP_FAILED)
            throw new IoUringException("mmap(offset=0x" + Long.toHexString(offset) + ", len=" + len + ") failed", Errno.of(cap));
        return m.reinterpret(len, arena, seg -> LibC.munmap(seg, len));
    }

    private static long u32(MemorySegment seg, int off)
    {
        return seg.get(JAVA_INT_UNALIGNED, off) & 0xFFFFFFFFL;
    }

    public int fd()
    {
        return fd;
    }

    public RingParams params()
    {
        return params;
    }

    public SubmissionQueue sq()
    {
        return sq;
    }

    public CompletionQueue cq()
    {
        return cq;
    }

    /** The {@code io_uring_enter} boundary for this live ring; forwards directly to the syscall. */
    @Override
    public int enter(int toSubmit, int waitNr, int flags)
    {
        return Syscalls.enter(cap, fd, toSubmit, waitNr, flags, MemorySegment.NULL, 0L);
    }

    /** The reused errno-capture segment for control syscalls on this ring (used by {@link RingSubmitter}). */
    @Override
    public MemorySegment captureSegment()
    {
        return cap;
    }

    /** Current SQ flags word (kernel-owned): carries {@code IORING_SQ_CQ_OVERFLOW}/{@code TASKRUN}. */
    @Override
    public int sqFlags()
    {
        return RingBarriers.loadPlain(sqRing, sqFlagsOff);
    }

    boolean isClosed()
    {
        return closed;
    }

    /**
     * Closes the ring fd, then closes the owning arena (which {@code munmap}s the ring regions). The caller is
     * responsible for having drained all in-flight I/O first; the low-level {@code Ring} does not track it.
     */
    @Override
    public void close()
    {
        if (closed)
            return;
        closed = true;
        LibC.close(cap, fd);
        arena.close();
    }
}
