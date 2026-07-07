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
package org.apache.cassandra.io.uring.cassandra;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture; // checkstyle: permit this import
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.uring.async.IoUring;
import org.apache.cassandra.io.util.AbstractReaderFileProxy;
import org.apache.cassandra.io.util.BufferManagingRebufferer;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.ChunkReader;
import org.apache.cassandra.io.util.Rebufferer;

/**
 * A {@link ChunkReader} that services uncompressed data-file chunk reads through the FFM io_uring binding instead of
 * {@code FileChannel.read}. It runs only on {@code ChunkCache} misses (the cache invokes the reader from its loader),
 * so hot reads never reach it. Following Strategy&nbsp;C, {@link #readChunk} is textually synchronous &mdash; it blocks
 * on the completion future; when the {@code ReadStage} task runs on a virtual thread the carrier unmounts while the
 * device I/O is in flight, and on a platform thread it behaves exactly like the blocking {@code FileChannel} read it
 * replaces. Thread-safe as required (the shared {@link IoUring} facade fans submissions across its ring pool).
 */
public final class IoUringChunkReader extends AbstractReaderFileProxy implements ChunkReader
{
    private final IoUring ioUring;
    private final FixedSlabRegistry slabs;
    private final int fd;
    private final int bufferSize;

    IoUringChunkReader(IoUring ioUring, FixedSlabRegistry slabs, ChannelProxy channel, long fileLength, int bufferSize)
    {
        super(channel, fileLength);
        this.ioUring = ioUring;
        this.slabs = slabs;
        this.bufferSize = bufferSize;
        this.fd = channel.getFileDescriptor();
        if (fd < 0)
            throw new IllegalStateException("could not obtain a file descriptor for io_uring reads of " + channel.filePath());
    }

    @Override
    public void readChunk(long position, ByteBuffer buffer)
    {
        buffer.clear();
        int slot = slabs.resolve(buffer);
        CompletableFuture<Integer> future = slot >= 0 ? ioUring.readFixed(fd, position, buffer, slot)
                                                       : ioUring.read(fd, position, buffer);
        try
        {
            int n = UringAwait.uninterruptibly(future);
            buffer.position(0).limit(Math.max(0, n));
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new FSReadError(cause, channel.filePath());
        }
    }

    @Override
    public int chunkSize()
    {
        return bufferSize;
    }

    @Override
    public BufferType preferredBufferType()
    {
        // io_uring reads require a native/direct buffer (the kernel DMAs into it), so force OFF_HEAP regardless of the
        // configured default. ChunkCache honours this and allocates page-aligned direct buffers from the chunk pool.
        return BufferType.OFF_HEAP;
    }

    @Override
    public Rebufferer instantiateRebufferer(boolean forScan)
    {
        if (Integer.bitCount(bufferSize) == 1)
            return new BufferManagingRebufferer.Aligned(this);
        return new BufferManagingRebufferer.Unaligned(this);
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s - chunk length %d, data length %d)",
                             getClass().getSimpleName(), channel.filePath(), bufferSize, fileLength());
    }
}
