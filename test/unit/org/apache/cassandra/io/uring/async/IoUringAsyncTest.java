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
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture; // checkstyle: permit this import
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.apache.cassandra.io.uring.probe.Capabilities;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.spi.IoUringConfig;
import org.apache.cassandra.utils.FBUtilities;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * End-to-end tests for the P1 async facade against the live kernel: golden-oracle async reads via {@code future.get()},
 * foreign-thread (MPSC + eventfd wakeup) submission, backpressure recycling, short-read/EOF handling, and
 * drain-before-close with an fd-leak check. Self-skips on non-Linux, pre-25 JDKs, and hosts without io_uring.
 */
public class IoUringAsyncTest
{
    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle OPEN = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("open"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), Linker.Option.firstVariadicArg(2));
    private static final MethodHandle CLOSE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("close"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final int O_RDONLY = 0;
    private static final int FILE_SIZE = 400_000;

    private static void assumeAvailable()
    {
        assumeTrue("Linux only", FBUtilities.isLinux);
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        Capabilities caps = IoUringAvailability.check();
        assumeTrue("io_uring unavailable: " + caps.reason(), caps.isAvailable());
    }

    private static byte[] pattern(int n)
    {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) ((i * 31 + 7) & 0xFF);
        return b;
    }

    private static int openReadOnly(Arena arena, Path p)
    {
        try
        {
            int fd = (int) OPEN.invokeExact(arena.allocateFrom(p.toAbsolutePath().toString()), O_RDONLY);
            assertTrue("open() failed", fd >= 0);
            return fd;
        }
        catch (Throwable t)
        {
            throw new AssertionError(t);
        }
    }

    private static void closeFd(int fd)
    {
        try
        {
            int ignored = (int) CLOSE.invokeExact(fd);
        }
        catch (Throwable t)
        {
            throw new AssertionError(t);
        }
    }

    private static byte[] toArray(ByteBuffer dst, int len)
    {
        byte[] out = new byte[len];
        dst.position(0);
        dst.get(out, 0, len);
        return out;
    }

    @Test
    public void asyncReadsMatchFileChannel() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-async", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared();
                 FileChannel oracle = FileChannel.open(file, StandardOpenOption.READ);
                 IoUring uring = IoUring.open(IoUringConfig.defaults()))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    long[][] cases = { { 0, 4096 }, { 4096, 65536 }, { 1234, 8192 }, { FILE_SIZE - 500, 4096 } };
                    for (long[] c : cases)
                    {
                        long off = c[0];
                        int len = (int) c[1];
                        int expLen = (int) Math.min(len, FILE_SIZE - off);
                        ByteBuffer dst = ByteBuffer.allocateDirect(len);
                        int got = uring.read(fd, off, dst).get();
                        assertEquals("bytes read at off=" + off, expLen, got);
                        ByteBuffer bb = ByteBuffer.allocate(expLen);
                        oracle.read(bb, off);
                        assertArrayEquals("async read vs FileChannel at off=" + off, bb.array(), toArray(dst, expLen));
                    }
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void shortReadPastEofCompletesWithAvailableBytes() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-eof", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared();
                 IoUring uring = IoUring.open(IoUringConfig.defaults()))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    long off = FILE_SIZE - 1000;   // request 64 KiB but only 1000 bytes remain -> short read then EOF
                    ByteBuffer dst = ByteBuffer.allocateDirect(65536);
                    int got = uring.read(fd, off, dst).get();
                    assertEquals(1000, got);
                    byte[] tail = new byte[1000];
                    System.arraycopy(expected, (int) off, tail, 0, 1000);
                    assertArrayEquals(tail, toArray(dst, 1000));
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void foreignThreadsAndBackpressure() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-mt", ".bin");
        try
        {
            Files.write(file, expected);
            IoUringConfig cfg = IoUringConfig.builder().queueDepth(8).pollerThreads(2).build();  // tiny queue forces backpressure
            try (Arena arena = Arena.ofShared();
                 IoUring uring = IoUring.open(cfg))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    int threads = 4;
                    int perThread = 64;
                    int chunk = 4096;
                    List<Thread> workers = new ArrayList<>();
                    boolean[] ok = new boolean[threads];
                    for (int t = 0; t < threads; t++)
                    {
                        int id = t;
                        Thread w = new Thread(() -> {
                            try
                            {
                                for (int i = 0; i < perThread; i++)
                                {
                                    long off = (long) ((id * perThread + i) % 90) * chunk;   // within file
                                    ByteBuffer dst = ByteBuffer.allocateDirect(chunk);
                                    int got = uring.read(fd, off, dst).get();
                                    if (got != chunk)
                                        return;
                                    for (int b = 0; b < chunk; b++)
                                        if (dst.get(b) != expected[(int) off + b])
                                            return;
                                }
                                ok[id] = true;
                            }
                            catch (Exception e)
                            {
                                ok[id] = false;
                            }
                        }, "submitter-" + t);
                        workers.add(w);
                        w.start();
                    }
                    for (Thread w : workers)
                        w.join(60_000);
                    for (int t = 0; t < threads; t++)
                        assertTrue("worker " + t + " did not complete all reads correctly", ok[t]);
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void drainOnCloseAndRejectAfterClose() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-drain", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared())
            {
                int fd = openReadOnly(arena, file);
                List<CompletableFuture<Integer>> futures = new ArrayList<>();
                IoUring uring = IoUring.open(IoUringConfig.defaults());
                for (int i = 0; i < 64; i++)
                {
                    ByteBuffer dst = ByteBuffer.allocateDirect(4096);
                    futures.add(uring.read(fd, (long) i * 4096, dst));
                }
                uring.close();   // must drain the in-flight ops before returning

                for (int i = 0; i < futures.size(); i++)
                    assertEquals("in-flight op " + i + " should have drained on close", 4096, (int) futures.get(i).get());

                // A submission after close must fail fast, not hang.
                try
                {
                    ByteBuffer dst = ByteBuffer.allocateDirect(4096);
                    uring.read(fd, 0, dst).get();
                    fail("read after close should fail");
                }
                catch (Exception expectedFailure)
                {
                    // expected
                }
                closeFd(fd);
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void repeatedOpenCloseDoesNotLeakFds() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-leak", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared())
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    // Warm up one cycle so any one-time fds are established, then measure across cycles.
                    cycle(fd);
                    int baseline = openFdCount();
                    for (int i = 0; i < 4; i++)
                        cycle(fd);
                    int after = openFdCount();
                    assertTrue("fd count grew from " + baseline + " to " + after + " across open/close cycles",
                               after <= baseline);
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    private static void cycle(int fd) throws Exception
    {
        try (IoUring uring = IoUring.open(IoUringConfig.defaults()))
        {
            ByteBuffer dst = ByteBuffer.allocateDirect(4096);
            int got = uring.read(fd, 0, dst).get();
            assertEquals(4096, got);
        }
    }

    @Test
    public void closeUnderConcurrentSubmitsDoesNotHangOrLeak() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-closerace", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared())
            {
                int fd = openReadOnly(arena, file);
                // Small queue depth so submitters block in backpressure.acquire() and exercise the close/quiesce path.
                IoUring uring = IoUring.open(IoUringConfig.builder().queueDepth(8).pollerThreads(2).build());
                List<CompletableFuture<Integer>> all = Collections.synchronizedList(new ArrayList<>());
                AtomicBoolean stop = new AtomicBoolean();

                List<Thread> submitters = new ArrayList<>();
                for (int t = 0; t < 4; t++)
                {
                    Thread w = new Thread(() -> {
                        for (int i = 0; i < 500 && !stop.get(); i++)
                        {
                            try
                            {
                                ByteBuffer dst = ByteBuffer.allocateDirect(4096);
                                all.add(uring.read(fd, (long) (i % 90) * 4096, dst));
                            }
                            catch (RuntimeException e)
                            {
                                // submission may throw synchronously once closed; keep going until stop
                            }
                        }
                    }, "race-submitter-" + t);
                    submitters.add(w);
                    w.start();
                }

                Thread.sleep(50);        // let real submissions get in flight, concurrent with close
                uring.close();            // must return (drain-before-close) despite concurrent submits
                stop.set(true);
                for (Thread w : submitters)
                    w.join(30_000);

                // Every future handed out must resolve (completed or exceptionally) -- no op may hang.
                for (CompletableFuture<Integer> f : all)
                {
                    try
                    {
                        f.get(10, java.util.concurrent.TimeUnit.SECONDS);   // completed normally (drained in-flight)
                    }
                    catch (java.util.concurrent.ExecutionException expectedAfterClose)
                    {
                        // failed fast because the loop was closing -- acceptable
                    }
                    assertTrue("a submitted future never resolved (hang)", f.isDone());
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void readFixedThroughRegisteredBuffer() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-readfixed", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared();
                 IoUring uring = IoUring.open(IoUringConfig.defaults()))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    int chunk = 8192;
                    ByteBuffer buf = ByteBuffer.allocateDirect(chunk);
                    uring.registerBuffers(new ByteBuffer[]{ buf.clear() });   // buf_index 0 == this buffer

                    long[] offsets = { 0, chunk, 3L * chunk, FILE_SIZE - 100 };
                    for (long off : offsets)
                    {
                        buf.clear();
                        int got = uring.readFixed(fd, off, buf, 0).get();
                        int expLen = (int) Math.min(chunk, FILE_SIZE - off);
                        assertEquals("READ_FIXED bytes at off=" + off, expLen, got);
                        for (int i = 0; i < expLen; i++)
                            assertEquals("byte at off=" + off + " i=" + i, expected[(int) off + i], buf.get(i));
                    }
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void readFixedThroughSparseRegisteredSlab() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-sparse", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena arena = Arena.ofShared();
                 IoUring uring = IoUring.open(IoUringConfig.defaults()))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    int slab = 65536;
                    int chunk = 8192;
                    ByteBuffer slabBuf = ByteBuffer.allocateDirect(slab);
                    long slabAddr = java.lang.foreign.MemorySegment.ofBuffer(slabBuf).address();

                    uring.registerBuffersSparse(4);                 // reserve 4 empty slots
                    uring.registerBuffersUpdate(2, slabAddr, slab);  // fill slot 2 with the whole slab

                    // READ_FIXED into a sub-region inside the slab, addressed by buf_index 2.
                    int subOffset = 3 * chunk;
                    long[] offsets = { 0, chunk, 5L * chunk, FILE_SIZE - 100 };
                    for (long off : offsets)
                    {
                        slabBuf.clear().position(subOffset);
                        ByteBuffer sub = slabBuf.slice();
                        sub.limit(chunk);
                        int got = uring.readFixed(fd, off, sub, 2).get();
                        int expLen = (int) Math.min(chunk, FILE_SIZE - off);
                        assertEquals("sparse READ_FIXED bytes at off=" + off, expLen, got);
                        for (int i = 0; i < expLen; i++)
                            assertEquals("byte at off=" + off + " i=" + i, expected[(int) off + i], sub.get(i));
                    }
                }
                finally
                {
                    closeFd(fd);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    private static int openFdCount()
    {
        try (java.util.stream.Stream<Path> fds = Files.list(Path.of("/proc/self/fd")))
        {
            return (int) fds.count();
        }
        catch (Exception e)
        {
            return -1;
        }
    }
}
