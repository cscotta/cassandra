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
package org.apache.cassandra.io.uring.abi;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import org.apache.cassandra.io.uring.IoUringException;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM {@link StructLayout}s for the {@code io_uring_register} argument structs, alongside {@link Layouts} which covers
 * the setup/submit/complete structs. Every size and offset below was compiler-verified against
 * {@code /usr/include/linux/io_uring.h} (and {@code <sys/resource.h>} for {@code struct rlimit}) on this host, and
 * {@link #verify()} re-checks the constructed layouts against those constants at startup so a wrong
 * {@code paddingLayout} (FFM does <em>not</em> auto-pad) fails loudly rather than corrupting a register call.
 *
 * <p>The register layer uses the plain {@code *_OFF} integer offsets with {@code JAVA_*_UNALIGNED} accessors (the
 * same idiom as {@link Sqe}/{@link Cqe}); the {@link StructLayout} objects exist for the layout guard and for the
 * {@code *_BYTES} sizes, which double as the {@code nr_args} value the kernel demands for the size-checked
 * {@code REGISTER_*2} opcodes.
 *
 * <p><b>ABI note:</b> {@code struct io_uring_rsrc_register} is <b>32 bytes</b> on this header
 * ({@code nr}@0, {@code flags}@4, {@code resv2}@8, {@code data}@16, {@code tags}@24), not 24 &mdash; the trailing
 * {@code __aligned_u64 data}/{@code tags} pair carries it past 24. {@link RegLayouts} implements the header-verified
 * 32 and uses {@link #RSRC_REGISTER_BYTES} as the {@code nr_args} the kernel copies for {@code REGISTER_FILES2}/
 * {@code REGISTER_BUFFERS2}.
 */
public final class RegLayouts
{
    // --- struct io_uring_rsrc_register (32 B) : REGISTER_FILES2 / REGISTER_BUFFERS2 arg ---
    public static final int RSRC_REGISTER_BYTES = 32;
    public static final int RSRC_REGISTER_NR = 0;
    public static final int RSRC_REGISTER_FLAGS = 4;
    public static final int RSRC_REGISTER_RESV2 = 8;
    public static final int RSRC_REGISTER_DATA = 16;
    public static final int RSRC_REGISTER_TAGS = 24;

    // --- struct io_uring_rsrc_update (16 B) : REGISTER_FILES_UPDATE / (UN)REGISTER_RING_FDS arg ---
    public static final int RSRC_UPDATE_BYTES = 16;
    public static final int RSRC_UPDATE_OFFSET = 0;
    public static final int RSRC_UPDATE_RESV = 4;
    public static final int RSRC_UPDATE_DATA = 8;

    // --- struct io_uring_rsrc_update2 (32 B) : tagged file/buffer updates ---
    public static final int RSRC_UPDATE2_BYTES = 32;
    public static final int RSRC_UPDATE2_OFFSET = 0;
    public static final int RSRC_UPDATE2_RESV = 4;
    public static final int RSRC_UPDATE2_DATA = 8;
    public static final int RSRC_UPDATE2_TAGS = 16;
    public static final int RSRC_UPDATE2_NR = 24;
    public static final int RSRC_UPDATE2_RESV2 = 28;

    // --- struct io_uring_sync_cancel_reg (64 B) : REGISTER_SYNC_CANCEL arg ---
    public static final int SYNC_CANCEL_BYTES = 64;
    public static final int SYNC_CANCEL_ADDR = 0;
    public static final int SYNC_CANCEL_FD = 8;
    public static final int SYNC_CANCEL_FLAGS = 12;
    public static final int SYNC_CANCEL_TIMEOUT = 16;   // embedded __kernel_timespec (16 B)
    public static final int SYNC_CANCEL_OPCODE = 32;

    // --- struct rlimit (16 B) : getrlimit(RLIMIT_MEMLOCK) precheck ---
    public static final int RLIMIT_BYTES = 16;
    public static final int RLIMIT_CUR = 0;
    public static final int RLIMIT_MAX = 8;

    /** {@code getrlimit}'s "no limit" sentinel ({@code RLIM_INFINITY}); {@code (rlim_t) -1}, i.e. all-ones u64. */
    public static final long RLIM_INFINITY = -1L;

    public static final StructLayout RSRC_REGISTER = MemoryLayout.structLayout(
        JAVA_INT.withName("nr"),
        JAVA_INT.withName("flags"),
        JAVA_LONG.withName("resv2"),
        JAVA_LONG.withName("data"),
        JAVA_LONG.withName("tags")).withName("io_uring_rsrc_register");

    public static final StructLayout RSRC_UPDATE = MemoryLayout.structLayout(
        JAVA_INT.withName("offset"),
        JAVA_INT.withName("resv"),
        JAVA_LONG.withName("data")).withName("io_uring_rsrc_update");

    public static final StructLayout RSRC_UPDATE2 = MemoryLayout.structLayout(
        JAVA_INT.withName("offset"),
        JAVA_INT.withName("resv"),
        JAVA_LONG.withName("data"),
        JAVA_LONG.withName("tags"),
        JAVA_INT.withName("nr"),
        JAVA_INT.withName("resv2")).withName("io_uring_rsrc_update2");

    public static final StructLayout SYNC_CANCEL = MemoryLayout.structLayout(
        JAVA_LONG.withName("addr"),
        JAVA_INT.withName("fd"),
        JAVA_INT.withName("flags"),
        Layouts.KERNEL_TIMESPEC.withName("timeout"),
        JAVA_BYTE.withName("opcode"),
        MemoryLayout.paddingLayout(7),
        MemoryLayout.sequenceLayout(3, JAVA_LONG).withName("pad2")).withName("io_uring_sync_cancel_reg");

    public static final StructLayout RLIMIT = MemoryLayout.structLayout(
        JAVA_LONG.withName("rlim_cur"),
        JAVA_LONG.withName("rlim_max")).withName("rlimit");

    private RegLayouts() {}

    /**
     * Asserts every constructed register-arg layout matches the compiler-verified ground truth (size + key offsets).
     * Cheap; call once before the first register call. Throws {@link IoUringException} on any mismatch (a padding or
     * offset bug that would otherwise make the kernel read the wrong bytes).
     */
    public static void verify()
    {
        check("rsrc_register.byteSize", RSRC_REGISTER.byteSize(), RSRC_REGISTER_BYTES);
        check("rsrc_register.data", RSRC_REGISTER.byteOffset(groupElement("data")), RSRC_REGISTER_DATA);
        check("rsrc_register.tags", RSRC_REGISTER.byteOffset(groupElement("tags")), RSRC_REGISTER_TAGS);

        check("rsrc_update.byteSize", RSRC_UPDATE.byteSize(), RSRC_UPDATE_BYTES);
        check("rsrc_update.data", RSRC_UPDATE.byteOffset(groupElement("data")), RSRC_UPDATE_DATA);

        check("rsrc_update2.byteSize", RSRC_UPDATE2.byteSize(), RSRC_UPDATE2_BYTES);
        check("rsrc_update2.nr", RSRC_UPDATE2.byteOffset(groupElement("nr")), RSRC_UPDATE2_NR);
        check("rsrc_update2.tags", RSRC_UPDATE2.byteOffset(groupElement("tags")), RSRC_UPDATE2_TAGS);

        check("sync_cancel.byteSize", SYNC_CANCEL.byteSize(), SYNC_CANCEL_BYTES);
        check("sync_cancel.fd", SYNC_CANCEL.byteOffset(groupElement("fd")), SYNC_CANCEL_FD);
        check("sync_cancel.flags", SYNC_CANCEL.byteOffset(groupElement("flags")), SYNC_CANCEL_FLAGS);
        check("sync_cancel.timeout", SYNC_CANCEL.byteOffset(groupElement("timeout")), SYNC_CANCEL_TIMEOUT);
        check("sync_cancel.opcode", SYNC_CANCEL.byteOffset(groupElement("opcode")), SYNC_CANCEL_OPCODE);

        check("rlimit.byteSize", RLIMIT.byteSize(), RLIMIT_BYTES);
        check("rlimit.rlim_max", RLIMIT.byteOffset(groupElement("rlim_max")), RLIMIT_MAX);
    }

    private static void check(String what, long actual, long expected)
    {
        if (actual != expected)
            throw new IoUringException("register ABI layout mismatch for " + what + ": expected " + expected + " but got " + actual);
    }
}
