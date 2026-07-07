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

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.apache.cassandra.io.uring.abi.SqCqFlags;
import org.apache.cassandra.io.uring.op.Prep;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Layer-1 fault-injection tests: drive the {@link FakeIoUringRing} through the real {@link RingSubmitter} +
 * {@link Prep} + {@link SubmissionQueue}/{@link CompletionQueue} with no kernel, asserting the submit/reap contract
 * under injected short reads, errors, EOF, out-of-order completions, and CQ overflow. Runs on any JDK 25 host
 * (Linux or not), needing only FFM native access for the fake's oracle byte copy into a direct buffer.
 */
public class FakeRingFaultTest
{
    private static final int FD = 7;
    private static final int ENTRIES = 8;

    private static byte[] pattern(int n)
    {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) ((i * 31 + 7) & 0xFF);
        return b;
    }

    /** Preps one READ into {@code buf} at {@code off} with {@code userData}, submits, and returns its completion res. */
    private static int readOnce(FakeIoUringRing ring, ByteBuffer buf, long off, long userData)
    {
        buf.clear();
        MemorySegment sqe = ring.sq().getSqe();
        assertTrue("SQ unexpectedly full", sqe != null);
        Prep.prepRead(sqe, FD, MemorySegment.ofBuffer(buf).address(), buf.remaining(), off);
        Prep.setData64(sqe, userData);
        RingSubmitter.submitAndWait(ring, 1);

        long[] seen = { -1L };
        int[] res = { Integer.MIN_VALUE };
        int n = ring.cq().forEachCqe((u, r, f) -> { seen[0] = u; res[0] = r; }, ENTRIES * 2);
        assertEquals("exactly one completion", 1, n);
        assertEquals("user_data correlation", userData, seen[0]);
        return res[0];
    }

    @Test
    public void oracleReadMatchesMappedFile()
    {
        byte[] file = pattern(4096);
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, file);
        try
        {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            for (long off : new long[]{ 0, 1024, 4096 - 100 })
            {
                int got = readOnce(ring, buf, off, 100 + off);
                int exp = (int) Math.min(1024, file.length - off);
                assertEquals("res at off=" + off, exp, got);
                byte[] out = new byte[exp];
                buf.position(0);
                buf.get(out, 0, exp);
                assertArrayEquals("bytes at off=" + off, Arrays.copyOfRange(file, (int) off, (int) off + exp), out);
            }
        }
        finally
        {
            ring.close();
        }
    }

    @Test
    public void shortReadCompletesWithInjectedCount()
    {
        byte[] file = pattern(4096);
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, file);
        try
        {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            ring.shortReadNext(300);
            assertEquals("short read res", 300, readOnce(ring, buf, 0, 1));
            byte[] out = new byte[300];
            buf.position(0);
            buf.get(out, 0, 300);
            assertArrayEquals("short-read bytes", Arrays.copyOfRange(file, 0, 300), out);
            // next read is a normal full read
            assertEquals("subsequent full read", 1024, readOnce(ring, buf, 0, 2));
        }
        finally
        {
            ring.close();
        }
    }

    @Test
    public void errorCompletionPropagatesNegativeRes()
    {
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, pattern(4096));
        try
        {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            ring.errorNext(5);   // EIO
            assertEquals("errored res", -5, readOnce(ring, buf, 0, 1));
        }
        finally
        {
            ring.close();
        }
    }

    @Test
    public void eofCompletionReturnsZero()
    {
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, pattern(4096));
        try
        {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            ring.eofNext();
            assertEquals("eof res", 0, readOnce(ring, buf, 0, 1));
        }
        finally
        {
            ring.close();
        }
    }

    @Test
    public void deferredCompletionsDeliverOutOfOrder()
    {
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, pattern(4096));
        try
        {
            ByteBuffer a = ByteBuffer.allocateDirect(512);
            ByteBuffer b = ByteBuffer.allocateDirect(512);
            ring.deferNext();
            ring.deferNext();
            MemorySegment s1 = ring.sq().getSqe();
            Prep.prepRead(s1, FD, MemorySegment.ofBuffer(a).address(), a.remaining(), 0);
            Prep.setData64(s1, 111);
            MemorySegment s2 = ring.sq().getSqe();
            Prep.prepRead(s2, FD, MemorySegment.ofBuffer(b).address(), b.remaining(), 0);
            Prep.setData64(s2, 222);
            RingSubmitter.submitAndWait(ring, 2);
            assertEquals("no completions while deferred", 0, ring.cq().cqReady());

            ring.postCqe(222, 10, 0);   // complete the second op first
            ring.postCqe(111, 20, 0);
            List<Long> order = new ArrayList<>();
            List<Integer> reses = new ArrayList<>();
            ring.cq().forEachCqe((u, r, f) -> { order.add(u); reses.add(r); }, ENTRIES * 2);
            assertEquals("out-of-order user_data", Arrays.asList(222L, 111L), order);
            assertEquals("out-of-order res", Arrays.asList(10, 20), reses);
        }
        finally
        {
            ring.close();
        }
    }

    @Test
    public void cqOverflowSetsFlagThenDrainsOnGetEvents()
    {
        byte[] file = pattern(4096);
        FakeIoUringRing ring = FakeIoUringRing.open(ENTRIES).mapFile(FD, file);
        try
        {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            ring.injectCqOverflow();

            buf.clear();
            MemorySegment sqe = ring.sq().getSqe();
            Prep.prepRead(sqe, FD, MemorySegment.ofBuffer(buf).address(), buf.remaining(), 0);
            Prep.setData64(sqe, 42);
            RingSubmitter.submitAndWait(ring, 1);

            assertTrue("CQ_OVERFLOW set", (ring.sqFlags() & SqCqFlags.CQ_OVERFLOW) != 0);
            assertEquals("nothing reapable while overflowed", 0, ring.cq().cqReady());

            // A GETEVENTS enter (which RingSubmitter forces when CQ_OVERFLOW is set) flushes the backlog.
            RingSubmitter.submitAndReapNonBlocking(ring);
            assertEquals("CQ_OVERFLOW cleared", 0, ring.sqFlags() & SqCqFlags.CQ_OVERFLOW);

            long[] seen = { -1L };
            int[] res = { Integer.MIN_VALUE };
            int n = ring.cq().forEachCqe((u, r, f) -> { seen[0] = u; res[0] = r; }, ENTRIES * 2);
            assertEquals("drained one completion", 1, n);
            assertEquals("drained user_data", 42L, seen[0]);
            assertEquals("drained res", 1024, res[0]);
        }
        finally
        {
            ring.close();
        }
    }
}
