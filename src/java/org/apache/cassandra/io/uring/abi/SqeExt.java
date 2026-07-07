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

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * Accessors for the {@code io_uring_sqe} union fields that the base {@link Sqe} intentionally omits (it exposes only
 * the read/write hot-path members). The network/filesystem/control preps in {@code op.PrepNet}/{@code op.PrepFs}/
 * {@code op.PrepCtl} drive these; keeping them here mirrors liburing's practice of writing one physical word through
 * many named union members.
 *
 * <p>The SQE is a heavily unionized 64-byte struct, so several of these setters write the <em>same</em> word under
 * different names — e.g. {@link #off2}/{@link #addr2} both land on offset 8 (the {@code off} union), and
 * {@link #spliceFdIn}/{@link #fileIndex}/{@link #addrLen} all target offset 44. That is deliberate: each op only ever
 * populates one member of a given union, and {@link Sqe#clear} (called by every prep via {@code op.Prep.prepRw})
 * has already zeroed the whole 64 bytes, so the untouched siblings read back as zero. All access is offset-based and
 * unaligned so it never trips FFM alignment checks on the packed struct, exactly like {@link Sqe}.
 */
public final class SqeExt
{
    // --- offsets not named in Layouts (they live inside SQE unions Layouts does not expose as *_OFF constants) ---
    /** {@code personality} (u16), immediately after {@code buf_index}/{@code buf_group}. */
    private static final int SQE_PERSONALITY = 42;
    /** {@code splice_fd_in}/{@code file_index}/{@code addr_len} union (s32/u32/u16). */
    private static final int SQE_SPLICE_FD_IN = 44;
    /** {@code addr3}/{@code optval} union (u64), start of the trailing 16-byte block. */
    private static final int SQE_ADDR3 = 48;

    private SqeExt() {}

    /**
     * {@code off}/{@code addr2} union at offset 8, written as a second offset (e.g. {@code off_out} for splice, the
     * new {@code user_data} for a poll/timeout update). Same physical word as {@link Sqe#off}.
     */
    public static void off2(MemorySegment sqe, long off2)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_OFF, off2);
    }

    /**
     * {@code addr2} union at offset 8, written as a secondary address (e.g. the destination {@code sockaddr} for
     * {@code sendto}). Same physical word as {@link Sqe#off}.
     */
    public static void addr2(MemorySegment sqe, long addr2)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_OFF, addr2);
    }

    /**
     * {@code splice_off_in} union at offset 16 (source offset for {@code splice}/{@code tee}). Same physical word as
     * {@link Sqe#addr}.
     */
    public static void spliceOffIn(MemorySegment sqe, long spliceOffIn)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_ADDR, spliceOffIn);
    }

    /**
     * The op-specific flags union at offset 28. This one physical word backs {@code rw_flags}, {@code fsync_flags},
     * {@code poll32_events}, {@code msg_flags}, {@code timeout_flags}, {@code accept_flags}, {@code cancel_flags},
     * {@code open_flags}, {@code statx_flags}, {@code fadvise_advice}, {@code splice_flags}, {@code rename_flags},
     * {@code unlink_flags}, {@code hardlink_flags} and {@code msg_ring_flags}; see {@link OpFlags} for the values.
     * Same physical word as {@link Sqe#rwFlags}.
     */
    public static void opFlags(MemorySegment sqe, int opFlags)
    {
        sqe.set(JAVA_INT_UNALIGNED, Layouts.SQE_RW_FLAGS, opFlags);
    }

    /**
     * {@code buf_group} union at offset 40 (provided-buffer group id for {@code BUFFER_SELECT}). Same physical word as
     * {@link Sqe#bufIndex}.
     */
    public static void bufGroup(MemorySegment sqe, int bufGroup)
    {
        sqe.set(JAVA_SHORT_UNALIGNED, Layouts.SQE_BUF_INDEX, (short) bufGroup);
    }

    /** {@code personality} (u16) at offset 42: submit this op under a previously registered credential set. */
    public static void personality(MemorySegment sqe, int personality)
    {
        sqe.set(JAVA_SHORT_UNALIGNED, SQE_PERSONALITY, (short) personality);
    }

    /** {@code splice_fd_in} (s32) at offset 44: the source fd for {@code splice}/{@code tee}. */
    public static void spliceFdIn(MemorySegment sqe, int spliceFdIn)
    {
        sqe.set(JAVA_INT_UNALIGNED, SQE_SPLICE_FD_IN, spliceFdIn);
    }

    /**
     * {@code file_index} (u32) at offset 44: the registered-file slot a {@code *_direct} op installs into. Callers
     * should route through {@code op.TargetFixedFile} which applies the {@code idx + 1} / {@code ALLOC} encoding.
     * Same physical word as {@link #spliceFdIn}.
     */
    public static void fileIndex(MemorySegment sqe, int fileIndex)
    {
        sqe.set(JAVA_INT_UNALIGNED, SQE_SPLICE_FD_IN, fileIndex);
    }

    /**
     * {@code addr_len} (u16) at offset 44: the destination address length for {@code sendto}. Occupies the low half of
     * the {@link #spliceFdIn}/{@link #fileIndex} union word.
     */
    public static void addrLen(MemorySegment sqe, int addrLen)
    {
        sqe.set(JAVA_SHORT_UNALIGNED, SQE_SPLICE_FD_IN, (short) addrLen);
    }

    /** {@code addr3} (u64) at offset 48: third pointer/attribute argument (e.g. {@code open_how} attrs). */
    public static void addr3(MemorySegment sqe, long addr3)
    {
        sqe.set(JAVA_LONG_UNALIGNED, SQE_ADDR3, addr3);
    }

    /** {@code optval} (u64) union at offset 48: setsockopt value pointer. Same physical word as {@link #addr3}. */
    public static void optval(MemorySegment sqe, long optval)
    {
        sqe.set(JAVA_LONG_UNALIGNED, SQE_ADDR3, optval);
    }
}
