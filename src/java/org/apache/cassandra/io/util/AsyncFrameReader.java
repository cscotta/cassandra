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

import java.nio.ByteBuffer;

import org.apache.cassandra.utils.Closeable;

/**
 * A positional frame read served by an {@link AsyncReadProvider} (io_uring today). It turns a file descriptor plus a
 * file offset into a device read into the caller's direct buffer, so the core compressed-read path can route its one
 * compressed-frame read through io_uring without referencing any io_uring / {@code java.lang.foreign} type. Obtained
 * from {@link AsyncReadProvider#newFrameReader} only when the provider {@link AsyncReadProvider#isAvailable() is
 * available}.
 *
 * <p>Contract (mirroring {@link ChannelProxy#read}): {@link #read} fills {@code dst.remaining()} bytes starting at file
 * offset {@code position} into the <em>direct</em> buffer {@code dst} and returns the number of bytes read; a short
 * read is surfaced to the caller (which treats it as corruption), and a device error throws an unchecked
 * {@code FSReadError}. The buffer must be direct (the kernel DMAs into it).
 */
public interface AsyncFrameReader extends Closeable
{
    /** Reads {@code dst.remaining()} bytes at file offset {@code position} into the direct buffer {@code dst}. */
    int read(ByteBuffer dst, long position);

    @Override
    default void close()
    {
    }
}
