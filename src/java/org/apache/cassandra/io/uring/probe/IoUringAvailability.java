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
package org.apache.cassandra.io.uring.probe;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.RegisterOps;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.Syscalls;
import org.apache.cassandra.io.uring.op.Prep;
import org.apache.cassandra.io.uring.ring.Ring;
import org.apache.cassandra.io.uring.ring.RingSubmitter;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * The hardened availability probe. io_uring is the most-restricted-in-practice Linux subsystem (Docker seccomp
 * returns {@code EPERM}; {@code kernel.io_uring_disabled} blocks it on hardened images; gVisor allows setup but
 * fails ops; systemd {@code SystemCallFilter} may SIGSYS the process), so graceful fallback is the primary path.
 *
 * <p>{@link #check()} sets up a tiny ring, records {@code params.features}, probes supported opcodes via
 * {@code IORING_REGISTER_PROBE}, and <em>exercises a real op</em> (a {@code NOP} reaped from the CQ &mdash; gVisor
 * passes setup but fails ops). Any failure is caught and reported as unavailable; the result is cached once and
 * never re-probed. The caller (Cassandra's {@code StartupChecks}) is responsible for the OS/kernel-floor gate and,
 * where a KILL seccomp policy is a risk, running the first probe in a short-lived forked helper. This class itself
 * does not touch {@code System} properties or the filesystem, keeping the core dependency-free.
 */
public final class IoUringAvailability
{
    private static final int PROBE_ENTRIES = 8;
    private static final long NOP_TOKEN = 0x1F0_0BA5EL;
    private static final int PROBE_MAX_OPS = 256;

    /** Opcodes guaranteed at the 5.6 kernel floor, used when REGISTER_PROBE itself is unavailable. */
    private static final long FLOOR_OPCODES =
        (1L << Opcodes.NOP) | (1L << Opcodes.READV) | (1L << Opcodes.WRITEV) | (1L << Opcodes.FSYNC)
        | (1L << Opcodes.READ_FIXED) | (1L << Opcodes.WRITE_FIXED) | (1L << Opcodes.READ) | (1L << Opcodes.WRITE);

    private static final Object LOCK = new Object();
    private static volatile Capabilities cached;

    private IoUringAvailability() {}

    /** Probes once (thread-safe) and caches the result. Never re-probes. Never throws for a runtime unavailability. */
    public static Capabilities check()
    {
        Capabilities c = cached;
        if (c != null)
            return c;
        synchronized (LOCK)
        {
            if (cached == null)
                cached = probe();
            return cached;
        }
    }

    private static Capabilities probe()
    {
        // A layout/padding bug is a hard error, not a runtime unavailability, so verify outside the catch.
        Layouts.verify();
        try (Ring ring = Ring.open(PROBE_ENTRIES, 0))
        {
            int features = ring.params().features();
            long supported = probeOpcodes(ring.fd());

            MemorySegment sqe = ring.sq().getSqe();
            if (sqe == null)
                return Capabilities.unavailable("could not acquire an SQE on a fresh ring");
            Prep.prepNop(sqe);
            Prep.setData64(sqe, NOP_TOKEN);
            RingSubmitter.submitAndWait(ring, 1);

            boolean[] ok = { false };
            ring.cq().forEachCqe((userData, res, flags) -> {
                if (userData == NOP_TOKEN && res >= 0)
                    ok[0] = true;
            }, PROBE_ENTRIES);

            if (!ok[0])
                return Capabilities.unavailable("NOP submitted but no successful completion was reaped");
            return Capabilities.available(features, supported);
        }
        catch (Throwable t)
        {
            return Capabilities.unavailable(t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /**
     * Fills a {@code struct io_uring_probe} via {@code IORING_REGISTER_PROBE} and returns a bitset of supported
     * opcodes. Header is 16 bytes; each {@code io_uring_probe_op} is 8 bytes ({@code op, resv, flags, resv2}).
     * Falls back to the kernel-floor opcode set if the probe register call fails.
     */
    private static long probeOpcodes(int fd)
    {
        try (Arena arena = Arena.ofConfined())
        {
            MemorySegment cap = arena.allocate(Errno.CAPTURE);
            long probeBytes = 16L + (long) PROBE_MAX_OPS * 8L;
            MemorySegment probe = arena.allocate(probeBytes);
            int ret = Syscalls.register(cap, fd, RegisterOps.PROBE, probe, PROBE_MAX_OPS);
            if (ret < 0)
                return FLOOR_OPCODES;

            int opsLen = probe.get(JAVA_BYTE, 1) & 0xFF;   // last_op + 1
            int max = Math.min(opsLen, 64);
            long bits = 0L;
            for (int op = 0; op < max; op++)
            {
                int opFlags = probe.get(JAVA_SHORT_UNALIGNED, 16L + (long) op * 8L + 2L) & 0xFFFF;
                if ((opFlags & RegisterOps.IO_URING_OP_SUPPORTED) != 0)
                    bits |= (1L << op);
            }
            return bits;
        }
        catch (Throwable t)
        {
            return FLOOR_OPCODES;
        }
    }
}
