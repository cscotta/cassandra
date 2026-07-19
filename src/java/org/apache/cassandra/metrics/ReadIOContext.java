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

package org.apache.cassandra.metrics;

import java.util.HashSet;
import java.util.Set;

/**
 * A mutable, thread-confined accumulator of the IO a single read request issues on its serving thread.
 *
 * One instance lives in a thread-local for the duration of a read-stage task (see {@link ReadIOTracker}).
 * The low-level IO seams ({@code ChannelProxy.read}, the chunk cache, the compressed chunk readers, and the
 * per-sstable read listener) increment the fields here; {@code ReadCommand} snapshots the context before and
 * after executing a command and rolls the delta into the per-table IO histograms.
 *
 * It is not synchronised: a context is only ever touched by the single thread that serves the request. Because
 * the counters are page-cache-agnostic (a positional {@code pread} is recorded whether it is served from the OS
 * page cache or the device), {@link #physicalBytesRead} is "bytes requested at the syscall boundary" — the split
 * between page-cache hits and real device IO is measured out-of-process by the eBPF collectors.
 */
public class ReadIOContext
{
    /** Upper bound on the number of distinct file paths tracked, to bound per-request memory. */
    static final int MAX_TRACKED_FILES = 128;

    private long physicalBytesRead;
    private long physicalReadOps;
    private long bytesDecompressed;
    private long chunkCacheAccesses;
    private long chunkCacheMisses;

    // Distinct component files touched while serving the request; bounded by MAX_TRACKED_FILES.
    private final Set<String> filesTouched = new HashSet<>();
    private long filesTouchedOverflow;

    /**
     * Record one positional read syscall. {@code bytes} is the value returned by {@code FileChannel.read}: it may
     * be negative at EOF, in which case no bytes are added but the op is still counted (a syscall did occur).
     */
    public void addPhysicalRead(long bytes)
    {
        physicalReadOps++;
        if (bytes > 0)
            physicalBytesRead += bytes;
    }

    public void addDecompressed(long bytes)
    {
        if (bytes > 0)
            bytesDecompressed += bytes;
    }

    public void addChunkCacheAccess()
    {
        chunkCacheAccesses++;
    }

    public void addChunkCacheMiss()
    {
        chunkCacheMisses++;
    }

    /** Record that {@code path} was read from while serving the request (deduplicated, bounded). */
    public void addFile(String path)
    {
        if (path == null || filesTouched.contains(path))
            return;
        if (filesTouched.size() < MAX_TRACKED_FILES)
            filesTouched.add(path);
        else
            filesTouchedOverflow++;
    }

    public int distinctFilesTouched()
    {
        return filesTouched.size() + (int) Math.min(filesTouchedOverflow, Integer.MAX_VALUE - filesTouched.size());
    }

    public Snapshot snapshot()
    {
        return new Snapshot(physicalBytesRead, physicalReadOps, bytesDecompressed,
                            chunkCacheAccesses, chunkCacheMisses, distinctFilesTouched());
    }

    /**
     * An immutable point-in-time view of a {@link ReadIOContext}. {@link #minus(Snapshot)} yields the IO that
     * accrued between two snapshots, which is how a single command's contribution is isolated from any other IO
     * on the same thread.
     */
    public static final class Snapshot
    {
        public static final Snapshot ZERO = new Snapshot(0, 0, 0, 0, 0, 0);

        public final long physicalBytesRead;
        public final long physicalReadOps;
        public final long bytesDecompressed;
        public final long chunkCacheAccesses;
        public final long chunkCacheMisses;
        public final long filesTouched;

        Snapshot(long physicalBytesRead, long physicalReadOps, long bytesDecompressed,
                 long chunkCacheAccesses, long chunkCacheMisses, long filesTouched)
        {
            this.physicalBytesRead = physicalBytesRead;
            this.physicalReadOps = physicalReadOps;
            this.bytesDecompressed = bytesDecompressed;
            this.chunkCacheAccesses = chunkCacheAccesses;
            this.chunkCacheMisses = chunkCacheMisses;
            this.filesTouched = filesTouched;
        }

        public long chunkCacheHits()
        {
            return Math.max(0, chunkCacheAccesses - chunkCacheMisses);
        }

        /** Returns a snapshot whose fields are {@code this - other}, clamped at zero. */
        public Snapshot minus(Snapshot other)
        {
            if (other == null)
                return this;
            return new Snapshot(Math.max(0, physicalBytesRead - other.physicalBytesRead),
                                Math.max(0, physicalReadOps - other.physicalReadOps),
                                Math.max(0, bytesDecompressed - other.bytesDecompressed),
                                Math.max(0, chunkCacheAccesses - other.chunkCacheAccesses),
                                Math.max(0, chunkCacheMisses - other.chunkCacheMisses),
                                Math.max(0, filesTouched - other.filesTouched));
        }
    }
}
