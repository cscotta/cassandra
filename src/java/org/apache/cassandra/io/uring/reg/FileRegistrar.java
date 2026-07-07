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
 * Registers SSTable file descriptors with a ring so ops can address them by a small integer slot with
 * {@code IOSQE_FIXED_FILE} (see {@link org.apache.cassandra.io.uring.abi.SqeFlags#FIXED_FILE}), which lets the kernel
 * skip the per-op {@code fget}/{@code fput} on the ring's fixed-file table. The registered table is a dense array
 * indexed {@code 0..nr-1}; an SQE then puts the <em>slot index</em> (not the real fd) in {@code sqe.fd} and sets
 * {@code IOSQE_FIXED_FILE}.
 *
 * <p>Uses the tagged {@code IORING_REGISTER_FILES2} form (kernel &ge; 5.13, {@code IORING_FEAT_RSRC_TAGS}) via
 * {@code struct io_uring_rsrc_register}. The kernel size-checks that opcode, so {@code nr_args} is the struct byte
 * size ({@link RegLayouts#RSRC_REGISTER_BYTES}), not the file count &mdash; the count travels in {@code rsrc.nr}.
 * {@link #registerFilesUpdate(int, int[])} patches slots in place with the untagged {@code io_uring_rsrc_update}
 * form, whose {@code nr_args} <em>is</em> the count.
 *
 * <p>Not thread-safe: like the rest of the ring API, drive one instance from the ring's owning thread. Owns a
 * private {@link Arena} plus a reusable {@code errno} capture segment for its register syscalls; {@link #close()}
 * releases that arena (it does not unregister &mdash; call {@link #unregisterFiles()} first if needed).
 */
public final class FileRegistrar implements AutoCloseable
{
    private final int ringFd;
    private final Arena arena;
    private final MemorySegment cap;

    /** @param ringFd the ring fd from {@link org.apache.cassandra.io.uring.ring.Ring#fd()}. */
    public FileRegistrar(int ringFd)
    {
        this.ringFd = ringFd;
        this.arena = Arena.ofShared();
        this.cap = arena.allocate(Errno.CAPTURE);
    }

    /**
     * Registers {@code fds} into a fresh dense fixed-file table via {@code REGISTER_FILES2}. Slot {@code i} maps to
     * {@code fds[i]}; a subsequent SQE addresses it with {@code sqe.fd = i} and {@code IOSQE_FIXED_FILE}. Any
     * previously registered table must be released with {@link #unregisterFiles()} first ({@code EBUSY} otherwise).
     */
    public void registerFiles(int[] fds)
    {
        if (fds.length == 0)
            throw new IoUringException("registerFiles requires at least one fd");
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment fdArray = scratch.allocate((long) fds.length * Integer.BYTES, Integer.BYTES);
            for (int i = 0; i < fds.length; i++)
                fdArray.set(JAVA_INT_UNALIGNED, (long) i * Integer.BYTES, fds[i]);

            MemorySegment rsrc = scratch.allocate(RegLayouts.RSRC_REGISTER);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_NR, fds.length);
            rsrc.set(JAVA_LONG_UNALIGNED, RegLayouts.RSRC_REGISTER_DATA, fdArray.address());
            registerRsrc(rsrc, "REGISTER_FILES2");
        }
    }

    /**
     * Registers a fully sparse table of {@code nr} empty slots (flags = {@link RegisterOps#RSRC_REGISTER_SPARSE},
     * {@code data = 0}) so slots can be filled later with {@link #registerFilesUpdate(int, int[])}. Preferred over an
     * all-{@code -1} fd array when the fd set is not yet known at ring setup.
     */
    public void registerFilesSparse(int nr)
    {
        if (nr <= 0)
            throw new IoUringException("registerFilesSparse requires nr > 0");
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment rsrc = scratch.allocate(RegLayouts.RSRC_REGISTER);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_NR, nr);
            rsrc.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_REGISTER_FLAGS, RegisterOps.RSRC_REGISTER_SPARSE);
            registerRsrc(rsrc, "REGISTER_FILES2(sparse)");
        }
    }

    /**
     * Updates {@code fds.length} slots of an already-registered table starting at {@code offset}, via
     * {@code REGISTER_FILES_UPDATE} ({@code struct io_uring_rsrc_update}, {@code nr_args} = the count). Slot
     * {@code offset+i} becomes {@code fds[i]}; a value of {@code -2} ({@code IORING_REGISTER_FILES_SKIP}) leaves a
     * slot unchanged.
     */
    public void registerFilesUpdate(int offset, int[] fds)
    {
        if (fds.length == 0)
            throw new IoUringException("registerFilesUpdate requires at least one fd");
        try (Arena scratch = Arena.ofConfined())
        {
            MemorySegment fdArray = scratch.allocate((long) fds.length * Integer.BYTES, Integer.BYTES);
            for (int i = 0; i < fds.length; i++)
                fdArray.set(JAVA_INT_UNALIGNED, (long) i * Integer.BYTES, fds[i]);

            MemorySegment update = scratch.allocate(RegLayouts.RSRC_UPDATE);
            update.set(JAVA_INT_UNALIGNED, RegLayouts.RSRC_UPDATE_OFFSET, offset);
            update.set(JAVA_LONG_UNALIGNED, RegLayouts.RSRC_UPDATE_DATA, fdArray.address());

            int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_FILES_UPDATE, update, fds.length);
            if (ret < 0)
                throw new IoUringException("REGISTER_FILES_UPDATE failed", Errno.of(cap));
        }
    }

    /** Releases the entire registered fixed-file table ({@code arg = NULL}, {@code nr_args = 0}). */
    public void unregisterFiles()
    {
        int ret = Syscalls.register(cap, ringFd, RegisterOps.UNREGISTER_FILES, MemorySegment.NULL, 0);
        if (ret < 0)
            throw new IoUringException("UNREGISTER_FILES failed", Errno.of(cap));
    }

    private void registerRsrc(MemorySegment rsrc, String what)
    {
        int ret = Syscalls.register(cap, ringFd, RegisterOps.REGISTER_FILES2, rsrc, RegLayouts.RSRC_REGISTER_BYTES);
        if (ret < 0)
            throw new IoUringException(what + " failed", Errno.of(cap));
    }

    @Override
    public void close()
    {
        arena.close();
    }
}
