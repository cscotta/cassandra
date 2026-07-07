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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.quicktheories.QuickTheory.qt;

/**
 * Property-based soak of the read path: for random sequences of {@code (offset, length)} reads against a temp file,
 * driven with test-side back-pressure (never more than {@code ringEntries()} in flight) and interleaved submit/reap,
 * two invariants must hold for every generated sequence:
 * <ul>
 *   <li><b>Data:</b> each completion's byte count and payload equal {@link FileChannel#read} at that offset/length,
 *       with cross-EOF reads completing short (exactly the bytes that exist).</li>
 *   <li><b>Correlation:</b> the multiset of reaped {@code user_data} tokens equals the submitted set &mdash; no lost,
 *       no duplicated, no fabricated completions.</li>
 * </ul>
 * Self-skips on non-Linux, pre-25 JDKs, and hosts where io_uring is unavailable (Docker seccomp, gVisor, ...).
 */
public class IoUringPropertyTest
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
    private static final int FILE_SIZE = 200_000;
    private static final int RING_ENTRIES = 32;
    private static final int MAX_READ_LEN = 8192;
    private static final int MAX_OPS = 48;

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

    /** Generates a sequence of {@code {offset, length}} pairs; offsets span the file (and its EOF) to force shorts. */
    private static Gen<List<int[]>> readSequences()
    {
        Gen<Integer> offsets = SourceDSL.integers().between(0, FILE_SIZE);
        Gen<Integer> lengths = SourceDSL.integers().between(1, MAX_READ_LEN);
        Gen<int[]> reads = offsets.zip(lengths, (off, len) -> new int[]{ off, len });
        return SourceDSL.lists().of(reads).ofSizeBetween(1, MAX_OPS);
    }

    @Test
    public void randomReadSequencesMatchFileChannel() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        Path file = Files.createTempFile("iouring-property", ".bin");
        try
        {
            Files.write(file, expected);
            try (Arena fileArena = Arena.ofShared();
                 FileChannel oracle = FileChannel.open(file, StandardOpenOption.READ))
            {
                int fd = openReadOnly(fileArena, file);
                try
                {
                    qt().withExamples(50)
                        .forAll(readSequences())
                        .checkAssert(seq -> runSequence(fd, oracle, seq));
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

    /**
     * Drives one generated sequence on a fresh ring: submits reads in capacity-bounded batches, reaps interleaved,
     * and checks the data and correlation invariants. A fresh ring and arena per sequence keep examples (and shrink
     * re-runs) fully isolated. Wraps the oracle's {@link IOException} so the quicktheories consumer stays unchecked.
     */
    private static void runSequence(int fd, FileChannel oracle, List<int[]> seq)
    {
        try (Arena arena = Arena.ofConfined();
             Ring ring = Ring.open(RING_ENTRIES))
        {
            int capacity = ring.sq().ringEntries();
            Map<Long, int[]> inFlight = new HashMap<>();       // token -> {off, len}
            Map<Long, MemorySegment> bufByToken = new HashMap<>();
            Map<Long, byte[]> expectedByToken = new HashMap<>();
            Set<Long> submitted = new HashSet<>();
            List<Long> reaped = new ArrayList<>();

            long nextToken = 1;
            int idx = 0;
            int guard = 0;
            int hardCap = seq.size() * 8 + 256;
            while ((idx < seq.size() || !inFlight.isEmpty()) && guard++ < hardCap)
            {
                int batch = 0;
                while (idx < seq.size() && inFlight.size() < capacity)
                {
                    MemorySegment sqe = ring.sq().getSqe();
                    if (sqe == null)     // physical SQ full: stop filling, go reap
                        break;

                    int off = seq.get(idx)[0];
                    int len = seq.get(idx)[1];
                    int expLen = (int) Math.max(0, Math.min(len, (long) FILE_SIZE - off));
                    long token = nextToken++;

                    byte[] want = new byte[expLen];
                    if (expLen > 0)
                    {
                        ByteBuffer bb = ByteBuffer.wrap(want);
                        int r = readFully(oracle, bb, off);
                        assertEquals("oracle read length at off=" + off, expLen, r);
                    }

                    MemorySegment buf = arena.allocate(Math.max(1, len));
                    buf.fill((byte) 0);

                    Prep.prepRead(sqe, fd, buf.address(), len, off);
                    Prep.setData64(sqe, token);

                    inFlight.put(token, new int[]{ off, len });
                    bufByToken.put(token, buf);
                    expectedByToken.put(token, want);
                    submitted.add(token);
                    idx++;
                    batch++;
                }

                if (batch > 0)
                    RingSubmitter.submit(ring);

                int reapedNow = ring.cq().forEachCqe((token, res, flags) -> {
                    int[] ol = inFlight.remove(token);
                    assertNotNull("completion for unknown token " + token, ol);
                    reaped.add(token);
                    int off = ol[0];
                    int len = ol[1];
                    int expLen = (int) Math.max(0, Math.min(len, (long) FILE_SIZE - off));
                    assertEquals("bytes read at off=" + off + " len=" + len, expLen, res);
                    if (expLen > 0)
                    {
                        byte[] fromRing = bufByToken.get(token).asSlice(0, expLen).toArray(JAVA_BYTE);
                        assertArrayEquals("payload at off=" + off, expectedByToken.get(token), fromRing);
                    }
                }, capacity * 2);

                if (reapedNow == 0 && !inFlight.isEmpty())
                    RingSubmitter.submitAndWait(ring, 1);   // block for at least one to guarantee progress
            }

            assertTrue("sequence made progress within its iteration budget", inFlight.isEmpty());
            assertEquals("no completion lost or duplicated", submitted.size(), reaped.size());
            assertEquals("reaped token set equals submitted token set", submitted, new HashSet<>(reaped));
        }
        catch (IOException e)
        {
            throw new AssertionError(e);
        }
    }

    /** Reads exactly {@code bb.remaining()} bytes at {@code position} (or until EOF), returning the byte count. */
    private static int readFully(FileChannel channel, ByteBuffer bb, long position) throws IOException
    {
        int total = 0;
        long pos = position;
        while (bb.hasRemaining())
        {
            int r = channel.read(bb, pos);
            if (r <= 0)
                break;
            total += r;
            pos += r;
        }
        return total;
    }
}
