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
package org.apache.cassandra.io.uring.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture; // checkstyle: permit this import

import org.apache.cassandra.io.uring.abi.Opcodes;

/**
 * One in-flight logical operation: the correlation token, the op parameters, the caller's completion future, and
 * &mdash; critically for memory safety (§9 H1) &mdash; a <b>strong reference to the I/O buffer</b>. The kernel may write
 * into that buffer after {@code io_uring_enter} returns and on another thread, so the buffer must stay reachable until
 * this op's terminal CQE is reaped; the {@link InflightRegistry} holding this object is the keep-alive.
 *
 * <p>A single logical op can span several SQE submissions when the kernel returns a short read/write (§9 H9): the
 * {@code cur*}/{@code remaining} fields track the resubmit cursor, while {@code userData} stays constant (one token per
 * logical op, only ever one SQE in flight for it at a time). Fields are package-private for allocation-free access from
 * the {@link CompletionDispatcher} on the poller thread.
 */
final class PendingOp
{
    long userData;                 // assigned by InflightRegistry at registration (monotonic, never recycled)
    int op;                        // Opcodes.READ / WRITE / FSYNC
    int fd;
    int totalLen;                  // originally requested byte count (0 for fsync)
    long curAddr;                  // buffer address for the next (re)submission
    long fileOffset;               // file offset for the next (re)submission
    int remaining;                 // bytes still outstanding
    int bytesDone;                 // bytes transferred so far across (re)submissions
    int rwFlags;                   // RWF_* for read/write, or fsync flags; 0 by default
    int bufIndex;                  // registered-buffer index for READ_FIXED; unused otherwise
    long submitNanos;              // nanoTime at submitRead, for latency stats
    final ByteBuffer buffer;       // strong keep-alive ref (null for fsync)
    final CompletableFuture<Integer> future = new CompletableFuture<>();

    private PendingOp(int op, int fd, long addr, int len, long off, ByteBuffer buffer)
    {
        this.op = op;
        this.fd = fd;
        this.curAddr = addr;
        this.fileOffset = off;
        this.totalLen = len;
        this.remaining = len;
        this.buffer = buffer;
    }

    static PendingOp read(int fd, long addr, int len, long off, ByteBuffer buffer)
    {
        return new PendingOp(Opcodes.READ, fd, addr, len, off, buffer);
    }

    static PendingOp readFixed(int fd, long addr, int len, long off, ByteBuffer buffer, int bufIndex)
    {
        PendingOp op = new PendingOp(Opcodes.READ_FIXED, fd, addr, len, off, buffer);
        op.bufIndex = bufIndex;
        return op;
    }

    static PendingOp write(int fd, long addr, int len, long off, ByteBuffer buffer)
    {
        return new PendingOp(Opcodes.WRITE, fd, addr, len, off, buffer);
    }

    static PendingOp fsync(int fd)
    {
        return new PendingOp(Opcodes.FSYNC, fd, 0L, 0, 0L, null);
    }

    /** Advance the resubmit cursor after a partial transfer of {@code n} bytes. */
    void advance(int n)
    {
        bytesDone += n;
        remaining -= n;
        curAddr += n;
        fileOffset += n;
    }
}
