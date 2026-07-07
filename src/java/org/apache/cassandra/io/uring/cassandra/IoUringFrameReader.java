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
import org.apache.cassandra.io.uring.async.IoUring;
import org.apache.cassandra.io.util.AsyncFrameReader;
import org.apache.cassandra.io.util.ChannelProxy;

/**
 * The {@link AsyncFrameReader} that services the compressed data path's single compressed-frame read through the FFM
 * io_uring binding. The core {@code CompressedChunkReader.IoUring} owns the block-alignment / CRC / decompress logic
 * and calls this only to turn (file descriptor, position) into a device read into its direct staging buffer. Like the
 * uncompressed reader it blocks via {@link UringAwait} so the DMA target is never abandoned while the kernel may still
 * be writing it. The compressed staging buffer is a per-thread scratch buffer (never a registered chunk-cache slab),
 * so {@link FixedSlabRegistry#resolve} returns {@code NO_SLOT} and a plain {@code IORING_OP_READ} is used.
 */
final class IoUringFrameReader implements AsyncFrameReader
{
    private final IoUring ioUring;
    private final FixedSlabRegistry slabs;
    private final ChannelProxy channel;
    private final int fd;

    IoUringFrameReader(IoUring ioUring, FixedSlabRegistry slabs, ChannelProxy channel)
    {
        this.ioUring = ioUring;
        this.slabs = slabs;
        this.channel = channel;
        this.fd = channel.getFileDescriptor();
        if (fd < 0)
            throw new IllegalStateException("could not obtain a file descriptor for io_uring compressed reads of " + channel.filePath());
    }

    @Override
    public int read(ByteBuffer dst, long position)
    {
        int slot = slabs.resolve(dst);
        CompletableFuture<Integer> future = slot >= 0 ? ioUring.readFixed(fd, position, dst, slot)
                                                       : ioUring.read(fd, position, dst);
        try
        {
            return UringAwait.uninterruptibly(future);
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new FSReadError(cause, channel.filePath());
        }
    }
}
