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

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

/**
 * Registers a ring's own fd with the task via {@code IORING_REGISTER_RING_FDS} (kernel &ge; 5.18) so subsequent
 * {@code io_uring_enter} calls can pass the small registered <em>index</em> plus
 * {@link org.apache.cassandra.io.uring.abi.EnterFlags#REGISTERED_RING} and skip the per-enter fd lookup &mdash; a
 * cheap latency win for a busy poller thread.
 *
 * <p>The register argument is a single {@code struct io_uring_rsrc_update} with {@code nr_args = 1}. Following
 * liburing's {@code io_uring_register_ring_fd}, {@code data} holds the ring fd <em>value</em> (not a pointer to it)
 * and {@code offset} is set to {@code -1} to request an auto-assigned slot; the kernel writes the granted index back
 * into {@code offset}, which {@link #registerRingFd(int)} returns. {@link #unregisterRingFd(int)} passes that index
 * back in {@code offset}.
 *
 * <p>Not thread-safe: the registration is per-task, so drive it from the same thread that will subsequently enter
 * the ring with the registered index. Owns a private {@link Arena} plus a reusable {@code errno} capture segment;
 * {@link #close()} releases that arena.
 */
public final class RingFdRegistrar implements AutoCloseable
{
    /** {@code offset = -1U} asks the kernel to auto-allocate a registered-ring slot. */
    private static final int AUTO_INDEX = -1;

    private final Arena arena;
    private final MemorySegment cap;

    public RingFdRegistrar()
    {
        this.arena = Arena.ofShared();
        this.cap = arena.allocate(Errno.CAPTURE);
    }

    /**
     * Registers {@code ringFd} and returns the assigned registered-ring index to use with
     * {@link org.apache.cassandra.io.uring.abi.EnterFlags#REGISTERED_RING}. The register call must target the same
     * (real) {@code ringFd}. Throws {@link IoUringException} on failure (e.g. {@code EINVAL} on kernels &lt; 5.18).
     *
     * @param ringFd the real ring fd from {@link org.apache.cassandra.io.uring.ring.Ring#fd()}
     */
    public int registerRingFd(int ringFd)
    {
        MemorySegment update = arena.allocate(RegLayouts.RSRC_UPDATE);
        update.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE_OFFSET, AUTO_INDEX);
        // data carries the fd value itself (widened into the u64), per the RING_FDS ABI.
        update.set(JAVA_LONG_UNALIGNED, RegLayouts.RSRC_UPDATE_DATA, ringFd & 0xFFFFFFFFL);

        int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_RING_FDS, update, 1);
        if (ret < 0)
            throw new IoUringException("REGISTER_RING_FDS failed", Errno.of(cap));
        // Kernel copies the granted index back into offset.
        return update.get(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE_OFFSET);
    }

    /**
     * Unregisters the ring-fd slot at {@code index} (from a prior {@link #registerRingFd(int)}). Enters after this
     * must use the real ring fd again.
     *
     * @param ringFd the real ring fd (the syscall must still target a valid ring fd)
     * @param index  the registered index returned by {@link #registerRingFd(int)}
     */
    public void unregisterRingFd(int ringFd, int index)
    {
        MemorySegment update = arena.allocate(RegLayouts.RSRC_UPDATE);
        update.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE_OFFSET, index);

        int ret = Syscalls.register(cap, ringFd, RegisterOps.UNREGISTER_RING_FDS, update, 1);
        if (ret < 0)
            throw new IoUringException("UNREGISTER_RING_FDS failed", Errno.of(cap));
    }

    @Override
    public void close()
    {
        arena.close();
    }
}
