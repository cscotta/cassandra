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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.cache.ChunkCache;
import org.apache.cassandra.config.CassandraRelevantProperties;
import org.apache.cassandra.utils.MBeanWrapper;

import io.netty.util.concurrent.FastThreadLocal;

/**
 * Per-request IO attribution: brackets a read-stage task with a {@link ReadIOContext} held in a thread-local, so
 * the low-level IO seams can attribute the bytes/ops they issue to the request currently being served.
 *
 * The tracker is off by default (system property {@code cassandra.io_tracking.enabled}) and can be toggled at
 * runtime over JMX, so a benchmark harness can enable exact per-request accounting for a cold micro-benchmark and
 * disable it under full load. When disabled the hot path costs one predictable volatile read plus a branch — no
 * thread-local access and no allocation.
 *
 * Attribution is valid only while a request's reads run on the same thread that owns the context (the read-stage
 * thread), which is the case for the buffered/{@code standard} data path. mmap page faults and io_uring
 * poller-thread reads are not seen here; those are measured out-of-process by the eBPF collectors.
 */
public final class ReadIOTracker
{
    private static final Logger logger = LoggerFactory.getLogger(ReadIOTracker.class);

    public static final String MBEAN_NAME = "org.apache.cassandra.metrics:type=ReadIOTracker";

    private static final FastThreadLocal<ReadIOContext> CONTEXT = new FastThreadLocal<>();

    private static volatile boolean enabled = CassandraRelevantProperties.IO_TRACKING_ENABLED.getBoolean();

    static
    {
        MBeanWrapper.instance.registerMBean(new ReadIOTrackerMXBean() {}, MBEAN_NAME, MBeanWrapper.OnException.LOG);
    }

    private ReadIOTracker()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(boolean value)
    {
        enabled = value;
        logger.info("Per-request read IO tracking {}", value ? "enabled" : "disabled");
    }

    /** Start accounting for the request served on this thread. No-op when tracking is disabled. */
    public static void begin()
    {
        if (enabled)
            CONTEXT.set(new ReadIOContext());
    }

    /** End accounting for this thread's request and release the context. Safe to call unconditionally. */
    public static void endAndClear()
    {
        CONTEXT.remove();
    }

    /** The context bound to this thread, or {@code null} if none is active (never allocates). */
    public static ReadIOContext currentOrNull()
    {
        return CONTEXT.getIfExists();
    }

    /** Snapshot of this thread's context, or {@code null} if none is active. */
    public static ReadIOContext.Snapshot snapshotOrNull()
    {
        ReadIOContext c = CONTEXT.getIfExists();
        return c == null ? null : c.snapshot();
    }

    // -- record seams (call sites gate with isEnabled(); these no-op when no context is bound) --

    public static void recordPhysicalRead(long bytes, String path)
    {
        ReadIOContext c = CONTEXT.getIfExists();
        if (c != null)
        {
            c.addPhysicalRead(bytes);
            c.addFile(path);
        }
    }

    public static void recordDecompressed(long bytes)
    {
        ReadIOContext c = CONTEXT.getIfExists();
        if (c != null)
            c.addDecompressed(bytes);
    }

    public static void recordChunkCacheAccess()
    {
        ReadIOContext c = CONTEXT.getIfExists();
        if (c != null)
            c.addChunkCacheAccess();
    }

    public static void recordChunkCacheMiss()
    {
        ReadIOContext c = CONTEXT.getIfExists();
        if (c != null)
            c.addChunkCacheMiss();
    }

    public static void recordSSTable(String path)
    {
        ReadIOContext c = CONTEXT.getIfExists();
        if (c != null)
            c.addFile(path);
    }

    /**
     * JMX control surface so a benchmark harness can flip tracking and drop the chunk cache without restarting
     * the node. Named with the {@code MXBean} suffix so JMX accepts an implementing object of any class.
     */
    public interface ReadIOTrackerMXBean
    {
        default boolean isEnabled()
        {
            return ReadIOTracker.isEnabled();
        }

        default void setEnabled(boolean value)
        {
            ReadIOTracker.setEnabled(value);
        }

        /** Evict the on-heap/off-heap chunk cache (no-op if the chunk cache is disabled). */
        default void clearChunkCache()
        {
            if (ChunkCache.instance != null)
                ChunkCache.instance.clear();
        }
    }
}
