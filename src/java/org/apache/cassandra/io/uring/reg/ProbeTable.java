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
package org.apache.cassandra.io.uring.reg;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.IoUringException;
import org.apache.cassandra.io.uring.abi.RegisterOps;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.Syscalls;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * Fills a {@code struct io_uring_probe} via {@code IORING_REGISTER_PROBE} (opcode {@link RegisterOps#PROBE}) and
 * distils it to a 64-bit "opcode supported" bitset &mdash; the correct <em>runtime</em> opcode-availability check,
 * since the compile-host header advertises opcodes a production kernel may not implement.
 *
 * <p>Mirrors {@link org.apache.cassandra.io.uring.probe.IoUringAvailability}'s private probe, but as a reusable
 * registration-layer utility that reports failure explicitly ({@link IoUringException}) rather than silently
 * degrading to a kernel-floor set &mdash; the availability path wants a safe fallback, a caller that explicitly
 * probes wants the truth. The probe header is 16 bytes; each {@code io_uring_probe_op} is 8 bytes
 * ({@code op}, {@code resv}, {@code u16 flags}, {@code resv2}) and is "supported" iff
 * {@code flags &amp; }{@link RegisterOps#IO_URING_OP_SUPPORTED}.
 */
public final class ProbeTable
{
    /** Opcodes 0..255 is the ABI ceiling; we only care about the 0..63 that fit a {@code long} bitset. */
    private static final int PROBE_MAX_OPS = 256;
    private static final int PROBE_HEADER_BYTES = 16;
    private static final int PROBE_OP_BYTES = 8;
    private static final int PROBE_OP_FLAGS_OFF = 2;   // within each io_uring_probe_op
    private static final int PROBE_OPS_LEN_OFF = 1;    // io_uring_probe.ops_len

    private ProbeTable() {}

    /**
     * Returns a bitset where bit {@code op} is set iff the running kernel reports {@code IORING_OP_<op>} supported,
     * for {@code op} in {@code 0..63}. Throws {@link IoUringException} (carrying errno) if the probe register call
     * itself fails (e.g. {@code EPERM} under a seccomp policy).
     *
     * @param ringFd the ring fd from {@link org.apache.cassandra.io.uring.ring.Ring#fd()}
     */
    public static long probe(int ringFd)
    {
        try (Arena arena = Arena.ofConfined())
        {
            MemorySegment cap = arena.allocate(Errno.CAPTURE);
            long probeBytes = PROBE_HEADER_BYTES + (long) PROBE_MAX_OPS * PROBE_OP_BYTES;
            MemorySegment probe = arena.allocate(probeBytes);

            int ret = Syscalls.register(cap, ringFd, RegisterOps.PROBE, probe, PROBE_MAX_OPS);
            if (ret < 0)
                throw new IoUringException("REGISTER_PROBE failed", Errno.of(cap));

            int opsLen = probe.get(JAVA_BYTE, PROBE_OPS_LEN_OFF) & 0xFF;   // last_op + 1
            int max = Math.min(opsLen, Long.SIZE);
            long bits = 0L;
            for (int op = 0; op < max; op++)
            {
                int opFlags = probe.get(JAVA_SHORT_UNALIGNED,
                                        PROBE_HEADER_BYTES + (long) op * PROBE_OP_BYTES + PROBE_OP_FLAGS_OFF) & 0xFFFF;
                if ((opFlags & RegisterOps.IO_URING_OP_SUPPORTED) != 0)
                    bits |= (1L << op);
            }
            return bits;
        }
    }

    /** Whether {@code op} (0..63) is set in a {@link #probe(int)} bitset. Ops outside that range are never set. */
    public static boolean supports(long bitset, int op)
    {
        if (op < 0 || op >= Long.SIZE)
            return false;
        return (bitset & (1L << op)) != 0;
    }
}
