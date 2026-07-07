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
package org.apache.cassandra.io.uring;

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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.apache.cassandra.io.uring.op.Prep;
import org.apache.cassandra.io.uring.probe.Capabilities;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.ring.Ring;
import org.apache.cassandra.io.uring.ring.RingSubmitter;
import org.apache.cassandra.utils.FBUtilities;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The golden-oracle correctness gate for the io_uring binding: byte-for-byte equality of an {@code IORING_OP_READ}
 * against {@link FileChannel#read} across a spread of offsets, lengths, a cross-EOF short read and an at-EOF read,
 * plus a batched submission reaped out of order (proving {@code user_data} correlation and CQ accounting). Self-skips
 * on non-Linux, pre-25 JDKs, and hosts where io_uring is unavailable (Docker seccomp, hardened images, gVisor, ...).
 */
public class IoUringGoldenReadTest
{
    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle OPEN = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("open"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT),
        Linker.Option.firstVariadicArg(2));
    private static final MethodHandle CLOSE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("close"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final int O_RDONLY = 0;
    private static final int FILE_SIZE = 300_000;

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

    private static int openReadOnly(Arena arena, Path path)
    {
        MemorySegment cpath = arena.allocateFrom(path.toAbsolutePath().toString());
        int fd;
        try
        {
            fd = (int) OPEN.invokeExact(cpath, O_RDONLY);
        }
        catch (Throwable t)
        {
            throw new AssertionError(t);
        }
        assertTrue("open() failed", fd >= 0);
        return fd;
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

    @Test
    public void goldenReadsMatchFileChannel() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-golden", ".bin");
        try
        {
            Files.write(file, expected);
            long[][] cases = {
                { 0, 4096 }, { 4096, 8192 }, { 1234, 4096 }, { 65536, 131072 },
                { FILE_SIZE - 100, 4096 }, { FILE_SIZE, 4096 }
            };

            try (Arena arena = Arena.ofShared();
                 FileChannel oracle = FileChannel.open(file, StandardOpenOption.READ);
                 Ring ring = Ring.open(16))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    for (long[] c : cases)
                    {
                        long off = c[0];
                        int len = (int) c[1];
                        int expLen = (int) Math.max(0, Math.min(len, FILE_SIZE - off));

                        MemorySegment buf = arena.allocate(len);
                        buf.fill((byte) 0);
                        long token = 0xABCD_0000L | off;

                        MemorySegment sqe = ring.sq().getSqe();
                        assertNotNull("SQE should be available", sqe);
                        Prep.prepRead(sqe, fd, buf.address(), len, off);
                        Prep.setData64(sqe, token);
                        RingSubmitter.submitAndWait(ring, 1);

                        long[] gotToken = { -1 };
                        int[] gotRes = { Integer.MIN_VALUE };
                        int reaped = ring.cq().forEachCqe((ud, res, flags) -> {
                            gotToken[0] = ud;
                            gotRes[0] = res;
                        }, 16);

                        assertEquals("exactly one completion", 1, reaped);
                        assertEquals("user_data echoed", token, gotToken[0]);
                        assertEquals("bytes read at off=" + off, expLen, gotRes[0]);

                        if (expLen > 0)
                        {
                            byte[] fromRing = buf.asSlice(0, expLen).toArray(JAVA_BYTE);
                            ByteBuffer bb = ByteBuffer.allocate(expLen);
                            int r = oracle.read(bb, off);
                            assertEquals("FileChannel oracle read length", expLen, r);
                            assertArrayEquals("ring READ vs FileChannel at off=" + off, bb.array(), fromRing);
                        }
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
    public void batchedReadsReapOutOfOrder() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-batch", ".bin");
        try
        {
            Files.write(file, expected);
            int chunk = 4096;
            int nReads = 12;

            try (Arena arena = Arena.ofShared();
                 Ring ring = Ring.open(64))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    Map<Long, Long> tokenToOffset = new HashMap<>();
                    Map<Long, MemorySegment> tokenToBuf = new HashMap<>();
                    for (int i = 0; i < nReads; i++)
                    {
                        long off = (long) i * chunk * 3;   // spread across the file
                        long token = 0x5000_0000L + i;
                        MemorySegment buf = arena.allocate(chunk);
                        buf.fill((byte) 0);
                        tokenToOffset.put(token, off);
                        tokenToBuf.put(token, buf);

                        MemorySegment sqe = ring.sq().getSqe();
                        assertNotNull(sqe);
                        Prep.prepRead(sqe, fd, buf.address(), chunk, off);
                        Prep.setData64(sqe, token);
                    }

                    RingSubmitter.submitAndWait(ring, nReads);

                    int[] reaped = { 0 };
                    ring.cq().forEachCqe((token, res, flags) -> {
                        reaped[0]++;
                        Long off = tokenToOffset.get(token);
                        assertNotNull("unknown token " + token, off);
                        int expLen = (int) Math.min(chunk, FILE_SIZE - off);
                        assertEquals("bytes at off=" + off, expLen, res);
                        byte[] fromRing = tokenToBuf.get(token).asSlice(0, expLen).toArray(JAVA_BYTE);
                        for (int i = 0; i < expLen; i++)
                            assertEquals("byte at off=" + off + " i=" + i, expected[(int) (off + i)], fromRing[i]);
                    }, 64);

                    assertEquals("all completions reaped", nReads, reaped[0]);
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
    public void availabilityIsCachedAndConsistent()
    {
        assumeTrue("Linux only", FBUtilities.isLinux);
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        Capabilities a = IoUringAvailability.check();
        Capabilities b = IoUringAvailability.check();
        assertSame("availability result must be cached", a, b);
        if (a.isAvailable())
            assertTrue("READ opcode must be supported at the 5.6 floor", a.supports(22));
    }
}
