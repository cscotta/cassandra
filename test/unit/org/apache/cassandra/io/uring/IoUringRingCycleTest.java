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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Ring-mechanics correctness against the live kernel: SQ back-pressure ({@code getSqe} returns {@code null} once the
 * ring is physically full), the {@code sqSpaceLeft()} / {@code sqReady()} / {@code cqReady()} counters across a full
 * submit/reap cycle, and the short-read/resubmit contract (a cross-EOF READ returns fewer bytes than requested, and
 * resubmitting the remainder at {@code off + res} reassembles a buffer byte-for-byte equal to {@link FileChannel}).
 * Self-skips on non-Linux, pre-25 JDKs, and hosts where io_uring is unavailable (Docker seccomp, gVisor, ...).
 */
public class IoUringRingCycleTest
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

    /**
     * Reaps exactly {@code want} completions, tolerating a kernel that posts them across several {@code enter}s. The
     * completions are known to be in flight, so this always makes progress; the guard only defends against a bug.
     */
    private static void reapExactly(Ring ring, int want)
    {
        int total = 0;
        int guard = 0;
        while (total < want && guard++ < 1024)
        {
            total += ring.cq().forEachCqe((userData, res, flags) -> { }, want);
            if (total < want)
                RingSubmitter.submitAndWait(ring, want - total);
        }
        assertEquals("reaped every outstanding completion", want, total);
    }

    /**
     * {@code getSqe()} must hand out exactly {@code ringEntries()} slices and then return {@code null}: the app-owned
     * tail cannot outrun the kernel-owned head by more than the ring depth. Draining the ring must free the slots.
     */
    @Test
    public void getSqeReturnsNullWhenSqFull()
    {
        assumeAvailable();
        try (Ring ring = Ring.open(8))
        {
            int entries = ring.sq().ringEntries();
            assertTrue("granted at least the requested depth", entries >= 8);
            assertEquals("fresh ring is empty", entries, ring.sq().sqSpaceLeft());

            for (int i = 0; i < entries; i++)
            {
                MemorySegment sqe = ring.sq().getSqe();
                assertNotNull("SQE " + i + " of " + entries + " must be available", sqe);
                Prep.prepNop(sqe);
                Prep.setData64(sqe, 0x100L + i);
            }

            assertNull("ring full: getSqe must return null", ring.sq().getSqe());
            assertEquals("no free slots when full", 0, ring.sq().sqSpaceLeft());
            assertEquals("all prepared SQEs are ready to submit", entries, ring.sq().sqReady());

            int submitted = RingSubmitter.submitAndWait(ring, entries);
            assertEquals("kernel consumed the whole batch", entries, submitted);
            reapExactly(ring, entries);

            // Once the kernel has consumed the batch the slots are reusable again.
            MemorySegment sqe = ring.sq().getSqe();
            assertNotNull("SQE available again after draining", sqe);
            assertEquals("one slot now in use", entries - 1, ring.sq().sqSpaceLeft());
        }
    }

    /**
     * The three public counters must track a submit/reap cycle exactly: preparing SQEs consumes SQ space and grows
     * {@code sqReady()}; a blocking submit hands them all to the kernel (SQ empties) and posts the completions
     * ({@code cqReady()} grows); reaping drains the CQ back to zero.
     */
    @Test
    public void queueCountersTrackSubmitReapCycle()
    {
        assumeAvailable();
        try (Ring ring = Ring.open(16))
        {
            int entries = ring.sq().ringEntries();
            assertEquals("SQ starts empty", entries, ring.sq().sqSpaceLeft());
            assertEquals("nothing ready to submit", 0, ring.sq().sqReady());
            assertEquals("nothing to reap", 0, ring.cq().cqReady());

            int k = Math.min(5, entries);
            for (int i = 0; i < k; i++)
            {
                MemorySegment sqe = ring.sq().getSqe();
                assertNotNull(sqe);
                Prep.prepNop(sqe);
                Prep.setData64(sqe, 0x2000L + i);
            }

            assertEquals("k SQEs prepared but not yet consumed", k, ring.sq().sqReady());
            assertEquals("SQ space shrank by k", entries - k, ring.sq().sqSpaceLeft());
            assertEquals("no completions before submit", 0, ring.cq().cqReady());

            int submitted = RingSubmitter.submitAndWait(ring, k);
            assertEquals("submit consumed k", k, submitted);
            assertEquals("kernel drained the SQ", 0, ring.sq().sqReady());
            assertEquals("SQ space fully restored", entries, ring.sq().sqSpaceLeft());
            assertEquals("k completions now waiting", k, ring.cq().cqReady());

            int[] seen = { 0 };
            int reaped = ring.cq().forEachCqe((userData, res, flags) -> seen[0]++, entries);
            assertEquals("reaped k in one pass", k, reaped);
            assertEquals("consumer saw k", k, seen[0]);
            assertEquals("CQ drained", 0, ring.cq().cqReady());
        }
    }

    /**
     * A READ whose length runs past EOF completes short ({@code res < len}). Resubmitting the remainder at
     * {@code off + res} until the kernel returns 0 (true EOF) must reassemble a buffer byte-for-byte equal to the
     * corresponding file region read through {@link FileChannel}.
     */
    @Test
    public void shortReadResubmitReassemblesFile() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-shortread", ".bin");
        try
        {
            Files.write(file, expected);
            long off = FILE_SIZE - 5000;              // start near, but not at, EOF
            int reqLen = 20000;                       // extends 15000 bytes past EOF
            int expLen = (int) (FILE_SIZE - off);     // the bytes that actually exist

            try (Arena arena = Arena.ofShared();
                 FileChannel oracle = FileChannel.open(file, StandardOpenOption.READ);
                 Ring ring = Ring.open(16))
            {
                int fd = openReadOnly(arena, file);
                try
                {
                    MemorySegment buf = arena.allocate(reqLen);
                    buf.fill((byte) 0);

                    int totalRead = 0;
                    int firstRes = -1;
                    int iterations = 0;
                    while (totalRead < reqLen && iterations++ < 64)
                    {
                        MemorySegment sqe = ring.sq().getSqe();
                        assertNotNull(sqe);
                        Prep.prepRead(sqe, fd, buf.address() + totalRead, reqLen - totalRead, off + totalRead);
                        Prep.setData64(sqe, 0xE0F0_0000L | totalRead);
                        RingSubmitter.submitAndWait(ring, 1);

                        int[] gotRes = { Integer.MIN_VALUE };
                        int reaped = ring.cq().forEachCqe((userData, res, flags) -> gotRes[0] = res, 16);
                        assertEquals("exactly one completion per resubmit", 1, reaped);

                        int r = gotRes[0];
                        assertTrue("READ must not error, got res=" + r, r >= 0);
                        if (firstRes < 0)
                            firstRes = r;
                        if (r == 0)          // kernel signalled EOF: nothing more to assemble
                            break;
                        totalRead += r;
                    }

                    assertTrue("first read must be short (res < requested)", firstRes >= 0 && firstRes < reqLen);
                    assertEquals("assembled exactly the available bytes", expLen, totalRead);

                    byte[] fromRing = buf.asSlice(0, expLen).toArray(JAVA_BYTE);
                    ByteBuffer bb = ByteBuffer.allocate(expLen);
                    int r = oracle.read(bb, off);
                    assertEquals("FileChannel oracle read length", expLen, r);
                    assertArrayEquals("reassembled short read vs FileChannel", bb.array(), fromRing);
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
}
