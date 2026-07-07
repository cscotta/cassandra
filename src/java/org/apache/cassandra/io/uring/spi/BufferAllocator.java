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
package org.apache.cassandra.io.uring.spi;

import java.nio.ByteBuffer;

import org.agrona.BufferUtil;

/**
 * SPI seam for the direct, page-aligned buffers the ring hands to the kernel. io_uring reads and writes must point at
 * a stable native address for the whole lifetime of the in-flight operation, so the ring only ever works with direct
 * {@link ByteBuffer}s whose base address it can hand to the kernel as the {@code sqe.addr}. This interface lets the
 * Cassandra adapter later swap in a pooled/slab allocator without the core knowing how the memory is obtained.
 *
 * <p>This type lives in the extractable core (no Cassandra dependency); the only external dependency of the default
 * implementation is agrona, which is already on the build classpath.
 */
public interface BufferAllocator
{
    /**
     * Acquire a direct buffer of at least {@code size} bytes whose base address is aligned to {@code align} bytes.
     * The alignment is what makes the buffer usable for {@code O_DIRECT} I/O (typically the device logical block
     * size, e.g. 512 or 4096). The returned buffer's {@link ByteBuffer#capacity()} is {@code size}.
     *
     * @param size  minimum capacity in bytes
     * @param align base-address alignment in bytes (a power of two)
     * @return a direct buffer ready to be handed to the ring
     */
    ByteBuffer acquire(int size, int align);

    /**
     * Return a buffer previously obtained from {@link #acquire(int, int)}. After this call the caller must not touch
     * the buffer again; a pooling implementation may recycle it. Passing a foreign buffer is undefined.
     */
    void release(ByteBuffer buffer);

    /**
     * The stable native base address of a direct buffer, as an unsigned 64-bit value suitable for {@code sqe.addr}.
     * The buffer must be direct; the address is only valid while the buffer is reachable and not released.
     */
    long address(ByteBuffer buffer);

    /**
     * The default allocator: every {@link #acquire} is a fresh {@code allocateDirectAligned} buffer and
     * {@link #release} is a no-op, leaving reclamation to the JVM cleaner. This is correct but does not pool; the
     * Cassandra adapter is expected to supply a pooling implementation on the hot path.
     */
    final class DirectBufferAllocator implements BufferAllocator
    {
        /** Shared stateless instance; the allocator holds no per-buffer state. */
        public static final DirectBufferAllocator INSTANCE = new DirectBufferAllocator();

        public DirectBufferAllocator()
        {
        }

        @Override
        public ByteBuffer acquire(int size, int align)
        {
            return BufferUtil.allocateDirectAligned(size, align);
        }

        @Override
        public void release(ByteBuffer buffer)
        {
            // No pooling: the buffer is reclaimed by the JVM cleaner once unreachable. Nothing to do here.
        }

        @Override
        public long address(ByteBuffer buffer)
        {
            return BufferUtil.address(buffer);
        }
    }
}
