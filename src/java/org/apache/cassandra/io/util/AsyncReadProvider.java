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
package org.apache.cassandra.io.util;

import org.apache.cassandra.io.compress.BufferType;

/**
 * The ServiceLoader boundary between Cassandra's read path and the JDK-25-only FFM {@code io.uring} binding. Core
 * classes ({@code DatabaseDescriptor}, {@code FileHandle}, {@code StartupChecks}) compile on JDK 11/17/21/25 and must
 * never reference {@code java.lang.foreign} or {@code org.apache.cassandra.io.uring.*} directly; they reach io_uring
 * only through this interface, whose signature uses core types alone. The implementation lives in the io_uring module
 * (compiled only on JDK 25) and is discovered via {@link AsyncReadProviders}; on any JDK where it is absent or io_uring
 * is unusable, {@link #isAvailable()} is {@code false} and the read path falls back to {@code standard}.
 */
public interface AsyncReadProvider
{
    /** Whether io_uring is usable on this host right now (probed once). When false, callers must fall back. */
    boolean isAvailable();

    /** Human-readable reason io_uring is unavailable (for startup logging), or a note when available. */
    String unavailableReason();

    /**
     * Build a thread-safe {@link ChunkReader} that services chunk reads for an uncompressed data file through io_uring.
     * The {@code channel} is already open (its {@link ChannelProxy#getFileDescriptor() fd} is what the ring reads).
     * Called only when {@link #isAvailable()} is true. Implementations open their ring pool lazily/idempotently.
     */
    ChunkReader newChunkReader(ChannelProxy channel, long fileLength, BufferType bufferType, int chunkSize);

    /**
     * Build an {@link AsyncFrameReader} that services positional frame reads (the single compressed-frame read of the
     * compressed data path) for {@code channel} through io_uring. {@code directIo} signals the channel was opened
     * {@code O_DIRECT} (the caller has already block-aligned the offset/length/buffer). Called only when
     * {@link #isAvailable()} is true.
     */
    AsyncFrameReader newFrameReader(ChannelProxy channel, boolean directIo);

    /** Release the ring pool and any native resources. Idempotent; called on node shutdown. */
    void shutdown();
}
