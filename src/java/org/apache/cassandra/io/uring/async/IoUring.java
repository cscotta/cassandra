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

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture; // checkstyle: permit this import
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.probe.Capabilities;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.spi.IoUringConfig;
import org.apache.cassandra.io.uring.spi.PermitGate;
import org.apache.cassandra.io.uring.spi.RingThreadFactory;
import org.apache.cassandra.io.uring.spi.UringLogger;
import org.apache.cassandra.io.uring.spi.UringStatsListener;

/**
 * The high-level asynchronous facade: a small pool of {@link IoUringEventLoop}s (one ring + poller thread each, per
 * {@link IoUringConfig#pollerThreads()}) with reads/writes/fsyncs round-robined across them and returned as
 * {@link CompletableFuture}s. This is the entry point the Cassandra adapter drives; under Strategy&nbsp;C the read code
 * stays textually synchronous by calling {@code future.get()} on a virtual thread, whose carrier unmounts while the
 * device I/O is in flight (the poller, a platform thread, does the blocking {@code io_uring_enter}).
 *
 * <p>{@link #open(IoUringConfig)} uses the standalone SPI defaults; the overload injects the Cassandra adapter's
 * {@link RingThreadFactory}/{@link PermitGate}/{@link UringLogger}/{@link UringStatsListener}. Availability is probed
 * once (§11) and {@code open} throws if io_uring is unusable, leaving the fallback-to-{@code standard} decision to the
 * caller. {@link #close()} drains and tears down every loop.
 */
public final class IoUring implements AutoCloseable
{
    private final IoUringEventLoop[] loops;
    private final AtomicInteger next = new AtomicInteger();
    private final int features;

    private IoUring(IoUringEventLoop[] loops)
    {
        this.loops = loops;
        this.features = loops[0].features();
    }

    /** Open with the standalone SPI defaults (daemon poller threads, slf4j logging, no-op stats, Semaphore gate). */
    public static IoUring open(IoUringConfig cfg)
    {
        return open(cfg, RingThreadFactory.DaemonRingThreadFactory.INSTANCE,
                    UringLogger.Slf4jUringLogger.forClass(IoUring.class), UringStatsListener.NOOP);
    }

    /**
     * Open with caller-supplied SPI implementations. Probes availability once and throws {@link IoUringException} if
     * io_uring is unusable on this host. Each poller thread gets its own {@link PermitGate} sized to the queue depth.
     */
    public static IoUring open(IoUringConfig cfg, RingThreadFactory threads, UringLogger log, UringStatsListener stats)
    {
        Capabilities caps = IoUringAvailability.check();
        if (!caps.isAvailable())
            throw new IoUringException("io_uring unavailable: " + caps.reason());

        int n = cfg.pollerThreads();
        IoUringEventLoop[] loops = new IoUringEventLoop[n];
        int created = 0;
        try
        {
            for (int i = 0; i < n; i++)
            {
                PermitGate gate = new PermitGate.SemaphorePermitGate(cfg.queueDepth());
                loops[i] = new IoUringEventLoop(cfg, threads, gate, log, stats, "io_uring-loop-" + i);
                loops[i].start();
                created = i + 1;
            }
        }
        catch (RuntimeException | Error e)
        {
            for (int i = 0; i < created; i++)   // unwind any loops already started
                loops[i].closeAsync();
            throw e;
        }
        return new IoUring(loops);
    }

    /** The probed {@code IORING_FEAT_*} bitset (cheap, cached). */
    public static Capabilities probeCapabilities()
    {
        return IoUringAvailability.check();
    }

    /** The {@code IORING_FEAT_*} bitset of this instance's rings. */
    public int features()
    {
        return features;
    }

    /** Number of rings / poller threads backing this instance. */
    public int pollers()
    {
        return loops.length;
    }

    /** Async positional read of {@code dst.remaining()} bytes at {@code off} into the direct buffer {@code dst}. */
    public CompletableFuture<Integer> read(int fd, long off, ByteBuffer dst)
    {
        return route().submitRead(fd, off, dst);
    }

    /**
     * Register {@code buffers} (their current {@code [position, limit)} regions) as fixed buffers on <em>every</em>
     * ring, so a subsequent {@link #readFixed} to any ring can address them by the shared {@code buf_index} =
     * their position in this array. Each buffer must be direct. Throws {@link IoUringException} if the kernel rejects
     * the pin (e.g. {@code RLIMIT_MEMLOCK}); the caller should then fall back to {@link #read}.
     */
    public void registerBuffers(ByteBuffer[] buffers)
    {
        long[] addrs = new long[buffers.length];
        long[] lens = new long[buffers.length];
        for (int i = 0; i < buffers.length; i++)
        {
            ByteBuffer b = buffers[i];
            if (!b.isDirect())
                throw new IllegalArgumentException("registered buffers must be direct");
            addrs[i] = MemorySegment.ofBuffer(b).address();
            lens[i] = b.remaining();
        }
        for (IoUringEventLoop loop : loops)
            loop.registerBuffers(addrs, lens);
    }

    /**
     * Reserve {@code nr} empty (sparse) fixed-buffer slots on <em>every</em> ring, to be filled incrementally with
     * {@link #registerBuffersUpdate} (e.g. as a buffer pool grows). Slot indices are consistent across rings.
     */
    public void registerBuffersSparse(int nr)
    {
        for (IoUringEventLoop loop : loops)
            loop.registerBuffersSparse(nr);
    }

    /**
     * Register the region {@code [addr, addr+len)} into sparse fixed-buffer slot {@code slot} on <em>every</em> ring,
     * so a subsequent {@link #readFixed} to any ring can address it by {@code bufIndex == slot}.
     */
    public void registerBuffersUpdate(int slot, long addr, long len)
    {
        long[] addrs = { addr };
        long[] lens = { len };
        for (IoUringEventLoop loop : loops)
            loop.registerBuffersUpdate(slot, addrs, lens);
    }

    /**
     * Async {@code READ_FIXED} of {@code dst.remaining()} bytes at {@code off} into {@code dst}, which must lie inside
     * the region registered at {@code bufIndex} via {@link #registerBuffers}.
     */
    public CompletableFuture<Integer> readFixed(int fd, long off, ByteBuffer dst, int bufIndex)
    {
        return route().submitReadFixed(fd, off, dst, bufIndex);
    }

    /** Async positional write of {@code src.remaining()} bytes at {@code off} from the direct buffer {@code src}. */
    public CompletableFuture<Integer> write(int fd, long off, ByteBuffer src)
    {
        return route().submitWrite(fd, off, src);
    }

    /** Async fsync (or fdatasync when {@code dataSync}); the future completes when the sync is durable. */
    public CompletableFuture<Void> fsync(int fd, boolean dataSync)
    {
        return route().submitFsync(fd, dataSync).thenApply(ignored -> null);
    }

    private IoUringEventLoop route()
    {
        return loops.length == 1 ? loops[0] : loops[Math.floorMod(next.getAndIncrement(), loops.length)];
    }

    /** Drains and tears down every ring; blocks until all are closed. Do not call from a poller thread. */
    @Override
    public void close()
    {
        List<CompletableFuture<Void>> closing = new ArrayList<>(loops.length);
        for (IoUringEventLoop loop : loops)
            closing.add(loop.closeAsync());
        for (CompletableFuture<Void> cf : closing)
            cf.join();
    }
}
