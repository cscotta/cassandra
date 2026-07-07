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
package org.apache.cassandra.test.microbench;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.sun.nio.file.ExtendedOpenOption;

import org.agrona.BufferUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import org.apache.cassandra.io.uring.op.Prep;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.ring.Ring;
import org.apache.cassandra.io.uring.ring.RingSubmitter;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Head-to-head throughput of the FFM io_uring binding versus standard {@link FileChannel} I/O, in two cache regimes
 * and at a configurable queue depth / thread count, across sequential reads, random reads and sequential writes at
 * chunk sizes 4/8/16/32/64 KiB:
 * <ul>
 *   <li><b>WARM</b> (default) &mdash; buffered I/O through the page cache. Measures per-op mechanism overhead; io_uring
 *       is expected to lose because there is no device latency to overlap.</li>
 *   <li><b>COLD</b> &mdash; <code>O_DIRECT</code> on both sides (page cache bypassed, {@code posix_fadvise(DONTNEED)}
 *       in setup), so every read reaches the NVMe. This is the device-bound regime where io_uring's ability to keep
 *       many I/Os in flight from one thread can win.</li>
 * </ul>
 *
 * <p>{@code -p mode=WARM|COLD}, {@code -p qd=<queue depth>} and JMH {@code -t <threads>} select the regime. The
 * io_uring side keeps {@code qd} ops in flight per thread (one {@code io_uring_enter} per batch); the
 * {@link FileChannel} side issues {@code qd} blocking positioned calls per invocation (one in flight per thread) &mdash;
 * so at high {@code qd} the comparison shows what a single thread can drive each way. State is {@link Scope#Thread} so
 * {@code -t N} gives each thread its own ring, fd and buffers.
 *
 * <p>The backing file lives on the NVMe btrfs (not tmpfs, or "cold" would be RAM) and is created once and reused.
 * {@code O_DIRECT} is {@code 0x10000} on this aarch64 host (arm64 swaps O_DIRECT/O_DIRECTORY vs x86-64). Throughput is
 * reported by JMH as batches/sec; the report multiplies by {@code qd * chunk} for GB/s. Run e.g.:
 * {@code ant microbench -Dbenchmark.name=IoUringVsBufferedIo -Djmh.args="-p mode=COLD -p qd=128 -t 4"}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 4, time = 1)
@Fork(value = 1, jvmArgsAppend = { "--enable-native-access=ALL-UNNAMED" })
@State(Scope.Thread)
public class IoUringVsBufferedIoBench
{
    private static final int MAX_CHUNK = 65536;
    private static final int ALIGN = 4096;                 // O_DIRECT offset/length/buffer alignment
    private static final long FILE_SIZE = 512L * 1024 * 1024;
    // Must be on a real device (btrfs/NVMe here), never tmpfs, or the COLD regime would read from RAM.
    private static final Path DATA_FILE = Path.of("/home/cscotta/projects/cassandra/io_uring_bench/data.bin");
    private static final int RANDOM_OFFSETS = 8192;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle OPEN = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("open"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT),
        Linker.Option.firstVariadicArg(2));
    private static final MethodHandle CLOSE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("close"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MethodHandle POSIX_FADVISE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("posix_fadvise"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT));
    private static final int O_RDWR = 2;
    private static final int O_DIRECT = 0x10000;           // aarch64 value (x86-64 is 0x4000)
    private static final int POSIX_FADV_DONTNEED = 4;

    private static final Object FILE_LOCK = new Object();
    private static volatile boolean fileReady;

    @Param({ "WARM", "COLD" })
    public String mode;

    @Param({ "128" })
    public int qd;

    @Param({ "4096", "8192", "16384", "32768", "65536" })
    public int chunk;

    private boolean skip;
    private boolean direct;
    private int fd = -1;
    private FileChannel channel;
    private Ring ring;
    private Arena arena;
    private MemorySegment readBuf;    // qd * MAX_CHUNK, io_uring read target
    private MemorySegment writeBuf;   // qd * MAX_CHUNK, io_uring write source
    private ByteBuffer fcReadBuf;     // aligned MAX_CHUNK, reused across the FileChannel loop
    private ByteBuffer fcWriteBuf;    // aligned MAX_CHUNK, pre-filled
    private long[] randomOffsets;
    private int offIdx;
    private long seqReadPos;
    private long seqWritePos;

    @Setup(Level.Trial)
    public void setup() throws Throwable
    {
        if (!IoUringAvailability.check().isAvailable())
        {
            skip = true;
            return;
        }
        direct = "COLD".equals(mode);
        ensureDataFile();

        int flags = O_RDWR | (direct ? O_DIRECT : 0);
        try (Arena open = Arena.ofConfined())
        {
            MemorySegment cpath = open.allocateFrom(DATA_FILE.toAbsolutePath().toString());
            fd = (int) OPEN.invokeExact(cpath, flags);
            if (fd < 0)
                throw new IllegalStateException("open(flags=0x" + Integer.toHexString(flags) + ") failed on " + DATA_FILE);
            if (direct)
            {
                int ignored = (int) POSIX_FADVISE.invokeExact(fd, 0L, FILE_SIZE, POSIX_FADV_DONTNEED);
            }
        }

        OpenOption[] opts = direct
            ? new OpenOption[]{ StandardOpenOption.READ, StandardOpenOption.WRITE, ExtendedOpenOption.DIRECT }
            : new OpenOption[]{ StandardOpenOption.READ, StandardOpenOption.WRITE };
        channel = FileChannel.open(DATA_FILE, opts);

        int entries = Math.max(8, Integer.highestOneBit(qd) << 1);
        ring = Ring.open(entries);

        arena = Arena.ofShared();
        readBuf = arena.allocate((long) qd * MAX_CHUNK, ALIGN);
        writeBuf = arena.allocate((long) qd * MAX_CHUNK, ALIGN);
        for (long i = 0; i < writeBuf.byteSize(); i++)
            writeBuf.set(JAVA_BYTE, i, (byte) (i & 0x7F));
        fcReadBuf = BufferUtil.allocateDirectAligned(MAX_CHUNK, ALIGN);
        fcWriteBuf = BufferUtil.allocateDirectAligned(MAX_CHUNK, ALIGN);
        while (fcWriteBuf.hasRemaining())
            fcWriteBuf.put((byte) 0x5A);
        fcWriteBuf.clear();

        randomOffsets = new long[RANDOM_OFFSETS];
        Random rnd = new Random(20260705L);
        long span = FILE_SIZE - MAX_CHUNK;
        for (int i = 0; i < RANDOM_OFFSETS; i++)
        {
            long off = Math.floorMod(rnd.nextLong(), span);
            randomOffsets[i] = off - (off % ALIGN);      // block-aligned for O_DIRECT
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Throwable
    {
        if (ring != null)
            ring.close();
        if (arena != null)
            arena.close();
        if (channel != null)
            channel.close();
        if (fd >= 0)
        {
            int ignored = (int) CLOSE.invokeExact(fd);
            fd = -1;
        }
        // DATA_FILE is left on disk for reuse across forks; removed by the harness after the run.
    }

    private static void ensureDataFile() throws IOException
    {
        if (fileReady && Files.exists(DATA_FILE) && Files.size(DATA_FILE) == FILE_SIZE)
            return;
        synchronized (FILE_LOCK)
        {
            if (Files.exists(DATA_FILE) && Files.size(DATA_FILE) == FILE_SIZE)
            {
                fileReady = true;
                return;
            }
            Files.createDirectories(DATA_FILE.getParent());
            byte[] block = new byte[1 << 20];
            for (int i = 0; i < block.length; i++)
                block[i] = (byte) ((i * 31 + 7) & 0xFF);
            try (FileChannel out = FileChannel.open(DATA_FILE, StandardOpenOption.CREATE,
                                                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
            {
                long written = 0;
                ByteBuffer bb = ByteBuffer.wrap(block);
                while (written < FILE_SIZE)
                {
                    bb.clear();
                    out.write(bb);
                    written += block.length;
                }
                out.force(true);
            }
            fileReady = true;
        }
    }

    private long nextRandomOffset()
    {
        long off = randomOffsets[offIdx];
        offIdx = (offIdx + 1) % RANDOM_OFFSETS;
        return off;
    }

    private long nextSeqBase(boolean write)
    {
        long stride = (long) qd * chunk;
        long span = FILE_SIZE - stride;
        long pos = write ? seqWritePos : seqReadPos;
        long base = Math.floorMod(pos, span);
        base -= base % ALIGN;
        if (write)
            seqWritePos = pos + stride;
        else
            seqReadPos = pos + stride;
        return base;
    }

    private long ringRead(long baseOffset, boolean sequential)
    {
        for (int i = 0; i < qd; i++)
        {
            long off = sequential ? baseOffset + (long) i * chunk : nextRandomOffset();
            MemorySegment sqe = ring.sq().getSqe();
            Prep.prepRead(sqe, fd, readBuf.address() + (long) i * chunk, chunk, off);
            Prep.setData64(sqe, i);
        }
        RingSubmitter.submitAndWait(ring, qd);
        long[] total = { 0 };
        ring.cq().forEachCqe((userData, res, flags) -> total[0] += res, qd);
        return total[0];
    }

    // ---------------- random reads (the power-of-two sweep) ----------------

    @Benchmark
    public long randRead_ioUring()
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        return ringRead(0, false);
    }

    @Benchmark
    public long randRead_buffered() throws IOException
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        long total = 0;
        for (int i = 0; i < qd; i++)
        {
            fcReadBuf.clear();
            fcReadBuf.limit(chunk);
            total += channel.read(fcReadBuf, nextRandomOffset());
        }
        return total;
    }

    // ---------------- sequential reads ----------------

    @Benchmark
    public long seqRead_ioUring()
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        return ringRead(nextSeqBase(false), true);
    }

    @Benchmark
    public long seqRead_buffered() throws IOException
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        long base = nextSeqBase(false);
        long total = 0;
        for (int i = 0; i < qd; i++)
        {
            fcReadBuf.clear();
            fcReadBuf.limit(chunk);
            total += channel.read(fcReadBuf, base + (long) i * chunk);
        }
        return total;
    }

    // ---------------- sequential writes ----------------

    @Benchmark
    public long seqWrite_ioUring()
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        long base = nextSeqBase(true);
        for (int i = 0; i < qd; i++)
        {
            MemorySegment sqe = ring.sq().getSqe();
            Prep.prepWrite(sqe, fd, writeBuf.address() + (long) i * chunk, chunk, base + (long) i * chunk);
            Prep.setData64(sqe, i);
        }
        RingSubmitter.submitAndWait(ring, qd);
        long[] total = { 0 };
        ring.cq().forEachCqe((userData, res, flags) -> total[0] += res, qd);
        return total[0];
    }

    @Benchmark
    public long seqWrite_buffered() throws IOException
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");
        long base = nextSeqBase(true);
        long total = 0;
        for (int i = 0; i < qd; i++)
        {
            fcWriteBuf.clear();
            fcWriteBuf.limit(chunk);
            total += channel.write(fcWriteBuf, base + (long) i * chunk);
        }
        return total;
    }
}
