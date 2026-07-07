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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import accord.utils.Gen;
import accord.utils.Gens;
import accord.utils.RandomSource;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.io.compress.CompressedSequentialWriter;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.utils.memory.MemoryUtil;

import static accord.utils.Property.qt;
import static org.apache.cassandra.config.CassandraRelevantProperties.JAVA_IO_TMPDIR;
import static org.apache.cassandra.schema.CompressionParams.DEFAULT_CHUNK_LENGTH;
import static org.junit.Assume.assumeTrue;

/**
 * Byte-parity + corruption coverage for {@link CompressedChunkReader.IoUring}, the io_uring-backed compressed reader.
 * It references only core types and self-skips (via {@link AsyncReadProvider#isAvailable()}) unless a real io_uring
 * provider is present (Linux + JDK 25 + usable kernel), so it compiles on every JDK and runs only where io_uring works.
 * The reader reuses the {@link CompressedChunkReader.Direct} alignment/CRC/decompress path, so this mirrors
 * {@code DirectCompressedChunkReaderTest} to prove the io_uring frame read is a drop-in replacement.
 */
public class IoUringCompressedChunkReaderTest extends CompressedChunkReaderTestBase
{
    private static int seed;

    @BeforeClass
    public static void setup()
    {
        DatabaseDescriptor.clientInitialization();
        seed = new Random().nextInt();
    }

    private static Gen<Integer> mixedChunkLengths()
    {
        return Gens.pick(java.util.Arrays.asList(4096, 8192, 16384, 32768, 65536));
    }

    @Test
    public void compressedReads()
    {
        assumeTrue("io_uring unavailable", AsyncReadProviders.get().isAvailable());
        testReads(compressionParams(mixedChunkLengths()), Gens.longs().between(1, 1 << 16));
    }

    @Test
    public void storedUncompressedReads()
    {
        assumeTrue("io_uring unavailable", AsyncReadProviders.get().isAvailable());
        // maxCompressedLength = 0 forces the stored (incompressible) branch: chunk.length >= maxCompressedLength.
        testReads(Gens.constant(CompressionParams.lz4(DEFAULT_CHUNK_LENGTH, 0)), Gens.longs().between(1, 1 << 16));
    }

    @Test
    public void edgeCases()
    {
        assumeTrue("io_uring unavailable", AsyncReadProviders.get().isAvailable());
        Gen<CompressionParams> params = compressionParams(Gens.constant(4096));
        testReads(params, Gens.longs().of(1024));          // file smaller than one chunk
        testReads(params, Gens.longs().of(4096 + 96));     // partial trailing chunk
    }

    @Test
    public void corruptBlockThrows()
    {
        assumeTrue("io_uring unavailable", AsyncReadProviders.get().isAvailable());
        qt().withSeed(seed).forAll(Gens.random(), compressionParams(Gens.constant(4096))).check((rs, params) -> {
            long totalBytes = params.chunkLength() * 2L;
            List<ByteBuffer> chunks = generateRandomChunks(rs, params, totalBytes);
            File dataFile = new File(JAVA_IO_TMPDIR.getString() + "iouring_compressed_corrupt.bin");
            CompressionMetadata metadata;
            try (CompressedSequentialWriter writer = writer(params, dataFile))
            {
                for (ByteBuffer chunk : chunks)
                    writer.write(chunk.duplicate());
                writer.sync();
                metadata = writer.open(0);
            }

            CompressionMetadata.Chunk first = metadata.chunkFor(0);
            try (FileChannel fc = FileChannel.open(dataFile.toPath(), StandardOpenOption.WRITE))
            {
                fc.truncate(first.offset + (long) first.length / 2);   // truncate halfway into the first chunk
            }

            boolean thrown = false;
            try
            {
                readAndVerify(dataFile, metadata, chunks, totalBytes);
            }
            catch (CorruptSSTableException e)
            {
                thrown = true;
            }
            finally
            {
                metadata.close();
                Files.deleteIfExists(dataFile.toPath());
            }
            Assert.assertTrue("expected CorruptSSTableException for a truncated chunk", thrown);
        });
    }

    private static void testReads(Gen<CompressionParams> paramsGen, Gen.LongGen totalBytesGen)
    {
        qt().withSeed(seed).forAll(Gens.random(), paramsGen).check((rs, params) -> {
            long totalBytes = totalBytesGen.nextLong(rs);
            List<ByteBuffer> chunks = generateRandomChunks(rs, params, totalBytes);
            File file = new File(JAVA_IO_TMPDIR.getString() + "iouring_compressed.bin");
            try (CompressedSequentialWriter writer = writer(params, file))
            {
                for (ByteBuffer chunk : chunks)
                    writer.write(chunk.duplicate());
                writer.sync();
                CompressionMetadata metadata = writer.open(0);
                readAndVerify(file, metadata, chunks, totalBytes);
            }
            finally
            {
                Files.deleteIfExists(file.toPath());
            }
        });
    }

    private static void readAndVerify(File file, CompressionMetadata metadata, List<ByteBuffer> expected, long totalBytes)
    {
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(metadata.chunkLength());
        try (ChannelProxy channel = new ChannelProxy(file, ChannelProxy.IOMode.IO_URING);
             CompressedChunkReader reader = new CompressedChunkReader.IoUring(channel, metadata, () -> 1d, AsyncReadProviders.get());
             CompressionMetadata metadataToClose = metadata)
        {
            long fileOffset = 0;
            long bytesRead = 0;
            int chunkIndex = 0;
            while (bytesRead < totalBytes)
            {
                ByteBuffer expectedChunk = expected.get(chunkIndex);
                readBuffer.clear();
                reader.readChunk(fileOffset, readBuffer);

                int expectedBytes = expectedChunk.remaining();
                Assert.assertTrue("short read at offset " + fileOffset, readBuffer.remaining() >= expectedBytes);
                int savedLimit = readBuffer.limit();
                readBuffer.limit(expectedBytes);
                Assert.assertEquals("mismatched data at offset " + fileOffset, expectedChunk, readBuffer);
                readBuffer.limit(savedLimit);

                bytesRead += expectedBytes;
                fileOffset += metadata.chunkLength();
                chunkIndex++;
            }
        }
        finally
        {
            MemoryUtil.clean(readBuffer);
        }
    }

    private static List<ByteBuffer> generateRandomChunks(RandomSource rs, CompressionParams params, long bytesToWrite)
    {
        List<ByteBuffer> chunks = new ArrayList<>();
        long generated = 0;
        while (generated < bytesToWrite)
        {
            ByteBuffer buf = ByteBuffer.allocate(params.chunkLength());
            int fill = (int) Math.min(buf.capacity(), bytesToWrite - generated);
            byte[] bytes = new byte[fill];
            rs.nextBytes(bytes);
            buf.put(bytes);
            buf.flip();
            chunks.add(buf);
            generated += fill;
        }
        return chunks;
    }

    private static CompressedSequentialWriter writer(CompressionParams params, File dataFile)
    {
        return new CompressedSequentialWriter(dataFile, new File("file.offset"), new File("file.digest"),
                                              writerOption(1 << 10), params, new MetadataCollector(new ClusteringComparator()));
    }
}
