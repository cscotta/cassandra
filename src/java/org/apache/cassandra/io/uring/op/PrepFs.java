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

import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.SqeExt;

/**
 * Filesystem-op preps mirroring liburing's {@code io_uring_prep_*} path/metadata wrappers. Present for library
 * completeness; the storage read path does not currently open, rename or unlink through the ring, though these are
 * the natural building blocks for async SSTable open/rename/preallocate should that ever be wired in.
 *
 * <p>Every prep first calls {@link Prep#prepRw} (which zeroes the whole 64-byte SQE) and then sets the op-specific
 * extras via {@link SqeExt}. Path/struct arguments ({@code pathAddr}, {@code howAddr}, {@code statxbufAddr},
 * {@code fdsAddr}) are raw native addresses; the caller owns the backing {@code MemorySegment} and must keep it
 * alive until the CQE is reaped. {@code dfd} follows the {@code *at(2)} convention ({@code AT_FDCWD} for cwd-relative).
 * {@code *Direct} variants install a registered-file descriptor via {@link TargetFixedFile}.
 */
public final class PrepFs
{
    private PrepFs() {}

    /**
     * {@code IORING_OP_OPENAT}: openat(2). {@code mode} rides in {@code len}, the open {@code flags} ({@code O_*}) in
     * the {@code open_flags} union word.
     */
    public static void openat(MemorySegment sqe, int dfd, long pathAddr, int flags, int mode)
    {
        Prep.prepRw(sqe, Opcodes.OPENAT, dfd, pathAddr, mode, 0L);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_OPENAT2}: openat2(2). {@code howAddr} points at a {@code struct open_how} of {@code howLen}
     * bytes (24 on current kernels); the struct rides in {@code off} with its size in {@code len}.
     */
    public static void openat2(MemorySegment sqe, int dfd, long pathAddr, long howAddr, int howLen)
    {
        Prep.prepRw(sqe, Opcodes.OPENAT2, dfd, pathAddr, howLen, howAddr);
    }

    /**
     * {@code IORING_OP_OPENAT} into a registered-file slot: like {@link #openat} but the opened file is installed as a
     * fixed descriptor at {@code fileIndex} (or {@link TargetFixedFile#ALLOC} to auto-allocate).
     */
    public static void openatDirect(MemorySegment sqe, int dfd, long pathAddr, int flags, int mode, int fileIndex)
    {
        openat(sqe, dfd, pathAddr, flags, mode);
        TargetFixedFile.set(sqe, fileIndex);
    }

    /**
     * {@code IORING_OP_OPENAT2} into a registered-file slot: like {@link #openat2} but the opened file is installed as
     * a fixed descriptor at {@code fileIndex} (or {@link TargetFixedFile#ALLOC} to auto-allocate).
     */
    public static void openat2Direct(MemorySegment sqe, int dfd, long pathAddr, long howAddr, int howLen, int fileIndex)
    {
        openat2(sqe, dfd, pathAddr, howAddr, howLen);
        TargetFixedFile.set(sqe, fileIndex);
    }

    /** {@code IORING_OP_CLOSE}: close(2) the real descriptor {@code fd}. */
    public static void close(MemorySegment sqe, int fd)
    {
        Prep.prepRw(sqe, Opcodes.CLOSE, fd, 0L, 0, 0L);
    }

    /**
     * {@code IORING_OP_CLOSE} of a registered-file slot: closes the fixed descriptor at {@code fileIndex} (the real
     * {@code fd} field is left zero, per liburing).
     */
    public static void closeDirect(MemorySegment sqe, int fileIndex)
    {
        close(sqe, 0);
        TargetFixedFile.set(sqe, fileIndex);
    }

    /**
     * {@code IORING_OP_STATX}: statx(2). {@code mask} (the {@code STATX_*} request mask) rides in {@code len},
     * {@code statxbufAddr} (the out {@code struct statx}) in {@code off}, and the {@code AT_*} {@code flags} in the
     * {@code statx_flags} union word.
     */
    public static void statx(MemorySegment sqe, int dfd, long pathAddr, int flags, int mask, long statxbufAddr)
    {
        Prep.prepRw(sqe, Opcodes.STATX, dfd, pathAddr, mask, statxbufAddr);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_RENAMEAT}: renameat2(2). {@code newDfd} rides in {@code len}, {@code newPathAddr} in
     * {@code off}, and the {@code RENAME_*} {@code flags} in the {@code rename_flags} union word.
     */
    public static void renameat(MemorySegment sqe, int oldDfd, long oldPathAddr, int newDfd, long newPathAddr, int flags)
    {
        Prep.prepRw(sqe, Opcodes.RENAMEAT, oldDfd, oldPathAddr, newDfd, newPathAddr);
        SqeExt.opFlags(sqe, flags);
    }

    /** {@code IORING_OP_UNLINKAT}: unlinkat(2). The {@code AT_*} {@code flags} (e.g. {@code AT_REMOVEDIR}) ride in {@code unlink_flags}. */
    public static void unlinkat(MemorySegment sqe, int dfd, long pathAddr, int flags)
    {
        Prep.prepRw(sqe, Opcodes.UNLINKAT, dfd, pathAddr, 0, 0L);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_LINKAT}: linkat(2). {@code newDfd} rides in {@code len}, {@code newPathAddr} in {@code off},
     * and the {@code AT_*} {@code flags} in the {@code hardlink_flags} union word.
     */
    public static void linkat(MemorySegment sqe, int oldDfd, long oldPathAddr, int newDfd, long newPathAddr, int flags)
    {
        Prep.prepRw(sqe, Opcodes.LINKAT, oldDfd, oldPathAddr, newDfd, newPathAddr);
        SqeExt.opFlags(sqe, flags);
    }

    /** {@code IORING_OP_MKDIRAT}: mkdirat(2). {@code mode} rides in {@code len}. */
    public static void mkdirat(MemorySegment sqe, int dfd, long pathAddr, int mode)
    {
        Prep.prepRw(sqe, Opcodes.MKDIRAT, dfd, pathAddr, mode, 0L);
    }

    /**
     * {@code IORING_OP_SYMLINKAT}: symlinkat(2). {@code targetAddr} (the link contents) rides in {@code addr},
     * {@code linkPathAddr} (the new symlink path, relative to {@code newDirFd}) in {@code off}.
     */
    public static void symlinkat(MemorySegment sqe, long targetAddr, int newDirFd, long linkPathAddr)
    {
        Prep.prepRw(sqe, Opcodes.SYMLINKAT, newDirFd, targetAddr, 0, linkPathAddr);
    }

    /**
     * {@code IORING_OP_SPLICE}: splice(2). {@code nbytes} rides in {@code len}, {@code offOut} in {@code off},
     * {@code offIn} in {@code splice_off_in}, {@code fdIn} in {@code splice_fd_in}, and the {@code SPLICE_F_*}
     * {@code spliceFlags} in the {@code splice_flags} union word. Use {@code -1} for an offset the fd's position tracks.
     */
    public static void splice(MemorySegment sqe, int fdIn, long offIn, int fdOut, long offOut, int nbytes, int spliceFlags)
    {
        Prep.prepRw(sqe, Opcodes.SPLICE, fdOut, 0L, nbytes, offOut);
        SqeExt.spliceOffIn(sqe, offIn);
        SqeExt.spliceFdIn(sqe, fdIn);
        SqeExt.opFlags(sqe, spliceFlags);
    }

    /** {@code IORING_OP_TEE}: tee(2). Duplicates {@code nbytes} between two pipes without consuming; {@code fdIn} rides in {@code splice_fd_in}. */
    public static void tee(MemorySegment sqe, int fdIn, int fdOut, int nbytes, int spliceFlags)
    {
        Prep.prepRw(sqe, Opcodes.TEE, fdOut, 0L, nbytes, 0L);
        SqeExt.spliceOffIn(sqe, 0L);
        SqeExt.spliceFdIn(sqe, fdIn);
        SqeExt.opFlags(sqe, spliceFlags);
    }

    /**
     * {@code IORING_OP_FILES_UPDATE}: patch the registered-file table inline. {@code fdsAddr} points at an
     * {@code int[]} of {@code nr} descriptors, written starting at slot {@code offset}.
     */
    public static void filesUpdate(MemorySegment sqe, long fdsAddr, int nr, long offset)
    {
        Prep.prepRw(sqe, Opcodes.FILES_UPDATE, -1, fdsAddr, nr, offset);
    }
}
