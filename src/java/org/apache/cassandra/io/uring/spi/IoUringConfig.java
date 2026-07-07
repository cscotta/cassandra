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

/**
 * Immutable, dependency-free tuning parameters for a ring. This is pure data: it holds no reference to Cassandra's
 * {@code Config}/{@code DatabaseDescriptor} and reads no system properties, so it can be built by the Cassandra
 * adapter from whatever source it likes and handed to the extractable core unchanged.
 *
 * <p>Instances are created through {@link #builder()}; {@link #defaults()} returns the all-defaults configuration.
 */
public final class IoUringConfig
{
    /** Default number of SQ/CQ entries requested at setup; the kernel rounds this up to a power of two. */
    public static final int DEFAULT_QUEUE_DEPTH = 256;
    /** Default number of poller threads driving completions. */
    public static final int DEFAULT_POLLER_THREADS = 1;
    /** Default I/O block size in bytes, used for {@code O_DIRECT} alignment. */
    public static final int DEFAULT_BLOCK_SIZE = 4096;

    private final int queueDepth;
    private final int pollerThreads;
    private final boolean sqpoll;
    private final boolean directIo;
    private final boolean fallbackOnUnavailable;
    private final int blockSize;

    private IoUringConfig(Builder builder)
    {
        this.queueDepth = builder.queueDepth;
        this.pollerThreads = builder.pollerThreads;
        this.sqpoll = builder.sqpoll;
        this.directIo = builder.directIo;
        this.fallbackOnUnavailable = builder.fallbackOnUnavailable;
        this.blockSize = builder.blockSize;
    }

    /** A fresh builder pre-populated with the defaults. */
    public static Builder builder()
    {
        return new Builder();
    }

    /** The all-defaults configuration. */
    public static IoUringConfig defaults()
    {
        return builder().build();
    }

    /** Requested number of SQ/CQ entries; the kernel rounds up to a power of two. */
    public int queueDepth()
    {
        return queueDepth;
    }

    /** Number of threads driving the ring's completion loop. */
    public int pollerThreads()
    {
        return pollerThreads;
    }

    /** Whether to request kernel-side submission-queue polling ({@code IORING_SETUP_SQPOLL}). */
    public boolean sqpoll()
    {
        return sqpoll;
    }

    /** Whether files are opened with {@code O_DIRECT}, requiring {@link #blockSize()}-aligned buffers and offsets. */
    public boolean directIo()
    {
        return directIo;
    }

    /** Whether an unavailable or unusable ring should silently fall back to blocking I/O rather than failing. */
    public boolean fallbackOnUnavailable()
    {
        return fallbackOnUnavailable;
    }

    /** I/O block size in bytes used for {@code O_DIRECT} alignment. */
    public int blockSize()
    {
        return blockSize;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof IoUringConfig))
            return false;
        IoUringConfig that = (IoUringConfig) o;
        return queueDepth == that.queueDepth
               && pollerThreads == that.pollerThreads
               && sqpoll == that.sqpoll
               && directIo == that.directIo
               && fallbackOnUnavailable == that.fallbackOnUnavailable
               && blockSize == that.blockSize;
    }

    @Override
    public int hashCode()
    {
        int result = queueDepth;
        result = 31 * result + pollerThreads;
        result = 31 * result + (sqpoll ? 1 : 0);
        result = 31 * result + (directIo ? 1 : 0);
        result = 31 * result + (fallbackOnUnavailable ? 1 : 0);
        result = 31 * result + blockSize;
        return result;
    }

    @Override
    public String toString()
    {
        return "IoUringConfig{queueDepth=" + queueDepth
               + ", pollerThreads=" + pollerThreads
               + ", sqpoll=" + sqpoll
               + ", directIo=" + directIo
               + ", fallbackOnUnavailable=" + fallbackOnUnavailable
               + ", blockSize=" + blockSize + '}';
    }

    /**
     * Fluent builder for {@link IoUringConfig}. Each setter returns {@code this}; {@link #build()} validates the
     * accumulated values and produces an immutable configuration.
     */
    public static final class Builder
    {
        private int queueDepth = DEFAULT_QUEUE_DEPTH;
        private int pollerThreads = DEFAULT_POLLER_THREADS;
        private boolean sqpoll = false;
        private boolean directIo = false;
        private boolean fallbackOnUnavailable = true;
        private int blockSize = DEFAULT_BLOCK_SIZE;

        private Builder()
        {
        }

        /** Set the requested SQ/CQ entry count; must be positive. */
        public Builder queueDepth(int queueDepth)
        {
            this.queueDepth = queueDepth;
            return this;
        }

        /** Set the number of completion poller threads; must be positive. */
        public Builder pollerThreads(int pollerThreads)
        {
            this.pollerThreads = pollerThreads;
            return this;
        }

        /** Enable or disable kernel-side submission-queue polling. */
        public Builder sqpoll(boolean sqpoll)
        {
            this.sqpoll = sqpoll;
            return this;
        }

        /** Enable or disable {@code O_DIRECT} I/O. */
        public Builder directIo(boolean directIo)
        {
            this.directIo = directIo;
            return this;
        }

        /** Enable or disable the blocking fallback when the ring is unavailable. */
        public Builder fallbackOnUnavailable(boolean fallbackOnUnavailable)
        {
            this.fallbackOnUnavailable = fallbackOnUnavailable;
            return this;
        }

        /** Set the {@code O_DIRECT} alignment block size in bytes; must be positive. */
        public Builder blockSize(int blockSize)
        {
            this.blockSize = blockSize;
            return this;
        }

        /** Validate and build the immutable configuration. */
        public IoUringConfig build()
        {
            if (queueDepth <= 0)
                throw new IllegalArgumentException("queueDepth must be positive, got " + queueDepth);
            if (pollerThreads <= 0)
                throw new IllegalArgumentException("pollerThreads must be positive, got " + pollerThreads);
            if (blockSize <= 0)
                throw new IllegalArgumentException("blockSize must be positive, got " + blockSize);
            return new IoUringConfig(this);
        }
    }
}
