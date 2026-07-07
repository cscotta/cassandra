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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * Thin, allocation-free accessors over a 64-byte {@code io_uring_sqe} slice of the SQE mmap. Uses offset-based
 * unaligned value accessors so field access never trips FFM alignment checks on the packed struct.
 *
 * <p>{@link #clear(MemorySegment)} must be called before filling a reused SQE slot: {@code get_sqe} hands back a
 * slot that still holds a previous op's bytes, and a stale {@code buf_index}/{@code rw_flags}/{@code personality}
 * is the single most common hand-rolled-ring corruption bug.
 */
public final class Sqe
{
    private Sqe() {}

    /** Zeroes the whole 64-byte SQE. The argument must be a slice of exactly {@link Layouts#SQE_BYTES} bytes. */
    public static void clear(MemorySegment sqe)
    {
        sqe.fill((byte) 0);
    }

    public static void opcode(MemorySegment sqe, int op)
    {
        sqe.set(JAVA_BYTE, Layouts.SQE_OPCODE, (byte) op);
    }

    public static void flags(MemorySegment sqe, int flags)
    {
        sqe.set(JAVA_BYTE, Layouts.SQE_FLAGS, (byte) flags);
    }

    public static void ioprio(MemorySegment sqe, int ioprio)
    {
        sqe.set(JAVA_SHORT_UNALIGNED, Layouts.SQE_IOPRIO, (short) ioprio);
    }

    public static void fd(MemorySegment sqe, int fd)
    {
        sqe.set(JAVA_INT_UNALIGNED, Layouts.SQE_FD, fd);
    }

    public static void off(MemorySegment sqe, long off)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_OFF, off);
    }

    public static void addr(MemorySegment sqe, long addr)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_ADDR, addr);
    }

    public static void len(MemorySegment sqe, int len)
    {
        sqe.set(JAVA_INT_UNALIGNED, Layouts.SQE_LEN, len);
    }

    public static void rwFlags(MemorySegment sqe, int rwFlags)
    {
        sqe.set(JAVA_INT_UNALIGNED, Layouts.SQE_RW_FLAGS, rwFlags);
    }

    public static void userData(MemorySegment sqe, long userData)
    {
        sqe.set(JAVA_LONG_UNALIGNED, Layouts.SQE_USER_DATA, userData);
    }

    public static void bufIndex(MemorySegment sqe, int bufIndex)
    {
        sqe.set(JAVA_SHORT_UNALIGNED, Layouts.SQE_BUF_INDEX, (short) bufIndex);
    }

    public static int opcodeOf(MemorySegment sqe)
    {
        return sqe.get(JAVA_BYTE, Layouts.SQE_OPCODE) & 0xFF;
    }

    public static long userDataOf(MemorySegment sqe)
    {
        return sqe.get(JAVA_LONG_UNALIGNED, Layouts.SQE_USER_DATA);
    }
}
