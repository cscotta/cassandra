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
package org.apache.cassandra.db.streaming;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

import com.google.common.collect.Iterators;
import com.google.common.primitives.Ints;

import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.utils.ChecksumType;
import org.apache.cassandra.utils.memory.MemoryUtil;

import static java.lang.Math.max;
import static java.lang.String.format;

/**
 * InputStream which reads compressed chunks from the underlying input stream and deals with decompression
 * and position tracking.
 *
 * The underlying input will be an instance of {@link RebufferingInputStream} except in some unit tests.
 *
 * Compressed chunks transferred will be a subset of all chunks in the source streamed sstable - just enough to
 * deserialize the requested partition position ranges. Correctness of the entire operation depends on provided
 * partition position ranges and compressed chunks properly matching, and there is no way on the receiving side to
 * verify if that's the case, which arguably makes this a little brittle.
 */
public class CompressedInputStream extends RebufferingInputStream implements AutoCloseable
{
    private static final double GROWTH_FACTOR = 1.5;

    private final DataInputPlus input;

    private final Iterator<CompressionMetadata.Chunk> compressedChunks;
    private final CompressionParams compressionParams;

    private final ChecksumType checksumType;
    private final DoubleSupplier validateChecksumChance;

    // Fixed-output (block-aligned) streaming: uncompressed chunk sizes vary, so the receiver cannot
    // derive a chunk's uncompressed base by masking; the per-chunk uncompressed offsets are streamed
    // alongside the chunks and consumed in order. index into them for the currently-loaded chunk.
    private final boolean fixedOutput;
    private final long[] uncompressedChunkOffsets;
    private int chunkIndex = -1;

    /**
     * The base offset of the current {@link #buffer} into the original sstable as if it were uncompressed.
     */
    private long uncompressedChunkPosition = Long.MIN_VALUE;

    /**
     * @param input Input input to read compressed data from
     * @param compressionInfo Compression info
     */
    public CompressedInputStream(DataInputPlus input,
                                 CompressionInfo compressionInfo,
                                 ChecksumType checksumType,
                                 DoubleSupplier validateChecksumChance)
    {
        super(ByteBuffer.allocateDirect(compressionInfo.parameters().usesFixedOutputChunks()
                                        ? compressionInfo.parameters().maxUncompressedChunkLength()
                                        : compressionInfo.parameters().chunkLength()));
        buffer.limit(0);

        this.input = input;
        this.checksumType = checksumType;
        this.validateChecksumChance = validateChecksumChance;

        compressionParams = compressionInfo.parameters();
        compressedChunks = Iterators.forArray(compressionInfo.chunks());
        this.fixedOutput = compressionParams.usesFixedOutputChunks();
        this.uncompressedChunkOffsets = compressionInfo.uncompressedChunkOffsets();
        // fixed-output: read the whole S-byte block; legacy: at most a chunkLength-sized compressed chunk
        compressedChunk = ByteBuffer.allocateDirect(fixedOutput ? compressionParams.compressedChunkLength()
                                                                : compressionParams.chunkLength());
    }

    /**
     * Invoked when crossing into the next {@link SSTableReader.PartitionPositionBounds} section
     * in {@link CassandraCompressedStreamReader#read(DataInputPlus)}.
     * Will skip 1..n compressed chunks of the original sstable.
     */
    public void position(long position) throws IOException
    {
        if (position < uncompressedChunkPosition + buffer.position())
            throw new IllegalStateException("stream can only move forward");

        if (position >= uncompressedChunkPosition + buffer.limit())
        {
            loadNextChunk();
            if (fixedOutput)
                // variable uncompressed chunk sizes: the loaded chunk's base is the streamed offset
                uncompressedChunkPosition = uncompressedChunkOffsets[chunkIndex];
            else
                // uncompressedChunkPosition = position - (position % compressionParams.chunkLength())
                uncompressedChunkPosition = position & -compressionParams.chunkLength();
        }

        buffer.position(Ints.checkedCast(position - uncompressedChunkPosition));
    }

    @Override
    protected void reBuffer() throws IOException
    {
        if (uncompressedChunkPosition < 0)
            throw new IllegalStateException("position(long position) wasn't called first");

        /*
         * reBuffer() will only be called if a partition range spanning multiple (adjacent) compressed chunks
         * has consumed the current uncompressed buffer, and needs to move to the next adjacent chunk.
         */
        loadNextChunk();
        if (fixedOutput)
            // adjacent chunks are contiguous in uncompressed space; the streamed offset is authoritative
            uncompressedChunkPosition = uncompressedChunkOffsets[chunkIndex];
        else
            // uncompressedChunkPosition in the legacy case *always* increases by the fixed chunk length
            uncompressedChunkPosition += compressionParams.chunkLength();
    }

    /**
     * Reads the next chunk, decompresses if necessary, and probabilistically verifies the checksum/CRC.
     *
     * Doesn't adjust uncompressedChunkPosition - it's up to the caller to do so.
     */
    private void loadNextChunk() throws IOException
    {
        if (!compressedChunks.hasNext())
            throw new EOFException();

        int chunkLength = compressedChunks.next().length;
        chunkIndex++;

        if (fixedOutput)
        {
            loadNextFixedOutputChunk(chunkLength);
            return;
        }

        chunkBytesRead += (chunkLength + 4); // chunk length + checksum or CRC length

        /*
         * uncompress if the buffer size is less than the max chunk size; else, if the buffer size is greater than
         * or equal to the maxCompressedLength, we assume the buffer is not compressed (see CASSANDRA-10520)
         */
        if (chunkLength < compressionParams.maxCompressedLength())
        {
            if (compressedChunk.capacity() < chunkLength)
            {
                // with poorly compressible data, it's possible for a compressed chunk to be larger than
                // configured uncompressed chunk size - depending on data, min_compress_ratio, and compressor;
                // we may need to resize the compressed buffer.
                MemoryUtil.clean(compressedChunk);
                compressedChunk = ByteBuffer.allocateDirect(max((int) (compressedChunk.capacity() * GROWTH_FACTOR), chunkLength));
            }

            compressedChunk.position(0).limit(chunkLength);
            readChunk(compressedChunk);
            compressedChunk.position(0);

            maybeValidateChecksum(compressedChunk, input.readInt());

            buffer.clear();
            compressionParams.getSstableCompressor().uncompress(compressedChunk, buffer);
            buffer.flip();
        }
        else
        {
            buffer.position(0).limit(chunkLength);
            readChunk(buffer);
            buffer.position(0);

            maybeValidateChecksum(buffer, input.readInt());
        }
    }

    /**
     * Fixed-output chunk: read the whole {@code S}-byte block (chunk.length == S; no trailing CRC in
     * the stream), parse [int payloadLen][payload][int CRC32][pad], verify the embedded CRC over the
     * payload, and decompress the payload into {@link #buffer}.
     */
    private void loadNextFixedOutputChunk(int blockSize) throws IOException
    {
        chunkBytesRead += blockSize; // CRC + padding are inside the fixed block

        if (compressedChunk.capacity() < blockSize)
        {
            MemoryUtil.clean(compressedChunk);
            compressedChunk = ByteBuffer.allocateDirect(blockSize);
        }
        compressedChunk.position(0).limit(blockSize);
        readChunk(compressedChunk);

        int payloadLen = compressedChunk.getInt(0);
        int header = CompressionMetadata.FIXED_BLOCK_HEADER_BYTES;
        if (payloadLen < 0 || header + payloadLen + CompressionMetadata.FIXED_BLOCK_CRC_BYTES > blockSize)
            throw new IOException(format("Corrupt fixed-output block: payloadLen=%d, blockSize=%d", payloadLen, blockSize));

        ByteBuffer payload = compressedChunk.duplicate();
        payload.position(header).limit(header + payloadLen);
        payload = payload.slice();

        maybeValidateChecksum(payload, compressedChunk.getInt(header + payloadLen));
        payload.position(0).limit(payloadLen);

        buffer.clear();
        compressionParams.getSstableCompressor().uncompress(payload, buffer);
        buffer.flip();
    }
    private ByteBuffer compressedChunk;

    private void readChunk(ByteBuffer dst) throws IOException
    {
        if (input instanceof RebufferingInputStream)
            ((RebufferingInputStream) input).readFully(dst);
        else
            readChunkSlow(dst);
    }

    // slow path that involves an intermediate copy into a byte array; only used by some of the unit tests
    private void readChunkSlow(ByteBuffer dst) throws IOException
    {
        if (copyArray == null)
            copyArray = new byte[dst.remaining()];
        else if (copyArray.length < dst.remaining())
            copyArray = new byte[max((int)(copyArray.length * GROWTH_FACTOR), dst.remaining())];

        input.readFully(copyArray, 0, dst.remaining());
        dst.put(copyArray, 0, dst.remaining());
    }
    private byte[] copyArray;

    private void maybeValidateChecksum(ByteBuffer buffer, int expectedChecksum) throws IOException
    {
        double validateChance = validateChecksumChance.getAsDouble();

        if (validateChance >= 1.0d || (validateChance > 0.0d && validateChance > ThreadLocalRandom.current().nextDouble()))
        {
            int position = buffer.position();
            int actualChecksum = (int) checksumType.of(buffer);
            buffer.position(position); // checksum calculation consumes the buffer, so we must reset its position afterwards

            if (expectedChecksum != actualChecksum)
                throw new IOException(format("Checksum didn't match (expected: %d, actual: %d)", expectedChecksum, actualChecksum));
        }
    }

    @Override
    public void close()
    {
        if (null != buffer)
        {
            MemoryUtil.clean(buffer);
            buffer = null;
        }

        if (null != compressedChunk)
        {
            MemoryUtil.clean(compressedChunk);
            compressedChunk = null;
        }
    }

    /**
     * @return accumulated size of all chunks read so far - including checksums
     */
    long chunkBytesRead()
    {
        return chunkBytesRead;
    }
    private long chunkBytesRead = 0;
}
