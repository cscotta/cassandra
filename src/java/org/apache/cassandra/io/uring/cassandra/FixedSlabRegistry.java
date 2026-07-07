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

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.config.CassandraRelevantProperties;
import org.apache.cassandra.io.uring.async.IoUring;
import org.apache.cassandra.utils.memory.BufferPool;
import org.apache.cassandra.utils.memory.BufferPools;

/**
 * Maps a chunk-cache read buffer to the {@code IORING_OP_READ_FIXED} buffer index of the pool macro-chunk (slab) that
 * contains it, so the read can DMA into a pre-pinned region instead of the kernel pinning the pages per I/O.
 *
 * <p>The chunk-cache {@link BufferPool} hands out direct buffers carved from a small number of large page-aligned
 * macro-chunks. Registering each whole macro-chunk once as a fixed buffer lets every sub-buffer read from it use
 * {@code READ_FIXED} addressed by that slab's {@code buf_index}. Registration is on demand: the first read whose buffer
 * falls inside a not-yet-registered macro-chunk reserves a sparse table (once), then fills the next free slot with that
 * macro-chunk's region on every ring; later reads from the same slab resolve by an address range lookup with no syscall.
 *
 * <p>Off by default &mdash; {@link #resolve} returns {@link #NO_SLOT} unless {@code -Dcassandra.io_uring.read_fixed=true}
 * &mdash; so the standard path is an unregistered {@code IORING_OP_READ}. Any failure (memlock refusal, table full, a
 * buffer not carved from a pool macro-chunk) also yields {@link #NO_SLOT}, and the caller falls back to plain
 * {@code READ}; a hard registration failure disables the registry for the remainder of the process.
 *
 * <p>Thread-safe: lookups are lock-free against a {@link ConcurrentSkipListMap}; the register-on-miss path is
 * serialized and re-checks under the lock.
 */
final class FixedSlabRegistry
{
    static final int NO_SLOT = -1;

    private final IoUring ioUring;
    private final BufferPool pool;
    private final int slabSize;
    private final int capacity;
    private final boolean enabled;

    /** base address of a registered macro-chunk -> its fixed-buffer slot; sorted so floorEntry finds the container. */
    private final ConcurrentSkipListMap<Long, Integer> registered = new ConcurrentSkipListMap<>();
    private final AtomicInteger nextSlot = new AtomicInteger(0);
    private volatile boolean sparseReserved = false;
    private volatile boolean disabled = false;

    FixedSlabRegistry(IoUring ioUring)
    {
        this.ioUring = ioUring;
        this.enabled = CassandraRelevantProperties.IO_URING_READ_FIXED.getBoolean();
        this.capacity = Math.max(1, CassandraRelevantProperties.IO_URING_FIXED_SLAB_SLOTS.getInt());
        // Touch the chunk-cache BufferPool only when READ_FIXED is enabled: its static init pulls in DatabaseDescriptor,
        // which need not be initialized in lightweight tool/test contexts that still use the plain io_uring read path.
        if (enabled)
        {
            this.pool = BufferPools.forChunkCache();
            this.slabSize = pool.macroChunkSize();
        }
        else
        {
            this.pool = null;
            this.slabSize = 0;
        }
    }

    /**
     * Resolves {@code buffer} to the fixed-buffer slot of the pool macro-chunk that fully contains its
     * {@code [position, limit)} region, registering that macro-chunk on demand.
     *
     * @return the {@code buf_index} to pass to {@code READ_FIXED}, or {@link #NO_SLOT} to use unregistered {@code READ}
     */
    int resolve(ByteBuffer buffer)
    {
        if (!enabled || disabled || !buffer.isDirect())
            return NO_SLOT;

        long addr = MemorySegment.ofBuffer(buffer).address();
        long len = buffer.remaining();

        Integer slot = containing(addr, len);
        if (slot != null)
            return slot;
        return registerContaining(addr, len);
    }

    /** The slot of the registered slab containing {@code [addr, addr+len)}, or {@code null} if none is registered yet. */
    private Integer containing(long addr, long len)
    {
        Map.Entry<Long, Integer> e = registered.floorEntry(addr);
        if (e == null)
            return null;
        long base = e.getKey();
        return addr + len <= base + slabSize ? e.getValue() : null;
    }

    private synchronized int registerContaining(long addr, long len)
    {
        if (disabled)
            return NO_SLOT;

        Integer slot = containing(addr, len);   // re-check: another thread may have registered this slab meanwhile
        if (slot != null)
            return slot;

        long base = -1;
        for (long b : pool.macroChunkBaseAddresses())
        {
            if (addr >= b && addr + len <= b + slabSize)
            {
                base = b;
                break;
            }
        }
        if (base < 0)
            return NO_SLOT;   // not carved from a pool macro-chunk (e.g. an oversized one-off allocation)

        if (!sparseReserved)
        {
            try
            {
                ioUring.registerBuffersSparse(capacity);
                sparseReserved = true;
            }
            catch (RuntimeException e)
            {
                disabled = true;
                return NO_SLOT;
            }
        }

        int s = nextSlot.get();
        if (s >= capacity)
            return NO_SLOT;   // table full; remaining slabs read via unregistered READ

        try
        {
            ioUring.registerBuffersUpdate(s, base, slabSize);
        }
        catch (RuntimeException e)
        {
            disabled = true;   // memlock or kernel refusal: stop trying and fall back everywhere
            return NO_SLOT;
        }
        nextSlot.incrementAndGet();
        registered.put(base, s);
        return s;
    }
}
