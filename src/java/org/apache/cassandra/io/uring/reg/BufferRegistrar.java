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
import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.RegLayouts;
import org.apache.cassandra.io.uring.abi.RegisterOps;
import org.apache.cassandra.io.uring.linux.Errno;
import org.apache.cassandra.io.uring.linux.LibC;
import org.apache.cassandra.io.uring.linux.Syscalls;

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

/**
 * Pins a set of buffer regions with a ring so {@code IORING_OP_READ_FIXED}/{@code WRITE_FIXED} can DMA into them
 * without the kernel re-pinning pages per I/O. Each region is described by a {@code struct iovec} ({@code base},
 * {@code len}); after registration an SQE prepared with
 * {@link org.apache.cassandra.io.uring.op.Prep#prepReadFixed} targets an address <em>inside</em> registered region
 * {@code buf_index} (the address need not be the region base &mdash; any sub-range of the pinned region is legal).
 *
 * <p>Registered buffers are charged against {@code RLIMIT_MEMLOCK}. This registrar prechecks that limit with
 * {@link LibC#getrlimit} and, if the requested total exceeds a finite soft limit, throws before the syscall; it also
 * maps the {@code ENOMEM}/{@code EPERM} the kernel itself returns into an {@link IoUringException} so the caller can
 * fall back to unregistered {@code IORING_OP_READ}. Uses the tagged {@code IORING_REGISTER_BUFFERS2} form (kernel
 * &ge; 5.13), whose {@code nr_args} is the {@code struct io_uring_rsrc_register} byte size, with the buffer count in
 * {@code rsrc.nr}.
 *
 * <p>Not thread-safe: drive one instance from the ring's owning thread. Owns a private {@link Arena} plus a reusable
 * {@code errno} capture segment; {@link #close()} releases that arena (it does not unregister).
 */
public final class BufferRegistrar implements AutoCloseable
{
    private final int ringFd;
    private final Arena arena;
    private final MemorySegment cap;

    /** @param ringFd the ring fd from {@link org.apache.cassandra.io.uring.ring.Ring#fd()}. */
    public BufferRegistrar(int ringFd)
    {
        this.ringFd = ringFd;
        this.arena = Arena.ofShared();
        this.cap = arena.allocate(Errno.CAPTURE);
    }

    /**
     * Registers {@code addrs.length} buffer regions via {@code REGISTER_BUFFERS2}. {@code addrs[i]}/{@code lens[i]}
     * become registered-buffer slot {@code i}. Prechecks {@code RLIMIT_MEMLOCK}; throws {@link IoUringException} (so
     * the caller can fall back to unregistered reads) if the total would exceed a finite soft limit, or if the kernel
     * rejects the pin with {@code ENOMEM}/{@code EPERM}.
     *
     * @param addrs native addresses of each region (each must remain valid and pinned until {@link #unregisterBuffers()})
     * @param lens  byte length of each region, index-aligned with {@code addrs}
     */
    public void registerBuffers(long[] addrs, long[] lens)
    {
        if (addrs.length == 0)
            throw new IoUringException("registerBuffers requires at least one region");
        if (addrs.length != lens.length)
            throw new IoUringException("registerBuffers addrs/lens length mismatch: " + addrs.length + " vs " + lens.length);

        long total = 0;
        for (long len : lens)
            total += len;
        checkMemlock(total);

        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment iovecs = scratch.allocate((long) addrs.length * Layouts.IOVEC_BYTES, Long.BYTES);
            for (int i = 0; i < addrs.length; i++)
            {
                long base = (long) i * Layouts.IOVEC_BYTES;
                iovecs.set(JAVA_LONG_UNALIGNED, base + Layouts.IOVEC_BASE, addrs[i]);
                iovecs.set(JAVA_LONG_UNALIGNED, base + Layouts.IOVEC_LEN, lens[i]);
            }

            MemorySegment rsrc = scratch.allocate(RegLayouts.RSRC_REGISTER);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_NR, addrs.length);
            rsrc.set(JAVA_LONG_UNALIGNED, RegLayouts.RSRC_REGISTER_DATA, iovecs.address());

            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_BUFFERS2, rsrc, RegLayouts.RSRC_REGISTER_BYTES);
            if (ret < 0)
                throw new IoUringException("REGISTER_BUFFERS2 failed (fall back to unregistered READ)", Errno.of(cap));
        }
    }

    /**
     * Reserves {@code nr} empty (sparse) registered-buffer slots ({@link RegisterOps#RSRC_REGISTER_SPARSE}) without
     * pinning anything, so slots can be filled later with {@link #registerBuffersUpdate}. Lets a caller size the
     * fixed-buffer table up front and register regions incrementally as they appear (e.g. as a buffer pool grows).
     */
    public void registerBuffersSparse(int nr)
    {
        if (nr <= 0)
            throw new IoUringException("registerBuffersSparse requires nr > 0");
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment rsrc = scratch.allocate(RegLayouts.RSRC_REGISTER);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_NR, nr);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_FLAGS, RegisterOps.RSRC_REGISTER_SPARSE);
            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_BUFFERS2, rsrc, RegLayouts.RSRC_REGISTER_BYTES);
            if (ret < 0)
                throw new IoUringException("REGISTER_BUFFERS2(sparse) failed", Errno.of(cap));
        }
    }

    /**
     * Fills {@code addrs.length} slots of an already-registered (sparse) table starting at {@code offset} via
     * {@code IORING_REGISTER_BUFFERS_UPDATE} ({@code struct io_uring_rsrc_update2}, {@code nr_args} = the struct size,
     * count in {@code rsrc.nr}); slot {@code offset+i} becomes {@code addrs[i]}/{@code lens[i]}. Prechecks
     * {@code RLIMIT_MEMLOCK} and maps {@code ENOMEM}/{@code EPERM} to {@link IoUringException} so the caller can fall
     * back to unregistered reads.
     */
    public void registerBuffersUpdate(int offset, long[] addrs, long[] lens)
    {
        if (addrs.length == 0)
            throw new IoUringException("registerBuffersUpdate requires at least one region");
        if (addrs.length != lens.length)
            throw new IoUringException("registerBuffersUpdate addrs/lens length mismatch: " + addrs.length + " vs " + lens.length);

        long total = 0;
        for (long len : lens)
            total += len;
        checkMemlock(total);

        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment iovecs = scratch.allocate((long) addrs.length * Layouts.IOVEC_BYTES, Long.BYTES);
            for (int i = 0; i < addrs.length; i++)
            {
                long base = (long) i * Layouts.IOVEC_BYTES;
                iovecs.set(JAVA_LONG_UNALIGNED, base + Layouts.IOVEC_BASE, addrs[i]);
                iovecs.set(JAVA_LONG_UNALIGNED, base + Layouts.IOVEC_LEN, lens[i]);
            }

            MemorySegment update = scratch.allocate(RegLayouts.RSRC_UPDATE2);
            update.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE2_OFFSET, offset);
            update.set(JAVA_LONG_UNALIGNED, RegLayouts.RSRC_UPDATE2_DATA, iovecs.address());
            update.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE2_NR, addrs.length);

            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_BUFFERS_UPDATE, update, RegLayouts.RSRC_UPDATE2_BYTES);
            if (ret < 0)
                throw new IoUringException("REGISTER_BUFFERS_UPDATE failed (fall back to unregistered READ)", Errno.of(cap));
        }
    }

    /** Releases the entire registered-buffer set ({@code arg = NULL}, {@code nr_args = 0}). */
    public void unregisterBuffers()
    {
        int ret = Syscalls.register(cap, ringFd, RegisterOps.UNREGISTER_BUFFERS, MemorySegment.NULL, 0);
        if (ret < 0)
            throw new IoUringException("UNREGISTER_BUFFERS failed", Errno.of(cap));
    }

    /**
     * Reads {@code RLIMIT_MEMLOCK} and throws {@link IoUringException} if {@code requestedBytes} exceeds a finite soft
     * limit. A best effort: mlock accounting rounds to whole pages and the kernel is the final authority, so a pass
     * here does not guarantee the pin succeeds &mdash; hence {@link #registerBuffers} also handles the kernel's own
     * {@code ENOMEM}/{@code EPERM}.
     */
    private void checkMemlock(long requestedBytes)
    {
        MemorySegment rlimit = arena.allocate(RegLayouts.RLIMIT);
        int ret = LibC.getrlimit(cap, LibC.RLIMIT_MEMLOCK, rlimit);
        if (ret < 0)
            return;   // cannot read the limit; let the register call be the authority
        long softLimit = rlimit.get(JAVA_LONG_UNALIGNED, RegLayouts.RLIMIT_CUR);
        if (softLimit != RegLayouts.RLIM_INFINITY && Long.compareUnsigned(requestedBytes, softLimit) > 0)
            throw new IoUringException("registered buffers (" + requestedBytes + " B) exceed RLIMIT_MEMLOCK soft limit ("
                                       + softLimit + " B); fall back to unregistered READ", Errno.ENOMEM);
    }

    @Override
    public void close()
    {
        arena.close();
    }
}
