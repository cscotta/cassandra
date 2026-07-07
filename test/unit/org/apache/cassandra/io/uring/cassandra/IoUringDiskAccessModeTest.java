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
import java.nio.file.Files;

import org.junit.Test;

import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.uring.probe.IoUringAvailability;
import org.apache.cassandra.io.util.AsyncReadProvider;
import org.apache.cassandra.io.util.AsyncReadProviders;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.ChunkReader;
import org.apache.cassandra.io.util.File;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.memory.MemoryUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * End-to-end test of the disk_access_mode=io_uring read path: the {@link AsyncReadProvider} discovered via
 * ServiceLoader must be the io_uring implementation, and the {@link ChunkReader} it builds over an
 * {@link ChannelProxy.IOMode#IO_URING} channel must return byte-for-byte the same data as the file's known content,
 * across chunk-aligned offsets and a cross-EOF chunk. Self-skips when io_uring is unavailable.
 */
public class IoUringDiskAccessModeTest
{
    private static final int FILE_SIZE = 300_000;
    private static final int CHUNK = 4096;

    private static void assumeAvailable()
    {
        assumeTrue("Linux only", FBUtilities.isLinux);
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        assumeTrue("io_uring unavailable", IoUringAvailability.check().isAvailable());
    }

    private static byte[] pattern(int n)
    {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) ((i * 31 + 7) & 0xFF);
        return b;
    }

    @Test
    public void serviceLoaderResolvesIoUringProvider()
    {
        assumeAvailable();
        AsyncReadProvider provider = AsyncReadProviders.get();
        assertNotNull(provider);
        assertTrue("io_uring provider should report available on this host", provider.isAvailable());
        assertEquals(IoUringReadProvider.class, provider.getClass());
    }

    @Test
    public void ioUringChunkReaderMatchesFileContent() throws Exception
    {
        assumeAvailable();
        byte[] expected = pattern(FILE_SIZE);
        java.nio.file.Path tmp = Files.createTempFile("iouring-dam", ".db");
        try
        {
            Files.write(tmp, expected);
            AsyncReadProvider provider = AsyncReadProviders.get();
            assumeTrue(provider.isAvailable());

            ChannelProxy channel = new ChannelProxy(new File(tmp.toString()), ChannelProxy.IOMode.IO_URING);
            try
            {
                ChunkReader reader = provider.newChunkReader(channel, channel.size(), BufferType.OFF_HEAP, CHUNK);
                assertEquals(CHUNK, reader.chunkSize());
                assertEquals(BufferType.OFF_HEAP, reader.preferredBufferType());

                long[] positions = { 0, CHUNK, 2L * CHUNK, 16L * CHUNK, (long) (FILE_SIZE / CHUNK) * CHUNK };
                for (long pos : positions)
                {
                    ByteBuffer buf = BufferType.OFF_HEAP.allocate(CHUNK);
                    reader.readChunk(pos, buf);
                    int expLen = (int) Math.max(0, Math.min(CHUNK, FILE_SIZE - pos));
                    assertEquals("limit at pos=" + pos, expLen, buf.limit());
                    assertEquals("position reset at pos=" + pos, 0, buf.position());
                    for (int i = 0; i < expLen; i++)
                        assertEquals("byte at pos=" + pos + " i=" + i, expected[(int) pos + i], buf.get(i));
                }
            }
            finally
            {
                channel.close();
                provider.shutdown();
            }
        }
        finally
        {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void oDirectReadMatchesFileContent() throws Exception
    {
        assumeAvailable();
        // O_DIRECT cannot use tmpfs; place the file on the repo build dir (a real device fs here).
        java.nio.file.Path dir = java.nio.file.Path.of("build", "test");
        Files.createDirectories(dir);
        java.nio.file.Path tmp = Files.createTempFile(dir, "iouring-odirect", ".db");
        byte[] expected = pattern(FILE_SIZE);
        try
        {
            Files.write(tmp, expected);
            AsyncReadProvider provider = AsyncReadProviders.get();
            assumeTrue(provider.isAvailable());

            ChannelProxy channel;
            try
            {
                channel = new ChannelProxy(new File(tmp.toString()), ChannelProxy.IOMode.IO_URING_DIRECT);
            }
            catch (RuntimeException openFailed)
            {
                assumeTrue("O_DIRECT unsupported on this filesystem: " + openFailed, false);
                return;
            }
            try
            {
                ChunkReader reader = provider.newChunkReader(channel, channel.size(), BufferType.OFF_HEAP, CHUNK);
                long[] positions = { 0, CHUNK, 16L * CHUNK, (long) (FILE_SIZE / CHUNK) * CHUNK };
                for (long pos : positions)
                {
                    ByteBuffer buf = alignedDirect(CHUNK, 4096);   // O_DIRECT needs a block-aligned buffer
                    reader.readChunk(pos, buf);
                    int expLen = (int) Math.max(0, Math.min(CHUNK, FILE_SIZE - pos));
                    assertEquals("limit at pos=" + pos, expLen, buf.limit());
                    for (int i = 0; i < expLen; i++)
                        assertEquals("byte at pos=" + pos + " i=" + i, expected[(int) pos + i], buf.get(i));
                }
            }
            finally
            {
                channel.close();
                provider.shutdown();
            }
        }
        finally
        {
            Files.deleteIfExists(tmp);
        }
    }

    private static ByteBuffer alignedDirect(int size, int align)
    {
        ByteBuffer raw = ByteBuffer.allocateDirect(size + align);
        long base = MemoryUtil.getAddress(raw);
        int off = (int) ((align - (base & (align - 1))) & (align - 1));
        raw.position(off).limit(off + size);
        return raw.slice();
    }
}
