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
package org.apache.cassandra.io.uring.op;

import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.abi.OpFlags;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.Sqe;
import org.apache.cassandra.io.uring.abi.SqeExt;

/**
 * Control-plane preps mirroring liburing's timeout / cancel / msg-ring / buffer-provisioning / advise wrappers.
 * Present for library completeness; the storage read path submits none of these, though {@code timeout}/{@code cancel}
 * back a bounded-wait poller and clean shutdown drain, and {@code fadvise}/{@code fallocate}/{@code ftruncate} are the
 * async equivalents of the hints/preallocation the flush path already issues synchronously.
 *
 * <p>Every prep first calls {@link Prep#prepRw} (which zeroes the whole 64-byte SQE) and then sets the op-specific
 * extras via {@link Sqe}/{@link SqeExt}. {@code tsAddr} is a native pointer to a {@code struct __kernel_timespec}
 * (see {@code abi.Layouts#KERNEL_TIMESPEC}); the caller owns that segment and must keep it alive until the CQE is
 * reaped. {@code userData} correlation tokens are matched by value, exactly as they were set on the target SQE.
 */
public final class PrepCtl
{
    private PrepCtl() {}

    /**
     * {@code IORING_OP_TIMEOUT}: fires a CQE after {@code count} completions elapse or the {@code tsAddr} duration is
     * reached, whichever comes first. {@code len} is fixed at 1, {@code count} rides in {@code off}, and the
     * {@code IORING_TIMEOUT_*} bits in {@code flags} select absolute vs relative and the clock source.
     */
    public static void timeout(MemorySegment sqe, long tsAddr, long count, int flags)
    {
        Prep.prepRw(sqe, Opcodes.TIMEOUT, -1, tsAddr, 1, count);
        SqeExt.opFlags(sqe, flags);
    }

    /** {@code IORING_OP_TIMEOUT_REMOVE}: cancel the timeout whose SQE carried {@code userData} (matched via {@code addr}). */
    public static void timeoutRemove(MemorySegment sqe, long userData, int flags)
    {
        Prep.prepRw(sqe, Opcodes.TIMEOUT_REMOVE, -1, 0L, 0, 0L);
        Sqe.addr(sqe, userData);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_TIMEOUT_REMOVE} as an update: re-arm the timeout matched by {@code userData} with the new
     * {@code __kernel_timespec} at {@code tsAddr} (which rides in {@code off}). Forces
     * {@link OpFlags#IORING_TIMEOUT_UPDATE} into {@code flags}.
     */
    public static void timeoutUpdate(MemorySegment sqe, long tsAddr, long userData, int flags)
    {
        Prep.prepRw(sqe, Opcodes.TIMEOUT_REMOVE, -1, 0L, 0, tsAddr);
        Sqe.addr(sqe, userData);
        SqeExt.opFlags(sqe, flags | OpFlags.IORING_TIMEOUT_UPDATE);
    }

    /**
     * {@code IORING_OP_LINK_TIMEOUT}: a deadline for the immediately preceding {@code IOSQE_IO_LINK}-ed SQE, cancelling
     * it with {@code -ECANCELED} if {@code tsAddr} elapses first. Must directly follow the guarded SQE in the chain.
     */
    public static void linkTimeout(MemorySegment sqe, long tsAddr, int flags)
    {
        Prep.prepRw(sqe, Opcodes.LINK_TIMEOUT, -1, tsAddr, 1, 0L);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_ASYNC_CANCEL}: cancel in-flight request(s) keyed by the 64-bit {@code userData}, per the
     * {@code IORING_ASYNC_CANCEL_*} bits in {@code flags}. Equivalent to {@link #cancel64}; the two names mirror
     * liburing's pointer vs {@code __u64} overloads, which collapse to one in Java.
     */
    public static void cancel(MemorySegment sqe, long userData, int flags)
    {
        cancel64(sqe, userData, flags);
    }

    /** {@code IORING_OP_ASYNC_CANCEL} keyed by a 64-bit {@code userData} token (matched via {@code addr}). */
    public static void cancel64(MemorySegment sqe, long userData, int flags)
    {
        Prep.prepRw(sqe, Opcodes.ASYNC_CANCEL, -1, 0L, 0, 0L);
        Sqe.addr(sqe, userData);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_MSG_RING}: post a message to another ring identified by {@code targetRingFd}. With
     * {@link OpFlags#IORING_MSG_DATA} the target sees a CQE whose {@code res} is {@code len} and {@code user_data} is
     * {@code data}; the {@code msg_ring_flags} modifier bits ride in {@code flags}.
     */
    public static void msgRing(MemorySegment sqe, int targetRingFd, int len, long data, int flags)
    {
        Prep.prepRw(sqe, Opcodes.MSG_RING, targetRingFd, 0L, len, data);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_PROVIDE_BUFFERS}: donate {@code nr} buffers of {@code len} bytes starting at {@code addr} to
     * group {@code bgid}, numbering them from {@code bid}. {@code nr} rides in {@code fd} and {@code bid} in {@code off};
     * the buffer group id goes to {@code buf_group}. Classic path; buffer rings are preferred where available.
     */
    public static void provideBuffers(MemorySegment sqe, long addr, int len, int nr, int bgid, int bid)
    {
        Prep.prepRw(sqe, Opcodes.PROVIDE_BUFFERS, nr, addr, len, bid);
        SqeExt.bufGroup(sqe, bgid);
    }

    /** {@code IORING_OP_REMOVE_BUFFERS}: remove {@code nr} buffers ({@code nr} rides in {@code fd}) from group {@code bgid}. */
    public static void removeBuffers(MemorySegment sqe, int nr, int bgid)
    {
        Prep.prepRw(sqe, Opcodes.REMOVE_BUFFERS, nr, 0L, 0, 0L);
        SqeExt.bufGroup(sqe, bgid);
    }

    /**
     * {@code IORING_OP_FADVISE}: posix_fadvise(2) over {@code [offset, offset+len)} of {@code fd}; {@code len} rides in
     * the {@code len} field and the {@code POSIX_FADV_*} {@code advice} in the {@code fadvise_advice} union word.
     */
    public static void fadvise(MemorySegment sqe, int fd, long offset, int len, int advice)
    {
        Prep.prepRw(sqe, Opcodes.FADVISE, fd, 0L, len, offset);
        SqeExt.opFlags(sqe, advice);
    }

    /**
     * {@code IORING_OP_FALLOCATE}: fallocate(2) on {@code fd}. {@code mode} rides in {@code len}, {@code offset} in
     * {@code off}, and the byte {@code len} in {@code addr}.
     */
    public static void fallocate(MemorySegment sqe, int fd, int mode, long offset, long len)
    {
        Prep.prepRw(sqe, Opcodes.FALLOCATE, fd, 0L, mode, offset);
        Sqe.addr(sqe, len);
    }

    /** {@code IORING_OP_FTRUNCATE}: ftruncate(2) {@code fd} to {@code len} bytes ({@code len} rides in {@code off}). */
    public static void ftruncate(MemorySegment sqe, int fd, long len)
    {
        Prep.prepRw(sqe, Opcodes.FTRUNCATE, fd, 0L, 0, len);
    }
}
