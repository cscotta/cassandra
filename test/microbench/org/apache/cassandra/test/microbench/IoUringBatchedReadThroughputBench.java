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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.LibC;
import org.apache.cassandra.io.uring.op.Prep;
import org.apache.cassandra.io.uring.probe.Capabilities;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.reg.BufferRegistrar;
import org.apache.cassandra.io.uring.ring.Ring;
import org.apache.cassandra.io.uring.ring.RingSubmitter;
import org.apache.cassandra.io.uring.ring.SubmissionQueue;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Informational throughput microbenchmark comparing io_uring batched buffered reads against a plain
 * {@link FileChannel} positional-read (pread) loop, over a matrix of queue depths and chunk sizes.
 *
 * <p>Both benchmark methods perform exactly {@code depth} reads of {@code chunkSize} bytes at random,
 * {@code chunkSize}-aligned offsets within a large (~512 MiB) temp file created in {@link #setup()} and deleted in
 * {@link #tearDown()}. {@link #ioUringBatched()} prepares {@code depth} {@code IORING_OP_READ} SQEs, submits them in
 * one {@code io_uring_enter} and waits for all {@code depth} completions before reaping them; {@link #fileChannelBaseline()}
 * issues the same {@code depth} preads sequentially. Each returns the total bytes read so the JIT cannot elide the work.
 *
 * <p>This is not a go/no-go gate. Because {@code org.junit.Assume} is not on the bench classpath, {@link #setup()}
 * records a {@code skip} flag when {@link IoUringAvailability} reports the ring unusable on the host, and each
 * {@code @Benchmark} then throws {@link IllegalStateException} so JMH surfaces the skip rather than reporting bogus numbers.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsAppend = { "--enable-native-access=ALL-UNNAMED" })
@State(Scope.Benchmark)
public class IoUringBatchedReadThroughputBench
{
    /** Size of the backing temp file; a multiple of every {@code chunkSize} so all aligned offsets read in full. */
    private static final long FILE_BYTES = 512L * 1024 * 1024;

    /** Buffer size used to lay down the temp file in {@link #setup()} (content is arbitrary for a read benchmark). */
    private static final int FILL_CHUNK = 1 << 20;

    /** {@code open(2)} flag for a read-only descriptor (no {@code O_CREAT}, so the variadic {@code mode} is never read). */
    private static final int O_RDONLY = 0;

    private static final long FILL_SEED = 0x5DEECE66DL;
    private static final long OFFSET_SEED = 0x1F0_0BA5EL;

    private static final Linker LINKER = Linker.nativeLinker();

    // int open(const char *pathname, int flags, ...) -- declared with two named args and no variadic args passed
    // (firstVariadicArg(2)), so it is only ever called without O_CREAT. Capture state is prepended, mirroring LibC.
    private static final MethodHandle OPEN = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("open"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT),
        Linker.Option.firstVariadicArg(2),
        Linker.Option.captureCallState("errno"));

    @Param({ "1", "4", "8", "16", "32", "64", "128" })
    private int depth;

    @Param({ "4096", "16384", "65536", "262144" })
    private int chunkSize;

    /** Set when io_uring is unavailable on the host; each benchmark throws so JMH surfaces the skip. */
    private boolean skip;

    private Path file;

    // io_uring side: an arena owning the errno-capture segment, the C path string and the read buffers, the raw fd,
    // the ring, and the single native region sliced into one destination buffer per in-flight read.
    private Arena arena;
    private MemorySegment cap;
    private int ringFd = -1;
    private Ring ring;
    private MemorySegment readBuffers;
    private BufferRegistrar registrar;

    // Baseline side: a positional-read channel and one direct buffer per read.
    private FileChannel channel;
    private ByteBuffer[] baselineBuffers;

    private Random offsets;

    @Setup
    public void setup() throws IOException
    {
        Capabilities caps = IoUringAvailability.check();
        if (!caps.isAvailable())
        {
            skip = true;
            return;
        }

        file = Files.createTempFile("iouring-read-bench", ".dat");
        fillFile(file, FILE_BYTES);

        // Baseline: a channel for positional reads plus one destination buffer per queued read.
        channel = FileChannel.open(file, StandardOpenOption.READ);
        baselineBuffers = new ByteBuffer[depth];
        for (int i = 0; i < depth; i++)
            baselineBuffers[i] = ByteBuffer.allocateDirect(chunkSize);

        // io_uring: a separate read-only fd (opened via libc), the ring, and pinned native destination buffers.
        arena = Arena.ofShared();
        cap = arena.allocate(Errno.CAPTURE);
        MemorySegment cPath = arena.allocateFrom(file.toString());
        ringFd = openFd(cap, cPath, O_RDONLY);
        if (ringFd < 0)
            throw new IOException("open(" + file + ") failed: " + Errno.strerror(Errno.of(cap)));

        ring = Ring.open(depth);
        readBuffers = arena.allocate((long) depth * chunkSize);

        // Pin the whole destination region once as fixed buffer index 0 so the READ_FIXED arm can skip per-I/O pinning.
        // Registration targets the ring fd (not the data-file fd) -- io_uring_register on a non-ring fd is EOPNOTSUPP.
        registrar = new BufferRegistrar(ring.fd());
        registrar.registerBuffers(new long[]{ readBuffers.address() }, new long[]{ (long) depth * chunkSize });

        offsets = new Random(OFFSET_SEED);
    }

    @TearDown
    public void tearDown() throws IOException
    {
        if (registrar != null)
            registrar.close();
        if (ring != null)
            ring.close();
        if (channel != null)
            channel.close();
        if (ringFd >= 0 && cap != null)
            LibC.close(cap, ringFd);
        if (arena != null)
            arena.close();
        if (file != null)
            Files.deleteIfExists(file);
    }

    /** Submit {@code depth} reads in one enter, wait for all of them, then reap. Returns total bytes read. */
    @Benchmark
    public long ioUringBatched()
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");

        SubmissionQueue sq = ring.sq();
        long base = readBuffers.address();
        for (int i = 0; i < depth; i++)
        {
            MemorySegment sqe = sq.getSqe();
            if (sqe == null)
                throw new IllegalStateException("SQ full at depth=" + depth + " (i=" + i + ')');
            Prep.prepRead(sqe, ringFd, base + (long) i * chunkSize, chunkSize, randomOffset());
            Prep.setData64(sqe, i);
        }

        RingSubmitter.submitAndWait(ring, depth);

        long[] bytesRead = { 0L };
        int reaped = 0;
        while (reaped < depth)
            reaped += ring.cq().forEachCqe((userData, res, flags) ->
            {
                if (res > 0)
                    bytesRead[0] += res;
            }, depth - reaped);
        return bytesRead[0];
    }

    /**
     * As {@link #ioUringBatched} but with {@code IORING_OP_READ_FIXED} into the pre-registered buffer region (index 0),
     * so the kernel skips per-I/O pinning of the destination pages. Same ring, depth, buffers and offsets, isolating
     * the registered-buffer delta that gates wiring {@code READ_FIXED} into the chunk-cache read path.
     */
    @Benchmark
    public long ioUringBatchedFixed()
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");

        SubmissionQueue sq = ring.sq();
        long base = readBuffers.address();
        for (int i = 0; i < depth; i++)
        {
            MemorySegment sqe = sq.getSqe();
            if (sqe == null)
                throw new IllegalStateException("SQ full at depth=" + depth + " (i=" + i + ')');
            Prep.prepReadFixed(sqe, ringFd, base + (long) i * chunkSize, chunkSize, randomOffset(), 0);
            Prep.setData64(sqe, i);
        }

        RingSubmitter.submitAndWait(ring, depth);

        long[] bytesRead = { 0L };
        int reaped = 0;
        while (reaped < depth)
            reaped += ring.cq().forEachCqe((userData, res, flags) ->
            {
                if (res > 0)
                    bytesRead[0] += res;
            }, depth - reaped);
        return bytesRead[0];
    }

    /** Issue {@code depth} positional reads (pread) sequentially. Returns total bytes read. */
    @Benchmark
    public long fileChannelBaseline() throws IOException
    {
        if (skip)
            throw new IllegalStateException("io_uring unavailable");

        long bytesRead = 0;
        for (int i = 0; i < depth; i++)
        {
            ByteBuffer buf = baselineBuffers[i];
            buf.clear();
            int n = channel.read(buf, randomOffset());
            if (n > 0)
                bytesRead += n;
        }
        return bytesRead;
    }

    /** A random, {@code chunkSize}-aligned start offset in {@code [0, FILE_BYTES - chunkSize]}. */
    private long randomOffset()
    {
        long maxStart = FILE_BYTES - chunkSize;
        long start = (offsets.nextLong() & Long.MAX_VALUE) % (maxStart + 1);
        return start & ~((long) chunkSize - 1);   // chunk sizes are powers of two, so this aligns down
    }

    private static int openFd(MemorySegment cap, MemorySegment cPath, int flags)
    {
        try
        {
            return (int) OPEN.invokeExact(cap, cPath, flags);
        }
        catch (Throwable t)
        {
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof Error)
                throw (Error) t;
            throw new RuntimeException(t);
        }
    }

    private static void fillFile(Path path, long bytes) throws IOException
    {
        byte[] chunk = new byte[FILL_CHUNK];
        new Random(FILL_SEED).nextBytes(chunk);
        ByteBuffer buf = ByteBuffer.wrap(chunk);
        try (FileChannel out = FileChannel.open(path, StandardOpenOption.WRITE,
                                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        {
            long written = 0;
            while (written < bytes)
            {
                buf.clear();
                if (bytes - written < buf.capacity())
                    buf.limit((int) (bytes - written));
                int toWrite = buf.remaining();
                while (buf.hasRemaining())
                    out.write(buf);
                written += toWrite;
            }
            out.force(true);
        }
    }
}
