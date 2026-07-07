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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture; // checkstyle: permit this import
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.Sqe;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.LibC;
import org.apache.cassandra.io.uring.op.Prep;
import org.apache.cassandra.io.uring.reg.BufferRegistrar;
import org.apache.cassandra.io.uring.reg.SyncCancel;
import org.apache.cassandra.io.uring.ring.Ring;
import org.apache.cassandra.io.uring.ring.RingSubmitter;
import org.apache.cassandra.io.uring.spi.IoUringConfig;
import org.apache.cassandra.io.uring.spi.PermitGate;
import org.apache.cassandra.io.uring.spi.RingThreadFactory;
import org.apache.cassandra.io.uring.spi.UringLogger;
import org.apache.cassandra.io.uring.spi.UringStatsListener;

/**
 * One ring driven by one dedicated poller thread (§10, thread-per-ring). Foreign submitters never touch the ring: they
 * create a {@link PendingOp}, take a backpressure permit, enqueue it on an MPSC queue, and wake the poller by writing an
 * eventfd (§9 H6). The poller thread exclusively owns the {@link Ring}, {@link InflightRegistry},
 * {@link CompletionDispatcher} and the prep cursor, so none of them need locks.
 *
 * <p>The poller loop, each iteration: (1) re-arm the eventfd-read SQE if needed; (2) drain the MPSC into the registry;
 * (3) fill SQEs from the pending-prep deque (fresh ops and short-read resubmits); (4) {@code io_uring_enter} waiting for
 * at least one completion (so an idle loop blocks on the eventfd until a foreign submit or {@link #closeAsync()} writes
 * it); (5) reap and dispatch every ready CQE. The SQ is sized to {@code queueDepth + 1} (the {@code +1} for the eventfd
 * op) and the CQ to twice that, so backpressure at {@code queueDepth} makes CQ overflow impossible (§9 H4).
 *
 * <p><b>Drain-before-close (§9 H10):</b> {@link #closeAsync()} flips {@code running} and wakes the poller; the loop keeps
 * reaping until the registry (real ops) drains, then synchronously cancels the lingering eventfd read
 * ({@link SyncCancel}) and only then closes the ring fd, {@code munmap}s (via {@code ring.close()}), and frees the
 * loop's own arena holding the eventfd buffer &mdash; so the kernel can never write freed memory.
 */
public final class IoUringEventLoop implements AutoCloseable
{
    /** Reserved {@code user_data} for the eventfd-wakeup read; real op tokens start at 1 (see {@link InflightRegistry}). */
    private static final long WAKEUP_TOKEN = Long.MIN_VALUE;

    /** Bound on teardown quiesce spins so a stuck submitter cannot hang shutdown forever. */
    private static final int DRAIN_MAX_SPINS = 1 << 20;

    /** Wall-clock budget for the graceful (uncancelled) teardown drain before falling back to cancellation. */
    private static final long GRACEFUL_DRAIN_NANOS = 2_000_000_000L;

    /** Wall-clock budget for the post-cancel teardown drain before giving up on undrainable ops. */
    private static final long CANCEL_DRAIN_NANOS = 2_000_000_000L;

    private final Ring ring;
    private final InflightRegistry registry = new InflightRegistry();
    private final Backpressure backpressure;
    private final CompletionDispatcher dispatcher;
    private final UringStatsListener stats;
    private final UringLogger log;

    private final ManyToOneConcurrentLinkedQueue<PendingOp> submitQueue = new ManyToOneConcurrentLinkedQueue<>();
    // Control tasks (e.g. buffer registration) that must run ON the poller thread, which exclusively owns the ring.
    private final ManyToOneConcurrentLinkedQueue<Runnable> controlTasks = new ManyToOneConcurrentLinkedQueue<>();
    private BufferRegistrar bufferRegistrar;   // poller-only; created on first registerBuffers
    private final ArrayDeque<PendingOp> pendingPrep = new ArrayDeque<>();   // poller-only: registered ops awaiting an SQE

    private final Arena loopArena;              // owns the eventfd buffer + wakeup capture segment
    private final MemorySegment wakeupCap;
    private final MemorySegment eventfdBuf;
    private int eventFd;
    private boolean eventfdArmed;               // poller-only
    private volatile boolean eventFdClosed;     // set by teardown before closing eventFd; read by closeAsync's wake

    private final SyncCancel syncCancel;
    private final Thread poller;
    private final AtomicBoolean closeRequested = new AtomicBoolean();
    private final AtomicInteger activeSubmitters = new AtomicInteger();   // foreign threads currently inside enqueue()
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    private volatile boolean running = true;

    public IoUringEventLoop(IoUringConfig cfg, RingThreadFactory threads, PermitGate gate,
                            UringLogger log, UringStatsListener stats, String name)
    {
        this.log = log;
        this.stats = stats;
        this.backpressure = new Backpressure(gate);

        int entries = ringEntries(cfg.queueDepth());
        int setupFlags = 0;
        if (cfg.sqpoll())
            log.warn("io_uring: SQPOLL requested but not enabled in this build; using the default submit path");
        // setupFlags stays 0 (SQPOLL is a future opt-in; see SetupFlags.SQPOLL).
        this.ring = Ring.open(entries, setupFlags);

        this.loopArena = Arena.ofShared();
        this.wakeupCap = loopArena.allocate(Errno.CAPTURE);
        this.eventfdBuf = loopArena.allocate(8, 8);
        int fd = LibC.eventfd(wakeupCap, 0, LibC.EFD_CLOEXEC);
        if (fd < 0)
        {
            int errno = Errno.of(wakeupCap);
            ring.close();
            loopArena.close();
            throw new IoUringException("eventfd() failed", errno);
        }
        this.eventFd = fd;
        this.syncCancel = new SyncCancel(ring.fd());
        this.dispatcher = new CompletionDispatcher(registry, backpressure, stats, log,
                                                   pendingPrep::add, () -> eventfdArmed = false, WAKEUP_TOKEN);
        this.poller = threads.newRingThread(name, this::runLoop);
    }

    /** Starts the poller thread. Call once, after construction. */
    public void start()
    {
        poller.start();
    }

    public int features()
    {
        return ring.params().features();
    }

    /** Async positional read of {@code dst.remaining()} bytes at file offset {@code off} into the direct buffer {@code dst}. */
    public CompletableFuture<Integer> submitRead(int fd, long off, ByteBuffer dst)
    {
        long addr = directAddress(dst);
        int len = dst.remaining();
        if (len <= 0)
            return failed("read length must be > 0");
        return enqueue(PendingOp.read(fd, addr, len, off, dst));
    }

    /** Async positional write of {@code src.remaining()} bytes at file offset {@code off} from the direct buffer {@code src}. */
    public CompletableFuture<Integer> submitWrite(int fd, long off, ByteBuffer src)
    {
        long addr = directAddress(src);
        int len = src.remaining();
        if (len <= 0)
            return failed("write length must be > 0");
        return enqueue(PendingOp.write(fd, addr, len, off, src));
    }

    /** Async fsync (or fdatasync when {@code dataSync}); the future completes with {@code 0} on success. */
    public CompletableFuture<Integer> submitFsync(int fd, boolean dataSync)
    {
        PendingOp op = PendingOp.fsync(fd);
        op.rwFlags = dataSync ? Opcodes.IORING_FSYNC_DATASYNC : 0;
        return enqueue(op);
    }

    /** Async positional {@code READ_FIXED} into a registered buffer (see {@link #registerBuffers}). */
    public CompletableFuture<Integer> submitReadFixed(int fd, long off, ByteBuffer dst, int bufIndex)
    {
        long addr = directAddress(dst);
        int len = dst.remaining();
        if (len <= 0)
            return failed("read length must be > 0");
        return enqueue(PendingOp.readFixed(fd, addr, len, off, dst, bufIndex));
    }

    /**
     * Register {@code addrs[i]}/{@code lens[i]} as this ring's fixed buffers (enabling {@link #submitReadFixed}), on the
     * poller thread. Throws {@link IoUringException} (e.g. {@code ENOMEM}/{@code EPERM} from {@code RLIMIT_MEMLOCK}) so
     * the caller can fall back to unregistered reads.
     */
    public void registerBuffers(long[] addrs, long[] lens)
    {
        runOnPoller(() -> {
            if (bufferRegistrar == null)
                bufferRegistrar = new BufferRegistrar(ring.fd());
            bufferRegistrar.registerBuffers(addrs, lens);
        });
    }

    /** Reserve {@code nr} empty (sparse) fixed-buffer slots on this ring, to be filled by {@link #registerBuffersUpdate}. */
    public void registerBuffersSparse(int nr)
    {
        runOnPoller(() -> {
            if (bufferRegistrar == null)
                bufferRegistrar = new BufferRegistrar(ring.fd());
            bufferRegistrar.registerBuffersSparse(nr);
        });
    }

    /** Fill sparse fixed-buffer slots {@code [offset, offset+addrs.length)} on this ring, on the poller thread. */
    public void registerBuffersUpdate(int offset, long[] addrs, long[] lens)
    {
        runOnPoller(() -> {
            if (bufferRegistrar == null)
                bufferRegistrar = new BufferRegistrar(ring.fd());
            bufferRegistrar.registerBuffersUpdate(offset, addrs, lens);
        });
    }

    /** Runs {@code task} on the poller thread and blocks the caller until it completes. Never call from the poller. */
    private void runOnPoller(Runnable task)
    {
        CompletableFuture<Void> done = new CompletableFuture<>();
        activeSubmitters.incrementAndGet();
        try
        {
            if (!running)
                throw new IoUringException("io_uring event loop is closed");
            controlTasks.offer(() -> {
                try
                {
                    task.run();
                    done.complete(null);
                }
                catch (Throwable t)
                {
                    done.completeExceptionally(t);
                }
            });
            if (!eventFdClosed)
                LibC.eventfdWrite(eventFd, 1L);
        }
        finally
        {
            activeSubmitters.decrementAndGet();
        }
        try
        {
            done.join();
        }
        catch (java.util.concurrent.CompletionException e)
        {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            throw new IoUringException("io_uring control task failed", cause);
        }
    }

    private void runControlTasks()
    {
        Runnable task;
        while ((task = controlTasks.poll()) != null)
            task.run();
    }

    private CompletableFuture<Integer> enqueue(PendingOp op)
    {
        // activeSubmitters gates teardown: it will not close the eventfd / free the loop arena while any foreign
        // thread is between here and the eventfd write below (review finding: submit-vs-close race).
        activeSubmitters.incrementAndGet();
        try
        {
            if (!running)
                return closedFuture(op);
            op.submitNanos = System.nanoTime(); // checkstyle: permit system clock
            backpressure.acquire();              // blocks this (foreign) thread if the ring is full; never the poller
            if (!running)                        // closed while we waited: undo and reject
            {
                backpressure.release();
                return closedFuture(op);
            }
            submitQueue.offer(op);
            if (!eventFdClosed)
                LibC.eventfdWrite(eventFd, 1L);   // wake the poller; eventFd stays open until activeSubmitters hits 0
            return op.future;
        }
        finally
        {
            activeSubmitters.decrementAndGet();
        }
    }

    // ---- poller thread ----

    private void runLoop()
    {
        try
        {
            while (running)
            {
                runControlTasks();
                armEventfd();
                drainSubmitQueue();
                prepPending();
                try
                {
                    RingSubmitter.submitAndWait(ring, 1);
                }
                catch (RuntimeException e)
                {
                    running = false;   // abnormal exit: reject new submits immediately (review finding: dead loop keeps accepting)
                    log.error("io_uring_enter failed; stopping poller and draining in-flight ops", e);
                    break;
                }
                ring.cq().forEachCqe(dispatcher, ring.cq().ringEntries());
                stats.inflightGauge(registry.size());
            }
            // running flipped false (close, or abnormal break). The eventfd wakeup guarantees a prompt exit even if a
            // data op is stuck; teardown drains what is in flight under a bound so shutdown never hangs on it.
        }
        finally
        {
            teardown();
        }
    }

    private void armEventfd()
    {
        if (eventfdArmed)
            return;
        MemorySegment sqe = ring.sq().getSqe();
        if (sqe == null)
            return;                              // no slot right now; retried next iteration (sizing makes this rare)
        Prep.prepRead(sqe, eventFd, eventfdBuf.address(), 8, 0L);
        Prep.setData64(sqe, WAKEUP_TOKEN);
        eventfdArmed = true;
    }

    private void drainSubmitQueue()
    {
        PendingOp op;
        while ((op = submitQueue.poll()) != null)
        {
            registry.register(op);               // assign token + become the buffer keep-alive before submission
            pendingPrep.add(op);
        }
    }

    private void prepPending()
    {
        while (!pendingPrep.isEmpty())
        {
            MemorySegment sqe = ring.sq().getSqe();
            if (sqe == null)
                break;                           // SQ full; remaining ops wait for the next iteration
            prepInto(sqe, pendingPrep.poll());
        }
    }

    private void prepInto(MemorySegment sqe, PendingOp op)
    {
        switch (op.op)
        {
            case Opcodes.READ:
                Prep.prepRead(sqe, op.fd, op.curAddr, op.remaining, op.fileOffset);
                if (op.rwFlags != 0)
                    Sqe.rwFlags(sqe, op.rwFlags);
                break;
            case Opcodes.READ_FIXED:
                // curAddr advances within the same registered buffer on a short-read resubmit, so buf_index is stable.
                Prep.prepReadFixed(sqe, op.fd, op.curAddr, op.remaining, op.fileOffset, op.bufIndex);
                break;
            case Opcodes.WRITE:
                Prep.prepWrite(sqe, op.fd, op.curAddr, op.remaining, op.fileOffset);
                if (op.rwFlags != 0)
                    Sqe.rwFlags(sqe, op.rwFlags);
                break;
            case Opcodes.FSYNC:
                Prep.prepFsync(sqe, op.fd, op.rwFlags);
                break;
            default:
                throw new IoUringException("unsupported op " + op.op);
        }
        Prep.setData64(sqe, op.userData);
        stats.onSubmit();
    }

    private void teardown()
    {
        running = false;   // guarantee closed even on abnormal (fatal-enter) exit so enqueue() rejects new ops
        try
        {
            // Phase 1: graceful, NON-BLOCKING, bounded drain -- let already-submitted ops complete normally (so a clean
            // close returns real read results, not spurious cancellations) without ever parking in the kernel.
            boundedDrain(GRACEFUL_DRAIN_NANOS);
            // Phase 2: cancel any straggler (and the armed eventfd read) and reap their terminal CQEs, still bounded.
            if (!registry.isEmpty())
            {
                try
                {
                    syncCancel.cancelAll();
                }
                catch (RuntimeException e)
                {
                    log.warn("io_uring: sync-cancel during teardown failed", e);
                }
            }
            boundedDrain(CANCEL_DRAIN_NANOS);
            ring.cq().forEachCqe(dispatcher, ring.cq().ringEntries());   // final sweep incl. the eventfd read's -ECANCELED
        }
        finally
        {
            // Close the eventfd only once no foreign thread can still write it: publish the closed flag, then wait for
            // any in-flight enqueue()/closeAsync() wake to finish (§9 H6/H10, review findings).
            eventFdClosed = true;
            quiesceSubmitters();
            // Flush any pending control task (e.g. a registerBuffers that raced close) so its caller does not hang;
            // then unregister/close the buffer registrar while the ring fd is still open.
            runControlTasks();
            if (bufferRegistrar != null)
            {
                try
                {
                    bufferRegistrar.unregisterBuffers();
                }
                catch (RuntimeException ignored)
                {
                    // nothing registered, or already gone; the arena close below reclaims everything
                }
                bufferRegistrar.close();
            }
            // Everything we could reap is kernel-quiesced. Close ring fd + munmap, then eventfd, then the loop arena.
            ring.close();
            LibC.close(wakeupCap, eventFd);
            loopArena.close();
            syncCancel.close();
            // Undrainable ops (a wedged ring/device that ignored cancellation): the kernel may STILL write their
            // buffers, so completing these futures risks a use-after-free if the caller reuses the buffer. We cannot
            // both avoid that and avoid hanging the caller; we surface the failure (so callers do not hang forever) and
            // log loudly -- this state means the ring/device is wedged and the node should fail its disk.
            if (!registry.isEmpty())
            {
                int stuck = registry.size();
                registry.failAll(op -> {
                    backpressure.release();
                    op.future.completeExceptionally(new IoUringException(
                        "io_uring ring wedged; op undrainable at shutdown -- its buffer must be treated as poisoned"));
                });
                log.error("io_uring: {} op(s) undrainable at teardown (ring/device wedged); their buffers are poisoned "
                          + "and the node should treat this as a disk failure", stuck);
            }
            // Fail any op a racing submitter offered after the poller stopped draining (release its permit too).
            PendingOp late;
            while ((late = submitQueue.poll()) != null)
            {
                backpressure.release();
                late.future.completeExceptionally(new IoUringException("io_uring event loop is closed"));
            }
            closeFuture.complete(null);
        }
    }

    /**
     * Non-blocking, wall-clock-bounded drain: repeatedly submit any accepted ops (fresh <em>and</em> short-read
     * resubmits) and reap ready completions via a {@code GETEVENTS}/{@code min_complete=0} enter, so a genuinely-stuck
     * in-flight op cannot park the poller and hang shutdown. Returns when the registry empties or the budget is spent.
     */
    private void boundedDrain(long budgetNanos)
    {
        long start = System.nanoTime(); // checkstyle: permit system clock
        // Drain accepted-but-unsubmitted ops (submitQueue/pendingPrep) too, not just those already in the registry:
        // on close the poller may exit with ops still queued, and they must be submitted and completed, not failed.
        while (!registry.isEmpty() || !submitQueue.isEmpty() || !pendingPrep.isEmpty())
        {
            drainSubmitQueue();
            prepPending();                       // re-prep fresh ops AND short-read resubmits so they actually submit
            try
            {
                RingSubmitter.submitAndReapNonBlocking(ring);
            }
            catch (RuntimeException e)
            {
                break;                           // ring wedged; fall through to the last-resort path
            }
            ring.cq().forEachCqe(dispatcher, ring.cq().ringEntries());
            boolean idle = registry.isEmpty() && submitQueue.isEmpty() && pendingPrep.isEmpty();
            if (idle || System.nanoTime() - start >= budgetNanos) // checkstyle: permit system clock
                break;
            Thread.onSpinWait();
        }
    }

    /** Wake any {@code acquire()}-blocked submitter and wait until no foreign thread is inside {@link #enqueue}/wake. */
    private void quiesceSubmitters()
    {
        long spins = 0;
        while (activeSubmitters.get() > 0)
        {
            backpressure.release();              // surplus permits are harmless at shutdown; this frees blocked acquirers
            if (++spins > DRAIN_MAX_SPINS)
                break;
            Thread.onSpinWait();
        }
    }

    // ---- lifecycle ----

    /** Requests shutdown, draining in-flight ops first; returns a future that completes when teardown is done. */
    public CompletableFuture<Void> closeAsync()
    {
        if (closeRequested.compareAndSet(false, true))
        {
            running = false;
            // Wake the poller, but guard the write like a submitter so teardown never closes eventFd underneath it
            // (review finding: eventfd_write to a closed/recycled fd). activeSubmitters gates teardown's quiesce, and
            // eventFdClosed short-circuits the write if teardown already closed the fd (abnormal self-teardown).
            activeSubmitters.incrementAndGet();
            try
            {
                if (!eventFdClosed)
                    LibC.eventfdWrite(eventFd, 1L);
            }
            finally
            {
                activeSubmitters.decrementAndGet();
            }
        }
        return closeFuture;
    }

    /** Blocking close; must not be called from the poller thread. */
    @Override
    public void close()
    {
        closeAsync().join();
    }

    private static long directAddress(ByteBuffer bb)
    {
        if (!bb.isDirect())
            throw new IllegalArgumentException("io_uring requires a direct ByteBuffer (heap buffers have no stable address)");
        return MemorySegment.ofBuffer(bb).address();
    }

    private CompletableFuture<Integer> failed(String message)
    {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        f.completeExceptionally(new IllegalArgumentException(message));
        return f;
    }

    private CompletableFuture<Integer> closedFuture(PendingOp op)
    {
        op.future.completeExceptionally(new IoUringException("io_uring event loop is closed"));
        return op.future;
    }

    private static int ringEntries(int queueDepth)
    {
        int need = queueDepth + 1;               // +1 for the eventfd-wakeup op
        int p = Integer.highestOneBit(need);
        if (p < need)
            p <<= 1;
        return Math.max(p, 8);
    }
}
