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
import org.apache.cassandra.io.uring.abi.RegLayouts;
import org.apache.cassandra.io.uring.abi.RegisterOps;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.Syscalls;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

/**
 * Synchronous cancellation via {@code IORING_REGISTER_SYNC_CANCEL} ({@link RegisterOps#REGISTER_SYNC_CANCEL}), the
 * cancel path used for shutdown drain: unlike the async {@code IORING_OP_ASYNC_CANCEL} SQE, this cancels in-flight
 * requests inline from the register syscall, so no SQE slot is consumed while tearing a ring down. Matched requests
 * still complete with {@code -ECANCELED} in their CQE, so the caller must reap those completions after cancelling.
 *
 * <p>The argument is a single {@code struct io_uring_sync_cancel_reg} ({@code nr_args = 1}) carrying the match key
 * ({@code user_data} in {@code addr}, or {@code fd}, or {@code opcode}) plus the {@code IORING_ASYNC_CANCEL_*} match
 * flags. {@link #cancelAll()} matches every in-flight request; a {@code timeout} of zero (used here) makes the
 * cancel non-blocking.
 *
 * <p>Owns a private {@link Arena} plus a reusable {@code errno} capture segment; drive it from the ring's owning
 * thread and {@link #close()} it to release the arena.
 */
public final class SyncCancel implements AutoCloseable
{
    // struct io_uring_sync_cancel_reg.flags -- IORING_ASYNC_CANCEL_* (shared with the async-cancel SQE).
    /** Cancel <em>all</em> requests matching the key rather than the first. */
    public static final int CANCEL_ALL = 1 << 0;
    /** Match on {@code fd} rather than {@code user_data}. */
    public static final int CANCEL_FD = 1 << 1;
    /** Match any request (ignore the key entirely); pair with {@link #CANCEL_ALL} to cancel everything. */
    public static final int CANCEL_ANY = 1 << 2;
    /** The {@code fd} is a registered (fixed) descriptor index, not a real fd. */
    public static final int CANCEL_FD_FIXED = 1 << 3;
    /** Match on {@code user_data} (the default when no other key flag is set). */
    public static final int CANCEL_USERDATA = 1 << 4;
    /** Match on {@code opcode}. */
    public static final int CANCEL_OP = 1 << 5;

    /** {@code ENOENT} (asm-generic, identical on x86-64/aarch64); {@code sync_cancel} returns it when nothing matched. */
    private static final int ENOENT = 2;

    private final int ringFd;
    private final Arena arena;
    private final MemorySegment cap;

    /** @param ringFd the ring fd from {@link org.apache.cassandra.io.uring.ring.Ring#fd()}. */
    public SyncCancel(int ringFd)
    {
        this.ringFd = ringFd;
        this.arena = Arena.ofShared();
        this.cap = arena.allocate(Errno.CAPTURE);
    }

    /**
     * Cancels every in-flight request on the ring ({@link #CANCEL_ALL} | {@link #CANCEL_ANY}), the shutdown-drain
     * primitive. Returns the number of requests the kernel cancelled ({@code 0} when there was nothing in flight,
     * which the kernel reports as {@code -ENOENT} and is treated here as a benign "already drained"). Throws
     * {@link IoUringException} on any other failure.
     */
    public int cancelAll()
    {
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment reg = scratch.allocate(RegLayouts.SYNC_CANCEL);
            reg.set(JAVA_INT_UNALIGNED, RegLayouts.SYNC_CANCEL_FLAGS, CANCEL_ALL | CANCEL_ANY);

            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_SYNC_CANCEL, reg, 1);
            if (ret >= 0)
                return ret;
            int errno = Errno.of(cap);
            if (errno == ENOENT)
                return 0;   // nothing matched -> nothing to drain
            throw new IoUringException("REGISTER_SYNC_CANCEL(all) failed", errno);
        }
    }

    /**
     * Cancels the in-flight request(s) identified by {@code userData} (the token set with
     * {@link org.apache.cassandra.io.uring.op.Prep#setData64}). With {@code all=false} the first match is cancelled;
     * with {@code all=true} every request carrying that token is. Matched requests complete with {@code -ECANCELED}.
     */
    public int cancelUserData(long userData, boolean all)
    {
        int flags = CANCEL_USERDATA | (all ? CANCEL_ALL : 0);
        return registerSyncCancel(userData, -1, flags, 0);
    }

    /**
     * Low-level {@code REGISTER_SYNC_CANCEL} passthrough. Builds the {@code struct io_uring_sync_cancel_reg} from the
     * given key fields with a zero (non-blocking) timeout and issues the register call.
     *
     * @param addr    match key placed in {@code addr} (a {@code user_data} token when {@link #CANCEL_USERDATA} is set)
     * @param fd      match {@code fd} ({@code -1} when not matching by fd)
     * @param flags   {@code IORING_ASYNC_CANCEL_*} match flags ({@code CANCEL_*} constants on this class)
     * @param opcode  match {@code opcode} ({@code IORING_OP_*}) when {@link #CANCEL_OP} is set; ignored otherwise
     * @return the number of requests cancelled ({@code >= 0})
     */
    public int registerSyncCancel(long addr, int fd, int flags, int opcode)
    {
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment reg = scratch.allocate(RegLayouts.SYNC_CANCEL);
            reg.set(JAVA_LONG_UNALIGNED, RegLayouts.SYNC_CANCEL_ADDR, addr);
            reg.set(JAVA_INT_UNALIGNED, RegLayouts.SYNC_CANCEL_FD, fd);
            reg.set(JAVA_INT_UNALIGNED, RegLayouts.SYNC_CANCEL_FLAGS, flags);
            reg.set(JAVA_BYTE, RegLayouts.SYNC_CANCEL_OPCODE, (byte) opcode);

            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_SYNC_CANCEL, reg, 1);
            if (ret < 0)
                throw new IoUringException("REGISTER_SYNC_CANCEL failed", Errno.of(cap));
            return ret;
        }
    }

    @Override
    public void close()
    {
        arena.close();
    }
}
