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

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.compress.BufferType;

/**
 * Loads the {@link AsyncReadProvider} implementation via {@link ServiceLoader}, caching the result. The io_uring
 * implementation is registered in {@code META-INF/services} but is compiled only on JDK 25; on JDK 11/17/21 (or any
 * build without it) the service class is absent, so loading throws and we fall back to a permanently-unavailable
 * provider. This is the one place the core read path bridges to the optional io_uring module, and it never references
 * an io_uring or {@code java.lang.foreign} type, so it compiles on every supported JDK.
 */
public final class AsyncReadProviders
{
    private static final Logger logger = LoggerFactory.getLogger(AsyncReadProviders.class);

    /** Fallback used whenever no io_uring provider is present/loadable (non-Linux, JDK &lt; 25, or excluded build). */
    private static final AsyncReadProvider UNAVAILABLE = new AsyncReadProvider()
    {
        @Override
        public boolean isAvailable()
        {
            return false;
        }

        @Override
        public String unavailableReason()
        {
            return "no io_uring provider on the classpath (requires Linux and JDK 25)";
        }

        @Override
        public ChunkReader newChunkReader(ChannelProxy channel, long fileLength, BufferType bufferType, int chunkSize)
        {
            throw new UnsupportedOperationException("io_uring is not available");
        }

        @Override
        public AsyncFrameReader newFrameReader(ChannelProxy channel, boolean directIo)
        {
            throw new UnsupportedOperationException("io_uring is not available");
        }

        @Override
        public void shutdown()
        {
        }
    };

    private static volatile AsyncReadProvider instance;

    private AsyncReadProviders()
    {
    }

    /** The (cached) provider; {@link AsyncReadProvider#isAvailable()} tells callers whether io_uring can be used. */
    public static AsyncReadProvider get()
    {
        AsyncReadProvider local = instance;
        if (local == null)
        {
            synchronized (AsyncReadProviders.class)
            {
                local = instance;
                if (local == null)
                    local = instance = load();
            }
        }
        return local;
    }

    private static AsyncReadProvider load()
    {
        try
        {
            for (AsyncReadProvider provider : ServiceLoader.load(AsyncReadProvider.class))
                return provider;   // the io_uring module registers exactly one
        }
        catch (Throwable t)
        {
            // ServiceConfigurationError/NoClassDefFoundError when the JDK-25-only impl class is absent, or an init
            // failure: treat as "io_uring unavailable" rather than failing node startup.
            logger.debug("No io_uring AsyncReadProvider available: {}", t.toString());
        }
        return UNAVAILABLE;
    }
}
