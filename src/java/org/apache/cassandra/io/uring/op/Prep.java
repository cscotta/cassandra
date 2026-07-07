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
import org.apache.cassandra.io.uring.abi.Sqe;

/**
 * Op-prep helpers: cheap field setters over a 64-byte SQE slice. {@link #prepRw} is the base every other prep is a
 * thin wrapper over; it {@link Sqe#clear zeroes the whole SQE first} so a reused slot cannot leak stale union bytes
 * (the #1 hand-rolled-ring bug). This is the v1 storage set; the full network/filesystem/control prep surface is
 * added by sibling {@code PrepNet}/{@code PrepFs}/{@code PrepCtl} helpers in the general-purpose library.
 */
public final class Prep
{
    private Prep() {}

    /** Base prep: clears the SQE, then sets opcode/fd/off/addr/len (all other fields remain zeroed). */
    public static void prepRw(MemorySegment sqe, int op, int fd, long addr, int len, long off)
    {
        Sqe.clear(sqe);
        Sqe.opcode(sqe, op);
        Sqe.fd(sqe, fd);
        Sqe.off(sqe, off);
        Sqe.addr(sqe, addr);
        Sqe.len(sqe, len);
    }

    /** {@code IORING_OP_READ}: pread of {@code len} bytes at file offset {@code off} into the buffer at {@code addr}. */
    public static void prepRead(MemorySegment sqe, int fd, long addr, int len, long off)
    {
        prepRw(sqe, Opcodes.READ, fd, addr, len, off);
    }

    /** {@code IORING_OP_READV}: {@code addr} points at an {@code iovec[]}, {@code nr} is the vector count. */
    public static void prepReadv(MemorySegment sqe, int fd, long iovecsAddr, int nr, long off)
    {
        prepRw(sqe, Opcodes.READV, fd, iovecsAddr, nr, off);
    }

    /** {@code IORING_OP_READ_FIXED}: {@code addr} is inside a registered buffer identified by {@code bufIndex}. */
    public static void prepReadFixed(MemorySegment sqe, int fd, long addr, int len, long off, int bufIndex)
    {
        prepRw(sqe, Opcodes.READ_FIXED, fd, addr, len, off);
        Sqe.bufIndex(sqe, bufIndex);
    }

    /** {@code IORING_OP_WRITE}: pwrite of {@code len} bytes at file offset {@code off} from the buffer at {@code addr}. */
    public static void prepWrite(MemorySegment sqe, int fd, long addr, int len, long off)
    {
        prepRw(sqe, Opcodes.WRITE, fd, addr, len, off);
    }

    /** {@code IORING_OP_FSYNC}: {@code fsyncFlags} is 0 for fsync or {@link Opcodes#IORING_FSYNC_DATASYNC}. */
    public static void prepFsync(MemorySegment sqe, int fd, int fsyncFlags)
    {
        prepRw(sqe, Opcodes.FSYNC, fd, 0L, 0, 0L);
        Sqe.rwFlags(sqe, fsyncFlags);
    }

    /** {@code IORING_OP_NOP}: completes immediately; used for the availability probe and as a barrier/keepalive. */
    public static void prepNop(MemorySegment sqe)
    {
        prepRw(sqe, Opcodes.NOP, -1, 0L, 0, 0L);
    }

    /** Sets the {@code user_data} correlation token echoed back in the CQE. */
    public static void setData64(MemorySegment sqe, long userData)
    {
        Sqe.userData(sqe, userData);
    }

    /** Sets the {@code IOSQE_*} per-SQE flags. */
    public static void setFlags(MemorySegment sqe, int iosqeFlags)
    {
        Sqe.flags(sqe, iosqeFlags);
    }
}
