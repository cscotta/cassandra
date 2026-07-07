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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.io.uring.abi.FeatureFlags;
import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.SqCqFlags;
import org.apache.cassandra.io.uring.abi.Sqe;

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * A deterministic in-process {@link RingHandle} that fakes the kernel side of io_uring for tests, with NO real ring
 * fd, mmap, or syscall. It allocates its own SQ/CQ/SQE segments (page-role-equivalent, heap-backed via a shared
 * {@link Arena}, 8-byte aligned so the aligned {@link RingBarriers} handles are legal) and builds the <em>real</em>
 * {@link SubmissionQueue}/{@link CompletionQueue} over them &mdash; so a test drives the genuine getSqe / flushSq /
 * forEachCqe cursors, the real acquire/release barriers, and (through {@link RingSubmitter} and, above it, the async
 * event loop) the real submit/reap/dispatch/resubmit/teardown logic. Only {@link #enter} is faked: it plays the
 * kernel, consuming the SQEs the app flushed and writing synthetic CQEs.
 *
 * <p>The fake reads each SQE via {@link Sqe}/raw offsets and, by default, services {@code READ}/{@code READ_FIXED} as
 * a faithful oracle against a {@link #mapFile mapped} byte image (copying into the SQE's destination address, short at
 * EOF), {@code NOP}/{@code FSYNC} as {@code res=0}, and {@code WRITE} as {@code res=len}. Faults are injected per the
 * next read via {@link #shortReadNext}/{@link #errorNext}/{@link #eofNext}/{@link #deferNext}; deferred ops complete
 * later and out of order via {@link #postCqe}; {@link #injectCqOverflow} exercises the CQ-overflow backlog path.
 *
 * <p>Single-threaded per the ring contract: at Layer 1 {@link #enter} runs inline on the test thread (via
 * {@link RingSubmitter}); at Layer 2 it runs on the event-loop poller thread. Not safe for concurrent callers.
 */
public final class FakeIoUringRing implements RingHandle
{
    // Compact ABI-role layout the fake both writes and reports to the real SubmissionQueue/CompletionQueue ctors.
    private static final long SQ_HEAD = 0;      // kernel-owned: the fake (kernel) advances it as it consumes SQEs
    private static final long SQ_TAIL = 4;      // app-owned: the app publishes it in flushSq
    private static final long SQ_FLAGS = 8;     // carries IORING_SQ_CQ_OVERFLOW
    private static final long SQ_ARRAY = 64;    // sq_array[] index table (sqEntries ints)

    private static final long CQ_HEAD = 0;      // app-owned: forEachCqe advances it
    private static final long CQ_TAIL = 4;      // kernel-owned: the fake advances it as it posts CQEs
    private static final long CQ_OVERFLOW = 8;  // count of CQEs the kernel could not deliver
    private static final long CQ_CQES = 64;     // cqe[] array (cqEntries * 16 bytes)

    private static final int FAKE_FD = -1000;   // sentinel; never a real fd

    private final Arena arena;
    private final MemorySegment sqRing;
    private final MemorySegment cqRing;
    private final MemorySegment sqes;
    private final MemorySegment cap;            // errno-capture segment (RingSubmitter reads it on enter() < 0)
    private final RingParams params;
    private final SubmissionQueue sq;
    private final CompletionQueue cq;
    private final int sqEntries;
    private final int cqEntries;
    private final int sqMask;
    private final int cqMask;

    // Fake-kernel state.
    private int kernelSqHead;                   // SQEs consumed so far (mirror of the SQ head we release-store)
    private int cqTailLocal;                    // CQEs posted so far (mirror of the CQ tail we release-store)
    private final Map<Integer, byte[]> files = new HashMap<>();
    private final ArrayDeque<int[]> overrides = new ArrayDeque<>();   // {kind, value} applied to the next read
    private final ArrayDeque<long[]> backlog = new ArrayDeque<>();    // {userData, res, flags} spilled on CQ overflow
    private final List<Arrival> arrivals = new ArrayList<>();
    private boolean forceSpill;

    private static final int OV_SHORT_READ = 0;
    private static final int OV_ERROR = 1;
    private static final int OV_EOF = 2;
    private static final int OV_DEFER = 3;

    private FakeIoUringRing(int sqEntries, int cqEntries)
    {
        if (Integer.bitCount(sqEntries) != 1 || Integer.bitCount(cqEntries) != 1)
            throw new IllegalArgumentException("entries must be powers of two: sq=" + sqEntries + " cq=" + cqEntries);
        this.sqEntries = sqEntries;
        this.cqEntries = cqEntries;
        this.sqMask = sqEntries - 1;
        this.cqMask = cqEntries - 1;
        this.arena = Arena.ofShared();
        this.sqRing = arena.allocate(SQ_ARRAY + (long) sqEntries * Integer.BYTES, 8);
        this.cqRing = arena.allocate(CQ_CQES + (long) cqEntries * Layouts.CQE_BYTES, 8);
        this.sqes = arena.allocate((long) sqEntries * Layouts.SQE_BYTES, 8);
        this.cap = arena.allocate(org.apache.cassandra.io.uring.linux.Errno.CAPTURE);
        this.sq = new SubmissionQueue(sqRing, sqes, SQ_HEAD, SQ_TAIL, SQ_FLAGS, SQ_ARRAY, sqEntries, sqMask, 0);
        this.cq = new CompletionQueue(cqRing, CQ_HEAD, CQ_TAIL, CQ_CQES, cqEntries, cqMask);
        int features = FeatureFlags.SINGLE_MMAP | FeatureFlags.NODROP;
        this.params = new RingParams(sqEntries, cqEntries, features, 0, true);
    }

    /** Opens a fake ring with {@code entries} SQEs and a CQ of {@code 2*entries} (mirroring the event-loop sizing). */
    public static FakeIoUringRing open(int entries)
    {
        return new FakeIoUringRing(entries, entries * 2);
    }

    // ---- programmability -------------------------------------------------------------------------------------------

    /** Provides the byte image a read of {@code fd} draws from (oracle default). Returns {@code this} for chaining. */
    public FakeIoUringRing mapFile(int fd, byte[] contents)
    {
        files.put(fd, contents);
        return this;
    }

    /** The next serviced read completes with {@code res = min(n, available)} (a short read), copying that many bytes. */
    public FakeIoUringRing shortReadNext(int n)
    {
        overrides.add(new int[]{ OV_SHORT_READ, n });
        return this;
    }

    /** The next serviced read completes with {@code res = -errno} and no data copied. */
    public FakeIoUringRing errorNext(int errno)
    {
        overrides.add(new int[]{ OV_ERROR, errno });
        return this;
    }

    /** The next serviced read completes with {@code res = 0} (EOF). */
    public FakeIoUringRing eofNext()
    {
        overrides.add(new int[]{ OV_EOF, 0 });
        return this;
    }

    /** The next serviced read posts NO completion this {@link #enter}; complete it later via {@link #postCqe}. */
    public FakeIoUringRing deferNext()
    {
        overrides.add(new int[]{ OV_DEFER, 0 });
        return this;
    }

    /** Posts a completion for a previously {@link #deferNext deferred} op (or any op), enabling out-of-order delivery. */
    public void postCqe(long userData, int res, int flags)
    {
        writeCqe(userData, res, flags);
    }

    /** Forces subsequent completions to spill to the CQ-overflow backlog and sets {@code IORING_SQ_CQ_OVERFLOW}. */
    public void injectCqOverflow()
    {
        forceSpill = true;
    }

    /** The decoded SQEs the fake kernel has seen, in submission order (test introspection). */
    public List<Arrival> arrivals()
    {
        return arrivals;
    }

    // ---- RingHandle ------------------------------------------------------------------------------------------------

    @Override
    public int fd()
    {
        return FAKE_FD;
    }

    @Override
    public RingParams params()
    {
        return params;
    }

    @Override
    public SubmissionQueue sq()
    {
        return sq;
    }

    @Override
    public CompletionQueue cq()
    {
        return cq;
    }

    @Override
    public MemorySegment captureSegment()
    {
        return cap;
    }

    @Override
    public int sqFlags()
    {
        return RingBarriers.loadPlain(sqRing, SQ_FLAGS);
    }

    /**
     * The fake kernel: flush any overflow backlog into freed CQ slots, then consume every SQE the app published (from
     * {@link #kernelSqHead} to the acquire-loaded SQ tail), service each, release-store the advanced SQ head so
     * {@link SubmissionQueue#getSqe} can reuse the slots, and return the count consumed. Never fails at the syscall
     * level (op errors are delivered as {@code res < 0} CQEs), so it always returns {@code >= 0}.
     */
    @Override
    public int enter(int toSubmit, int waitNr, int flags)
    {
        drainBacklog();
        // The fake plays the kernel: acquire-load the app-published SQ tail (mirror of loadCqTail's getAcquire).
        int appTail = RingBarriers.loadCqTail(sqRing, SQ_TAIL);
        int consumed = 0;
        while (kernelSqHead != appTail)
        {
            int sqeIndex = RingBarriers.loadPlain(sqRing, SQ_ARRAY + (long) (kernelSqHead & sqMask) * Integer.BYTES);
            MemorySegment sqe = sqes.asSlice((long) (sqeIndex & sqMask) * Layouts.SQE_BYTES, Layouts.SQE_BYTES);
            Arrival ar = decode(sqe);
            arrivals.add(ar);
            service(ar);
            kernelSqHead++;
            consumed++;
        }
        // Release-store the SQ head (mirror of advanceCqHead's setRelease) so getSqe observes the freed slots.
        RingBarriers.advanceCqHead(sqRing, SQ_HEAD, kernelSqHead);
        return consumed;
    }

    @Override
    public void close()
    {
        arena.close();
    }

    // ---- fake kernel internals -------------------------------------------------------------------------------------

    private Arrival decode(MemorySegment sqe)
    {
        int opcode = Sqe.opcodeOf(sqe);
        int fd = sqe.get(JAVA_INT_UNALIGNED, Layouts.SQE_FD);
        long off = sqe.get(JAVA_LONG_UNALIGNED, Layouts.SQE_OFF);
        long addr = sqe.get(JAVA_LONG_UNALIGNED, Layouts.SQE_ADDR);
        int len = sqe.get(JAVA_INT_UNALIGNED, Layouts.SQE_LEN);
        long userData = Sqe.userDataOf(sqe);
        int bufIndex = sqe.get(JAVA_SHORT_UNALIGNED, Layouts.SQE_BUF_INDEX) & 0xFFFF;
        return new Arrival(opcode, fd, addr, len, off, userData, bufIndex);
    }

    private void service(Arrival ar)
    {
        switch (ar.opcode)
        {
            case Opcodes.READ:
            case Opcodes.READ_FIXED:
                serviceRead(ar);
                break;
            case Opcodes.NOP:
            case Opcodes.FSYNC:
                writeCqe(ar.userData, 0, 0);
                break;
            case Opcodes.WRITE:
                writeCqe(ar.userData, ar.len, 0);
                break;
            default:
                writeCqe(ar.userData, -org.apache.cassandra.io.uring.linux.Errno.EINVAL, 0);
                break;
        }
    }

    private void serviceRead(Arrival ar)
    {
        int[] override = overrides.poll();
        if (override != null)
        {
            switch (override[0])
            {
                case OV_DEFER:
                    return;   // no completion this enter(); the test will postCqe() it later
                case OV_ERROR:
                    writeCqe(ar.userData, -override[1], 0);
                    return;
                case OV_EOF:
                    writeCqe(ar.userData, 0, 0);
                    return;
                case OV_SHORT_READ:
                    int n = Math.min(override[1], available(ar));
                    copy(ar, n);
                    writeCqe(ar.userData, n, 0);
                    return;
                default:
                    break;
            }
        }
        int n = available(ar);
        copy(ar, n);
        writeCqe(ar.userData, n, 0);
    }

    /** Bytes an oracle read of {@code ar} can return: {@code min(len, fileLen - off)}, or 0 past EOF / unmapped fd. */
    private int available(Arrival ar)
    {
        byte[] file = files.get(ar.fd);
        if (file == null || ar.off >= file.length)
            return 0;
        return (int) Math.min(ar.len, file.length - ar.off);
    }

    private void copy(Arrival ar, int n)
    {
        if (n <= 0)
            return;
        byte[] file = files.get(ar.fd);
        MemorySegment dest = MemorySegment.ofAddress(ar.addr).reinterpret(n);
        MemorySegment.copy(MemorySegment.ofArray(file), (int) ar.off, dest, 0, n);
    }

    private void writeCqe(long userData, int res, int flags)
    {
        if (forceSpill || cqFull())
        {
            backlog.add(new long[]{ userData, res & 0xFFFFFFFFL, flags });
            setOverflow();
            return;
        }
        rawWriteCqe(userData, res, flags);
    }

    private boolean cqFull()
    {
        int appHead = RingBarriers.loadPlain(cqRing, CQ_HEAD);
        return (cqTailLocal - appHead) >= cqEntries;
    }

    private void rawWriteCqe(long userData, int res, int flags)
    {
        long base = CQ_CQES + (long) (cqTailLocal & cqMask) * Layouts.CQE_BYTES;
        cqRing.set(JAVA_LONG_UNALIGNED, base + Layouts.CQE_USER_DATA, userData);
        cqRing.set(JAVA_INT_UNALIGNED, base + Layouts.CQE_RES, res);
        cqRing.set(JAVA_INT_UNALIGNED, base + Layouts.CQE_FLAGS, flags);
        cqTailLocal++;
        // Release-store the CQ tail (mirror of publishSqTail's setRelease) so forEachCqe's acquire-load sees the CQE.
        RingBarriers.publishSqTail(cqRing, CQ_TAIL, cqTailLocal);
    }

    private void drainBacklog()
    {
        while (!backlog.isEmpty() && !cqFull())
        {
            long[] e = backlog.poll();
            rawWriteCqe(e[0], (int) e[1], (int) e[2]);
        }
        if (backlog.isEmpty())
            clearOverflow();
    }

    private void setOverflow()
    {
        RingBarriers.storePlain(sqRing, SQ_FLAGS, RingBarriers.loadPlain(sqRing, SQ_FLAGS) | SqCqFlags.CQ_OVERFLOW);
        RingBarriers.storePlain(cqRing, CQ_OVERFLOW, RingBarriers.loadPlain(cqRing, CQ_OVERFLOW) + 1);
    }

    private void clearOverflow()
    {
        RingBarriers.storePlain(sqRing, SQ_FLAGS, RingBarriers.loadPlain(sqRing, SQ_FLAGS) & ~SqCqFlags.CQ_OVERFLOW);
    }

    /** A decoded SQE the fake kernel observed. */
    public static final class Arrival
    {
        public final int opcode;
        public final int fd;
        public final long addr;
        public final int len;
        public final long off;
        public final long userData;
        public final int bufIndex;

        Arrival(int opcode, int fd, long addr, int len, long off, long userData, int bufIndex)
        {
            this.opcode = opcode;
            this.fd = fd;
            this.addr = addr;
            this.len = len;
            this.off = off;
            this.userData = userData;
            this.bufIndex = bufIndex;
        }
    }
}
