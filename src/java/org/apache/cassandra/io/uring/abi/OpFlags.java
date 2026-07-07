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

/**
 * Op-specific flag constants that live in the {@code sqe.rw_flags} union word (offset 28, written via
 * {@link SqeExt#opFlags}) or, for the multishot toggles, in {@code sqe.ioprio} (offset 2, {@link Sqe#ioprio}).
 * These belong to the network/filesystem/control surface; the storage read path uses none of them, but the
 * general-purpose library exposes the full set for completeness.
 *
 * <p>As with every other opcode-numbered constant here, presence of a flag does not imply the running kernel honours
 * it — gate anything above the 5.6 floor via {@code IORING_REGISTER_PROBE}. Values verified against
 * {@code /usr/include/linux/io_uring.h} on this host.
 */
public final class OpFlags
{
    // --- poll (sqe.len for multishot; sqe.poll32_events carries the epoll mask) ---
    /** {@code IORING_OP_POLL_ADD} with this in {@code len} is multishot (re-arms, emits {@code CQE_F_MORE}). */
    public static final int IORING_POLL_ADD_MULTI = 1 << 0;
    /** {@code poll_update}: replace the armed event mask of the matched request. */
    public static final int IORING_POLL_UPDATE_EVENTS = 1 << 1;
    /** {@code poll_update}: replace the {@code user_data} of the matched request. */
    public static final int IORING_POLL_UPDATE_USER_DATA = 1 << 2;

    // --- accept (sqe.ioprio) ---
    /** {@code accept}: keep the SQE armed, emitting a CQE per accepted connection. */
    public static final int IORING_ACCEPT_MULTISHOT = 1 << 0;
    /** {@code accept}: never block, fail fast with {@code -EAGAIN}. */
    public static final int IORING_ACCEPT_DONTWAIT = 1 << 1;
    /** {@code accept}: arm an internal poll before attempting the accept. */
    public static final int IORING_ACCEPT_POLL_FIRST = 1 << 2;

    // --- send/recv (sqe.ioprio) ---
    /** {@code send}/{@code recv}: arm an internal poll before the first transfer attempt. */
    public static final int IORING_RECVSEND_POLL_FIRST = 1 << 0;
    /** {@code recv}: keep the SQE armed across multiple datagrams/segments (needs provided buffers). */
    public static final int IORING_RECV_MULTISHOT = 1 << 1;
    /** {@code send}/{@code recv}: address a registered buffer via {@code buf_index}. */
    public static final int IORING_RECVSEND_FIXED_BUF = 1 << 2;
    /** {@code send_zc}: report the copy/zero-copy decision back in a second CQE's {@code res}. */
    public static final int IORING_SEND_ZC_REPORT_USAGE = 1 << 3;

    // --- timeout (sqe.timeout_flags) ---
    /** {@code timeout}: the timespec is an absolute deadline, not a relative duration. */
    public static final int IORING_TIMEOUT_ABS = 1 << 0;
    /** {@code timeout_update}: this is an update of an existing timeout rather than a fresh arm. */
    public static final int IORING_TIMEOUT_UPDATE = 1 << 1;
    /** {@code timeout}: measure against {@code CLOCK_BOOTTIME}. */
    public static final int IORING_TIMEOUT_BOOTTIME = 1 << 2;
    /** {@code timeout}: measure against {@code CLOCK_REALTIME}. */
    public static final int IORING_TIMEOUT_REALTIME = 1 << 3;
    /** {@code link_timeout_update}: update of a linked timeout. */
    public static final int IORING_LINK_TIMEOUT_UPDATE = 1 << 4;
    /** {@code timeout}: report a fired timer as {@code res == 0} success instead of {@code -ETIME}. */
    public static final int IORING_TIMEOUT_ETIME_SUCCESS = 1 << 5;
    /** {@code timeout}: multishot timer, re-arms and emits a CQE on every expiry. */
    public static final int IORING_TIMEOUT_MULTISHOT = 1 << 6;
    /** Mask of the clock-source bits ({@link #IORING_TIMEOUT_BOOTTIME} | {@link #IORING_TIMEOUT_REALTIME}). */
    public static final int IORING_TIMEOUT_CLOCK_MASK = IORING_TIMEOUT_BOOTTIME | IORING_TIMEOUT_REALTIME;
    /** Mask of the update bits ({@link #IORING_TIMEOUT_UPDATE} | {@link #IORING_LINK_TIMEOUT_UPDATE}). */
    public static final int IORING_TIMEOUT_UPDATE_MASK = IORING_TIMEOUT_UPDATE | IORING_LINK_TIMEOUT_UPDATE;

    // --- cancel (sqe.cancel_flags) ---
    /** {@code cancel}: cancel every request matching the key, not just the first. */
    public static final int IORING_ASYNC_CANCEL_ALL = 1 << 0;
    /** {@code cancel}: match on {@code fd} rather than {@code user_data}. */
    public static final int IORING_ASYNC_CANCEL_FD = 1 << 1;
    /** {@code cancel}: match any in-flight request. */
    public static final int IORING_ASYNC_CANCEL_ANY = 1 << 2;
    /** {@code cancel}: the {@code fd} being matched is a registered (fixed) descriptor. */
    public static final int IORING_ASYNC_CANCEL_FD_FIXED = 1 << 3;
    /** {@code cancel}: match on {@code user_data} (the default when no other key is set). */
    public static final int IORING_ASYNC_CANCEL_USERDATA = 1 << 4;
    /** {@code cancel}: match on opcode. */
    public static final int IORING_ASYNC_CANCEL_OP = 1 << 5;

    // --- msg_ring: sqe.off carries the enum value below; sqe.msg_ring_flags carries the modifier bits ---
    /** {@code msg_ring}: pass {@code len} as the target CQE's {@code res} and {@code off} as its {@code user_data}. */
    public static final int IORING_MSG_DATA = 0;
    /** {@code msg_ring}: send a registered fd to the target ring. */
    public static final int IORING_MSG_SEND_FD = 1;
    /** {@code msg_ring_flags}: do not post a CQE to the target ring (not valid with {@link #IORING_MSG_DATA}). */
    public static final int IORING_MSG_RING_CQE_SKIP = 1 << 0;
    /** {@code msg_ring_flags}: pass {@code sqe.file_index} through as the target CQE's {@code flags}. */
    public static final int IORING_MSG_RING_FLAGS_PASS = 1 << 1;

    /** {@code fsync_flags}: request fdatasync semantics; alias of {@link Opcodes#IORING_FSYNC_DATASYNC}. */
    public static final int IORING_FSYNC_DATASYNC = Opcodes.IORING_FSYNC_DATASYNC;

    /**
     * Sentinel for {@code sqe.file_index} on {@code *_direct} ops: let io_uring allocate any free registered-file slot
     * and return the chosen index in {@code cqe.res} ({@code -ENFILE} if the table is full). {@code op.TargetFixedFile}
     * applies this raw (all other indices are stored as {@code idx + 1}).
     */
    public static final int IORING_FILE_INDEX_ALLOC = ~0;

    private OpFlags() {}
}
