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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.compress.CorruptBlockException;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.metrics.ReadIOTracker;
import org.apache.cassandra.utils.ChecksumType;
import org.apache.cassandra.utils.Closeable;

public abstract class CompressedChunkReader extends AbstractReaderFileProxy implements ChunkReader
{
    final CompressionMetadata metadata;
    final int maxCompressedLength;
    final DoubleSupplier crcCheckChanceSupplier;

    // Fixed-output (block-aligned) mode: each chunk is a self-describing block of fixedChunkSize bytes,
    // laid out as [int payloadLen][compressed payload][int CRC32][zero pad]. chunkFor returns
    // Chunk(chunkIndex * fixedChunkSize, fixedChunkSize); the reader parses the block internally.
    final boolean fixedOutput;
    final int fixedChunkSize;
    final int fixedPayloadCapacity;

    protected CompressedChunkReader(ChannelProxy channel, CompressionMetadata metadata, DoubleSupplier crcCheckChanceSupplier)
    {
        super(channel, metadata.dataLength);
        this.metadata = metadata;
        this.maxCompressedLength = metadata.maxCompressedLength();
        this.crcCheckChanceSupplier = crcCheckChanceSupplier;
        this.fixedOutput = metadata.parameters.usesFixedOutputChunks();
        this.fixedChunkSize = metadata.parameters.compressedChunkLength();
        this.fixedPayloadCapacity = fixedOutput
                                    ? fixedChunkSize - CompressionMetadata.FIXED_BLOCK_HEADER_BYTES - CompressionMetadata.FIXED_BLOCK_CRC_BYTES
                                    : 0;
        assert fixedOutput || Integer.bitCount(metadata.chunkLength()) == 1; //legacy chunk length must be a power of two
    }

    /**
     * Decompress a fixed-output block into {@code uncompressed}. {@code block} must hold the full
     * S-byte block starting at position 0. Parses the length prefix, optionally verifies the embedded
     * per-chunk CRC over the compressed payload, then decompresses the payload.
     */
    protected void uncompressFixedBlock(ByteBuffer block, ByteBuffer uncompressed, CompressionMetadata.Chunk chunk, boolean shouldCheckCrc) throws IOException
    {
        int payloadLen = block.getInt(0);
        if (payloadLen < 0 || payloadLen > fixedPayloadCapacity)
            throw new CorruptBlockException(channel.filePath(), chunk);

        ByteBuffer payload = block.duplicate();
        payload.position(CompressionMetadata.FIXED_BLOCK_HEADER_BYTES)
               .limit(CompressionMetadata.FIXED_BLOCK_HEADER_BYTES + payloadLen);
        payload = payload.slice(); // 0..payloadLen

        if (shouldCheckCrc)
        {
            int checksum = (int) ChecksumType.CRC32.of(payload);
            int stored = block.getInt(CompressionMetadata.FIXED_BLOCK_HEADER_BYTES + payloadLen);
            if (stored != checksum)
                throw new CorruptBlockException(channel.filePath(), chunk);
            payload.position(0).limit(payloadLen);
        }

        uncompressed.clear();
        // Size the destination to the chunk's EXACT uncompressed length: streaming-written frames carry
        // no content-size header, and Zstd's one-shot decompress then requires an exactly-sized output
        // (an oversized buffer yields 0 bytes). This is also correct for frames that do carry a size.
        uncompressed.limit(metadata.fixedOutputUncompressedLength(chunk));
        metadata.compressor().uncompress(payload, uncompressed);
        uncompressed.flip();
    }

    protected CompressedChunkReader forScan()
    {
        return this;
    }

    @VisibleForTesting
    public double getCrcCheckChance()
    {
        return crcCheckChanceSupplier.getAsDouble();
    }

    boolean shouldCheckCrc()
    {
        double checkChance = getCrcCheckChance();
        return checkChance >= 1d || (checkChance > 0d && checkChance > ThreadLocalRandom.current().nextDouble());
    }

    @Override
    public String toString()
    {
        return String.format("CompressedChunkReader.%s(%s - %s, chunk length %d, data length %d)",
                             getClass().getSimpleName(),
                             channel.filePath(),
                             metadata.compressor().getClass().getSimpleName(),
                             metadata.chunkLength(),
                             metadata.dataLength);
    }

    @Override
    public int chunkSize()
    {
        return metadata.bufferSize();
    }

    @Override
    public long chunkBase(long position)
    {
        return fixedOutput ? metadata.uncompressedChunkBase(position) : (position & -(long) chunkSize());
    }

    @Override
    public BufferType preferredBufferType()
    {
        return metadata.compressor().preferredBufferType();
    }

    @Override
    public Rebufferer instantiateRebufferer(boolean isScan)
    {
        if (fixedOutput)
            return new BufferManagingRebufferer.Variable(isScan ? forScan() : this);
        return new BufferManagingRebufferer.Aligned(isScan ? forScan() : this);
    }

    protected interface CompressedReader extends Closeable
    {
        default void allocateResources()
        {
        }

        default void deallocateResources()
        {
        }

        default boolean allocated()
        {
            return false;
        }

        default void close()
        {

        }

        /**
         * The returned buffer is only valid until the next call to read(). Callers must consume the data immediately.
         */
        ByteBuffer read(CompressionMetadata.Chunk chunk, boolean shouldCheckCrc) throws CorruptBlockException;
    }

    private static final class DirectRandomAccessReader implements CompressedReader
    {

        private final ChannelProxy channel;
        private final int blockSize;
        private final DirectThreadLocalByteBufferHolder bufferHolder;
        private final AsyncFrameReader frameReader;   // null => read via the FileChannel; non-null => via io_uring

        DirectRandomAccessReader(ChannelProxy ch, int blockSize, AsyncFrameReader frameReader)
        {
            this.channel = ch;
            this.blockSize = blockSize;
            this.bufferHolder = new DirectThreadLocalByteBufferHolder(blockSize);
            this.frameReader = frameReader;
        }

        @Override
        public ByteBuffer read(CompressionMetadata.Chunk chunk, boolean shouldCheckCrc) throws CorruptBlockException
        {
            int length = shouldCheckCrc ? chunk.length + Integer.BYTES // length + checksum length
                                        : chunk.length;

            long alignedPos = chunk.offset & -blockSize;
            int delta = (int) (chunk.offset - alignedPos);

            ByteBuffer buffer = bufferHolder.getBuffer(length + delta);
            // Only the device read differs between the FileChannel and io_uring paths; the block-aligned offset/length,
            // over-read-by-delta, slice, and CRC verification below are shared verbatim so they cannot drift.
            int read = frameReader != null ? frameReader.read(buffer, alignedPos) : channel.read(buffer, alignedPos);
            if (read < length + delta)
                throw new CorruptBlockException(channel.filePath(), chunk);

            buffer.position(delta);
            buffer.limit(delta + length);

            ByteBuffer slice = buffer.slice();
            slice.limit(chunk.length); // limit at chunk content end (before CRC)

            if (shouldCheckCrc)
            {
                int checksum = (int) ChecksumType.CRC32.of(slice);
                slice.limit(length);
                if (slice.getInt() != checksum)
                    throw new CorruptBlockException(channel.filePath(), chunk);

                slice.position(0).limit(chunk.length);
            }
            return slice;
        }

        @Override
        public void close()
        {
            if (frameReader != null)
                frameReader.close();
        }
    }

    private static class RandomAccessCompressedReader implements CompressedReader
    {
        private final ChannelProxy channel;
        private final ThreadLocalByteBufferHolder bufferHolder;

        private RandomAccessCompressedReader(ChannelProxy channel, CompressionMetadata metadata)
        {
            this.channel = channel;
            this.bufferHolder = new ThreadLocalByteBufferHolder(metadata.compressor().preferredBufferType());
        }

        @Override
        public ByteBuffer read(CompressionMetadata.Chunk chunk, boolean shouldCheckCrc) throws CorruptBlockException
        {
            int length = shouldCheckCrc ? chunk.length + Integer.BYTES // compressed length + checksum length
                                        : chunk.length;
            ByteBuffer compressed = bufferHolder.getBuffer(length);
            if (channel.read(compressed, chunk.offset) != length)
                throw new CorruptBlockException(channel.filePath(), chunk);
            compressed.flip();
            compressed.limit(chunk.length);

            if (shouldCheckCrc)
            {
                int checksum = (int) ChecksumType.CRC32.of(compressed);
                compressed.limit(length);
                if (compressed.getInt() != checksum)
                    throw new CorruptBlockException(channel.filePath(), chunk);
                compressed.position(0).limit(chunk.length);
            }
            return compressed;
        }
    }

    private static class ScanCompressedReader implements CompressedReader
    {

        private final ChannelProxy channel;
        private final ByteBufferHolder bufferHolder;
        private final ThreadLocalReadAheadBuffer readAheadBuffer;

        private ScanCompressedReader(ChannelProxy channel, ByteBufferHolder bufferHolder,
                                     ThreadLocalReadAheadBuffer readAheadBuffer)
        {
            this.channel = channel;
            this.bufferHolder = bufferHolder;
            this.readAheadBuffer = readAheadBuffer;
        }

        @Override
        public ByteBuffer read(CompressionMetadata.Chunk chunk, boolean shouldCheckCrc) throws CorruptBlockException
        {
            int length = shouldCheckCrc ? chunk.length + Integer.BYTES // compressed length + checksum length
                                        : chunk.length;
            ByteBuffer compressed = bufferHolder.getBuffer(length);

            int copied = 0;
            while (copied < length)
            {
                readAheadBuffer.fill(chunk.offset + copied);
                int leftToRead = length - copied;
                if (readAheadBuffer.remaining() >= leftToRead)
                    copied += readAheadBuffer.read(compressed, leftToRead);
                else
                    copied += readAheadBuffer.read(compressed, readAheadBuffer.remaining());
            }

            compressed.flip();
            compressed.limit(chunk.length);

            if (shouldCheckCrc)
            {
                int checksum = (int) ChecksumType.CRC32.of(compressed);
                compressed.limit(length);
                if (compressed.getInt() != checksum)
                    throw new CorruptBlockException(channel.filePath(), chunk);
                compressed.position(0).limit(chunk.length);
            }
            return compressed;
        }

        @Override
        public void allocateResources()
        {
            readAheadBuffer.allocateBuffer();
        }

        @Override
        public void deallocateResources()
        {
            readAheadBuffer.clear(true);
        }

        @Override
        public boolean allocated()
        {
            return readAheadBuffer.hasBuffer();
        }

        @Override
        public void close()
        {
            readAheadBuffer.close();
        }
    }

    public static class Direct extends CompressedChunkReader
    {

        private final CompressedReader reader;
        private final CompressedReader scanReader;

        public Direct(ChannelProxy channel, CompressionMetadata metadata, DoubleSupplier crcCheckChanceSupplier)
        {
            this(channel, metadata, crcCheckChanceSupplier, FileUtils.getFileBlockSize(channel.file()), null, true);
        }

        protected Direct(ChannelProxy channel, CompressionMetadata metadata, DoubleSupplier crcCheckChanceSupplier,
                         int blockSize, AsyncFrameReader frameReader, boolean buildScanReader)
        {
            super(channel, metadata, crcCheckChanceSupplier);
            this.reader = new DirectRandomAccessReader(channel, blockSize, frameReader);

            int readAheadBufferSize = DatabaseDescriptor.getCompressedReadAheadBufferSize();
            this.scanReader = (buildScanReader && readAheadBufferSize > 0 && readAheadBufferSize > metadata.chunkLength())
                              ? new ScanCompressedReader(channel,
                                                         new DirectThreadLocalByteBufferHolder(blockSize),
                                                         new DirectThreadLocalReadAheadBuffer(channel, readAheadBufferSize, blockSize))
                              : null;
        }

        @Override
        public void readChunk(long position, ByteBuffer uncompressed)
        {
            assert fixedOutput || (position & -uncompressed.capacity()) == position;
            assert position <= fileLength;

            try
            {
                CompressionMetadata.Chunk chunk = metadata.chunkFor(position);
                boolean shouldCheckCrc = shouldCheckCrc();

                uncompressed.clear();
                CompressedReader readFrom = (scanReader != null && scanReader.allocated()) ? scanReader : reader;

                if (fixedOutput)
                {
                    // read the whole S-byte block (chunk.length == S; no separate trailing CRC) and parse it
                    ByteBuffer block = readFrom.read(chunk, false);
                    try
                    {
                        uncompressFixedBlock(block, uncompressed, chunk, shouldCheckCrc);
                    }
                    catch (IOException e)
                    {
                        throw new CorruptBlockException(channel.filePath(), chunk, e);
                    }
                }
                else if (chunk.length < maxCompressedLength)
                {
                    ByteBuffer compressed = readFrom.read(chunk, shouldCheckCrc);
                    try
                    {
                        metadata.compressor().uncompress(compressed, uncompressed);
                    }
                    catch (IOException e)
                    {
                        throw new CorruptBlockException(channel.filePath(), chunk, e);
                    }
                    uncompressed.flip();
                }
                else
                {
                    ByteBuffer buffer = readFrom.read(chunk, shouldCheckCrc);
                    uncompressed.put(buffer);
                    uncompressed.flip();
                }

                if (ReadIOTracker.isEnabled())
                    ReadIOTracker.recordDecompressed(uncompressed.remaining());
            }
            catch (CorruptBlockException e)
            {
                // Make sure reader does not see stale data.
                uncompressed.position(0).limit(0);
                throw new CorruptSSTableException(e, channel.filePath());
            }
        }

        @Override
        protected CompressedChunkReader forScan()
        {
            if (scanReader != null)
                scanReader.allocateResources();

            return this;
        }

        @Override
        public void releaseUnderlyingResources()
        {
            if (scanReader != null)
                scanReader.deallocateResources();
        }

        @Override
        public void close()
        {
            reader.close();
            if (scanReader != null)
                scanReader.close();

            super.close();
        }
    }

    /**
     * A {@link Direct}-shaped compressed reader whose single compressed-frame read is served by io_uring rather than the
     * FileChannel. It reuses Direct's {@code chunkFor} / block alignment / CRC verification / decompress logic verbatim
     * (only the device read at the {@link CompressedReader} seam differs); with {@code io_uring_direct_io} off it uses
     * {@code blockSize == 1} so the same aligned reader performs an exact-length buffered read. Read-ahead scans fall
     * back to per-chunk reads ({@code scanReader == null}). Built only when the provider {@link AsyncReadProvider#isAvailable}.
     */
    public static class IoUring extends Direct
    {
        public IoUring(ChannelProxy channel, CompressionMetadata metadata, DoubleSupplier crcCheckChanceSupplier,
                       AsyncReadProvider provider)
        {
            super(channel, metadata, crcCheckChanceSupplier,
                  DatabaseDescriptor.getIoUringDirectIo() ? FileUtils.getFileBlockSize(channel.file()) : 1,
                  provider.newFrameReader(channel, DatabaseDescriptor.getIoUringDirectIo()),
                  false);
        }
    }

    public static class Standard extends CompressedChunkReader
    {

        private final CompressedReader reader;
        private final CompressedReader scanReader;

        public Standard(ChannelProxy channel, CompressionMetadata metadata, DoubleSupplier crcCheckChanceSupplier)
        {
            super(channel, metadata, crcCheckChanceSupplier);
            reader = new RandomAccessCompressedReader(channel, metadata);

            int readAheadBufferSize = DatabaseDescriptor.getCompressedReadAheadBufferSize();
            scanReader = (readAheadBufferSize > 0 && readAheadBufferSize > metadata.chunkLength())
                         ? new ScanCompressedReader(channel,
                                                    new ThreadLocalByteBufferHolder(metadata.compressor().preferredBufferType()),
                                                    new ThreadLocalReadAheadBuffer(channel, readAheadBufferSize, metadata.compressor().preferredBufferType())) : null;
        }

        @Override
        protected CompressedChunkReader forScan()
        {
            if (scanReader != null)
                scanReader.allocateResources();

            return this;
        }

        @Override
        public void releaseUnderlyingResources()
        {
            if (scanReader != null)
                scanReader.deallocateResources();
        }

        @Override
        public void readChunk(long position, ByteBuffer uncompressed)
        {
            try
            {
                // accesses must always be aligned (legacy mode); fixed-output positions are chunk bases
                assert fixedOutput || (position & -uncompressed.capacity()) == position;
                assert position <= fileLength;

                CompressionMetadata.Chunk chunk = metadata.chunkFor(position);
                boolean shouldCheckCrc = shouldCheckCrc();

                CompressedReader readFrom = (scanReader != null && scanReader.allocated()) ? scanReader : reader;

                if (fixedOutput)
                {
                    ByteBuffer block = readFrom.read(chunk, false);
                    try
                    {
                        uncompressFixedBlock(block, uncompressed, chunk, shouldCheckCrc);
                    }
                    catch (IOException e)
                    {
                        throw new CorruptBlockException(channel.filePath(), chunk, e);
                    }
                }
                else if (chunk.length < maxCompressedLength)
                {
                    ByteBuffer compressed = readFrom.read(chunk, shouldCheckCrc);
                    uncompressed.clear();

                    try
                    {
                        metadata.compressor().uncompress(compressed, uncompressed);
                    }
                    catch (IOException e)
                    {
                        throw new CorruptBlockException(channel.filePath(), chunk, e);
                    }
                    uncompressed.flip();
                }
                else
                {
                    // Read directly into destination buffer for zero-copy uncompressed path
                    uncompressed.position(0).limit(chunk.length);
                    if (channel.read(uncompressed, chunk.offset) != chunk.length)
                        throw new CorruptBlockException(channel.filePath(), chunk);

                    if (shouldCheckCrc)
                    {
                        uncompressed.flip();
                        int checksum = (int) ChecksumType.CRC32.of(uncompressed);

                        ByteBuffer scratch = ByteBuffer.allocate(Integer.BYTES);
                        if (channel.read(scratch, chunk.offset + chunk.length) != Integer.BYTES
                            || scratch.getInt(0) != checksum)
                            throw new CorruptBlockException(channel.filePath(), chunk);
                    }
                    uncompressed.flip();
                }

                if (ReadIOTracker.isEnabled())
                    ReadIOTracker.recordDecompressed(uncompressed.remaining());
            }
            catch (CorruptBlockException e)
            {
                // Make sure reader does not see stale data.
                uncompressed.position(0).limit(0);
                throw new CorruptSSTableException(e, channel.filePath());
            }
        }

        @Override
        public void close()
        {
            reader.close();
            if (scanReader != null)
                scanReader.close();

            super.close();
        }
    }

    public static class Mmap extends CompressedChunkReader
    {
        protected final MmappedRegions regions;

        public Mmap(ChannelProxy channel, CompressionMetadata metadata, MmappedRegions regions, DoubleSupplier crcCheckChanceSupplier)
        {
            super(channel, metadata, crcCheckChanceSupplier);
            this.regions = regions;
        }

        @Override
        public void readChunk(long position, ByteBuffer uncompressed)
        {
            try
            {
                // accesses must always be aligned (legacy mode); fixed-output positions are chunk bases
                assert fixedOutput || (position & -uncompressed.capacity()) == position;
                assert position <= fileLength;

                CompressionMetadata.Chunk chunk = metadata.chunkFor(position);

                MmappedRegions.Region region = regions.floor(chunk.offset);
                long segmentOffset = region.offset();
                int chunkOffset = Ints.checkedCast(chunk.offset - segmentOffset);
                ByteBuffer compressedChunk = region.buffer();

                if (fixedOutput)
                {
                    ByteBuffer block = compressedChunk.duplicate();
                    block.position(chunkOffset).limit(chunkOffset + chunk.length);
                    block = block.slice(); // 0..S
                    try
                    {
                        uncompressFixedBlock(block, uncompressed, chunk, shouldCheckCrc());
                    }
                    catch (IOException e)
                    {
                        throw new CorruptBlockException(channel.filePath(), chunk, e);
                    }
                    if (ReadIOTracker.isEnabled())
                        ReadIOTracker.recordDecompressed(uncompressed.remaining());
                    return;
                }

                compressedChunk.position(chunkOffset).limit(chunkOffset + chunk.length);

                uncompressed.clear();

                try
                {
                    if (shouldCheckCrc())
                    {
                        int checksum = (int) ChecksumType.CRC32.of(compressedChunk);

                        compressedChunk.limit(compressedChunk.capacity());
                        if (compressedChunk.getInt() != checksum)
                            throw new CorruptBlockException(channel.filePath(), chunk);

                        compressedChunk.position(chunkOffset).limit(chunkOffset + chunk.length);
                    }

                    if (chunk.length < maxCompressedLength)
                        metadata.compressor().uncompress(compressedChunk, uncompressed);
                    else
                        uncompressed.put(compressedChunk);
                }
                catch (IOException e)
                {
                    throw new CorruptBlockException(channel.filePath(), chunk, e);
                }
                uncompressed.flip();
                if (ReadIOTracker.isEnabled())
                    ReadIOTracker.recordDecompressed(uncompressed.remaining());
            }
            catch (CorruptBlockException e)
            {
                // Make sure reader does not see stale data.
                uncompressed.position(0).limit(0);
                throw new CorruptSSTableException(e, channel.filePath());
            }
        }

        public void close()
        {
            regions.closeQuietly();
            super.close();
        }
    }
}
