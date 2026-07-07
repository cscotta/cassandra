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

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.uring.async.IoUring;
import org.apache.cassandra.io.uring.probe.Capabilities;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.uring.spi.IoUringConfig;
import org.apache.cassandra.io.uring.spi.RingThreadFactory;
import org.apache.cassandra.io.uring.spi.UringLogger;
import org.apache.cassandra.io.uring.spi.UringStatsListener;
import org.apache.cassandra.io.util.AsyncFrameReader;
import org.apache.cassandra.io.util.AsyncReadProvider;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.ChunkReader;
import org.apache.cassandra.utils.FBUtilities;

/**
 * The {@link AsyncReadProvider} implementation backed by the FFM io_uring binding, discovered via
 * {@code META-INF/services}. This class exists only in the JDK-25 build (its package is compile-excluded on other
 * JDKs), so the core read path reaches io_uring exclusively through the {@link AsyncReadProvider} interface. It owns a
 * single process-wide {@link IoUring} facade, opened lazily on first use from the operator's {@code io_uring_*}
 * settings, and hands out {@link IoUringChunkReader}s that share it.
 *
 * <p>Must have a public no-arg constructor for {@link java.util.ServiceLoader}.
 */
public final class IoUringReadProvider implements AsyncReadProvider
{
    private volatile Handles handles;

    public IoUringReadProvider()
    {
    }

    @Override
    public boolean isAvailable()
    {
        return FBUtilities.isLinux && IoUringAvailability.check().isAvailable();
    }

    @Override
    public String unavailableReason()
    {
        if (!FBUtilities.isLinux)
            return "io_uring requires Linux";
        Capabilities caps = IoUringAvailability.check();
        return caps.isAvailable() ? "available" : caps.reason();
    }

    @Override
    public ChunkReader newChunkReader(ChannelProxy channel, long fileLength, BufferType bufferType, int chunkSize)
    {
        Handles h = ensureOpen();
        return new IoUringChunkReader(h.ioUring, h.slabs, channel, fileLength, chunkSize);
    }

    @Override
    public AsyncFrameReader newFrameReader(ChannelProxy channel, boolean directIo)
    {
        Handles h = ensureOpen();
        return new IoUringFrameReader(h.ioUring, h.slabs, channel);
    }

    @Override
    public synchronized void shutdown()
    {
        Handles h = handles;
        if (h != null)
        {
            handles = null;
            h.ioUring.close();
            if (h.stats instanceof CassandraUringMetrics)
                ((CassandraUringMetrics) h.stats).release();
        }
    }

    private Handles ensureOpen()
    {
        Handles local = handles;
        if (local == null)
        {
            synchronized (this)
            {
                local = handles;
                if (local == null)
                {
                    UringStatsListener stats = newStatsListener();
                    IoUring opened = IoUring.open(configFromSettings(),
                                                  RingThreadFactory.DaemonRingThreadFactory.INSTANCE,
                                                  UringLogger.Slf4jUringLogger.forClass(IoUringReadProvider.class),
                                                  stats);
                    FixedSlabRegistry registry;
                    try
                    {
                        registry = new FixedSlabRegistry(opened);
                    }
                    catch (RuntimeException | Error e)
                    {
                        opened.close();   // don't leak poller threads / ring fd / eventfd if the registry ctor fails
                        if (stats instanceof CassandraUringMetrics)
                            ((CassandraUringMetrics) stats).release();
                        throw e;
                    }
                    // Publish the (ioUring, slabs, stats) triple atomically and only once all are built, so no caller
                    // observes a half-initialized provider and a concurrent shutdown cannot inject a null into any.
                    local = handles = new Handles(opened, registry, stats);
                }
            }
        }
        return local;
    }

    /**
     * A {@link CassandraUringMetrics} listener when the metrics registry is usable, else {@link UringStatsListener#NOOP}
     * &mdash; a lightweight tool/test context may lack the metrics/JMX subsystem, and observability must never block
     * the read path from opening.
     */
    private static UringStatsListener newStatsListener()
    {
        try
        {
            return new CassandraUringMetrics();
        }
        catch (Throwable t)
        {
            return UringStatsListener.NOOP;
        }
    }

    private static IoUringConfig configFromSettings()
    {
        try
        {
            return IoUringConfig.builder()
                                .queueDepth(DatabaseDescriptor.getIoUringQueueDepth())
                                .pollerThreads(DatabaseDescriptor.getIoUringPollerThreads())
                                .sqpoll(DatabaseDescriptor.getIoUringSqpoll())
                                .directIo(DatabaseDescriptor.getIoUringDirectIo())
                                .fallbackOnUnavailable(DatabaseDescriptor.getIoUringFallbackOnUnavailable())
                                .build();
        }
        catch (Throwable t)
        {
            // DatabaseDescriptor config not initialized (e.g. a lightweight test/tool context): use library defaults.
            return IoUringConfig.defaults();
        }
    }

    /** The atomically-published facade, its fixed-buffer registry, and its stats listener, or {@code null} once shut down. */
    private static final class Handles
    {
        final IoUring ioUring;
        final FixedSlabRegistry slabs;
        final UringStatsListener stats;

        Handles(IoUring ioUring, FixedSlabRegistry slabs, UringStatsListener stats)
        {
            this.ioUring = ioUring;
            this.slabs = slabs;
            this.stats = stats;
        }
    }
}
