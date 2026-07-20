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
package org.apache.cassandra.io.compress;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.compression.CompressionDictionary;
import org.apache.cassandra.db.compression.CompressionDictionaryManager;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.File;
import org.apache.cassandra.io.util.FileInputStreamPlus;
import org.apache.cassandra.io.util.FileOutputStreamPlus;
import org.apache.cassandra.io.util.Memory;
import org.apache.cassandra.io.util.SafeMemory;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.Transactional;
import org.apache.cassandra.utils.concurrent.WrappedSharedCloseable;

/**
 * Holds metadata about compressed file
 * TODO extract interface ICompressionMetadata which will just provide non-resource properties
 */
public class CompressionMetadata extends WrappedSharedCloseable
{
    // Fixed-output (block-aligned) on-disk block framing: [int payloadLen][payload][int CRC32][pad].
    public static final int FIXED_BLOCK_HEADER_BYTES = 4;
    public static final int FIXED_BLOCK_CRC_BYTES = 4;

    // dataLength can represent either the true length of the file
    // or some shorter value, in the case we want to impose a shorter limit on readers
    // (when early opening, we want to ensure readers cannot read past fully written sections)
    public final long dataLength;
    public final long compressedFileLength;
    private final Memory chunkOffsets;
    public final long chunkOffsetsSize;
    public final File chunksIndexFile;
    public final CompressionParams parameters;
    @Nullable // null when no dictionary
    private final CompressionDictionary compressionDictionary;
    private volatile ICompressor resolvedCompressor;
    // Fixed-output only: the largest actual uncompressed chunk length, computed from the offset table.
    // Read buffers are sized to this (not the configured cap), so incompressible data uses ~S-sized
    // buffers that pool cleanly instead of cap-sized ones. 0 in legacy mode.
    private final int fixedMaxUncompressedChunk;

    @VisibleForTesting
    public static CompressionMetadata open(File chunksIndexFile,
                                           long compressedLength,
                                           boolean hasMaxCompressedSize)
    {
        return open(chunksIndexFile, compressedLength, hasMaxCompressedSize, null);
    }

    @VisibleForTesting
    public static CompressionMetadata open(File chunksIndexFile,
                                           long compressedLength,
                                           boolean hasMaxCompressedSize,
                                           @Nullable CompressionDictionaryManager compressionDictionaryManager)
    {
        CompressionParams parameters;
        long dataLength;
        Memory chunkOffsets;
        CompressionDictionary compressionDictionary;

        try (FileInputStreamPlus stream = chunksIndexFile.newInputStream())
        {
            String compressorName = stream.readUTF();
            int optionCount = stream.readInt();
            Map<String, String> options = new HashMap<>(optionCount);
            for (int i = 0; i < optionCount; ++i)
            {
                String key = stream.readUTF();
                String value = stream.readUTF();
                options.put(key, value);
            }
            int chunkLength = stream.readInt();
            int maxCompressedSize = Integer.MAX_VALUE;
            if (hasMaxCompressedSize)
                maxCompressedSize = stream.readInt();

            // Fixed-output mode is self-describing: the compressed target and uncompressed cap are
            // stored as reserved keys in the option map (read above). Strip them before handing the
            // remaining options to the compressor. Their absence means legacy fixed-input mode.
            int compressedChunkLength = 0;
            int maxUncompressedChunkLength = 0;
            String fixedOutputMarker = options.remove(CompressionParams.FIXED_OUTPUT_LENGTH_MARKER);
            if (fixedOutputMarker != null)
            {
                compressedChunkLength = Integer.parseInt(fixedOutputMarker);
                String capMarker = options.remove(CompressionParams.FIXED_OUTPUT_MAX_UNCOMPRESSED_MARKER);
                maxUncompressedChunkLength = capMarker != null
                                             ? Integer.parseInt(capMarker)
                                             : (int) Math.min((long) compressedChunkLength * CompressionParams.DEFAULT_MAX_UNCOMPRESSED_MULTIPLE, Integer.MAX_VALUE);
            }

            try
            {
                parameters = new CompressionParams(compressorName, chunkLength, maxCompressedSize, options,
                                                   compressedChunkLength, maxUncompressedChunkLength);
            }
            catch (ConfigurationException e)
            {
                throw new RuntimeException("Cannot create CompressionParams for stored parameters", e);
            }

            dataLength = stream.readLong();
            chunkOffsets = readChunkOffsets(stream);
            compressionDictionary = CompressionDictionary.deserialize(stream, compressionDictionaryManager);
        }
        catch (FileNotFoundException | NoSuchFileException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new CorruptSSTableException(e, chunksIndexFile);
        }

        return new CompressionMetadata(chunksIndexFile, parameters,
                                       chunkOffsets, chunkOffsets.size(), dataLength,
                                       compressedLength, compressionDictionary);
    }

    // Do not call this constructor from outside this class file, except in tests.
    // Within this class, use the static open() method or the Writer.open() method instead.
    @VisibleForTesting
    public CompressionMetadata(File chunksIndexFile,
                               CompressionParams parameters,
                               Memory chunkOffsets,
                               long chunkOffsetsSize,
                               long dataLength,
                               long compressedFileLength,
                               CompressionDictionary compressionDictionary)
    {
        // Build array with chunkOffsets and a wrapper that releases dictionary ref
        super(buildCloseableArray(chunkOffsets, compressionDictionary));
        this.chunksIndexFile = chunksIndexFile;
        this.parameters = parameters;
        this.dataLength = dataLength;
        this.compressedFileLength = compressedFileLength;
        this.chunkOffsets = chunkOffsets;
        this.chunkOffsetsSize = chunkOffsetsSize;
        this.compressionDictionary = compressionDictionary;
        this.fixedMaxUncompressedChunk = parameters.usesFixedOutputChunks()
                                         ? computeFixedMaxUncompressedChunk(chunkOffsets, chunkOffsetsSize, dataLength, parameters.compressedChunkLength())
                                         : 0;
    }

    /**
     * The largest actual uncompressed chunk length across the offset table (fixed-output). Sizing read
     * buffers to this instead of the configured cap keeps incompressible data on ~S-sized buffers that
     * pool cleanly. O(chunkCount) once per open.
     */
    private static int computeFixedMaxUncompressedChunk(Memory chunkOffsets, long chunkOffsetsSize, long dataLength, int compressedChunkLength)
    {
        long count = chunkOffsetsSize >> 3;
        int max = compressedChunkLength; // never smaller than one block target
        for (long i = 0; i < count; i++)
        {
            long base = chunkOffsets.getLong(i << 3);
            long end = (i + 1 < count) ? chunkOffsets.getLong((i + 1) << 3) : dataLength;
            int len = (int) (end - base);
            if (len > max)
                max = len;
        }
        return max;
    }

    private static AutoCloseable[] buildCloseableArray(Memory chunkOffsets, CompressionDictionary dictionary)
    {
        if (dictionary == null)
            return new AutoCloseable[] { chunkOffsets };

        Ref<? extends CompressionDictionary> dictRef = dictionary.tryRef();
        if (dictRef == null)
        {
            // Close chunkOffsets before throwing to prevent resource leak.
            // The CompressionMetadata constructor will not complete if we throw here,
            // so we must clean up resources that were passed in.
            chunkOffsets.close();
            throw new IllegalStateException("Failed to acquire reference to compression dictionary");
        }

        return new AutoCloseable[] { chunkOffsets, dictRef::release };
    }

    /**
     * Copy constructor for creating shared copies via sharedCopy().
     * <br>
     * This uses the WrappedSharedCloseable pattern where all copies share the same
     * underlying resources (chunkOffsets Memory and dictionary reference). The super()
     * call increments the shared reference count, and resources are only released when
     * the last copy is closed.
     * <br>
     * Reference counting behavior:
     * - Original CompressionMetadata acquires 1 dictionary reference (in buildCloseableArray)
     * - All copies share that reference (via super(copy) incrementing shared ref count)
     * - When last copy closes, WrappedSharedCloseable.Tidy releases the reference once
     */
    private CompressionMetadata(CompressionMetadata copy)
    {
        super(copy);
        this.chunksIndexFile = copy.chunksIndexFile;
        this.parameters = copy.parameters;
        this.dataLength = copy.dataLength;
        this.compressedFileLength = copy.compressedFileLength;
        this.chunkOffsets = copy.chunkOffsets;
        this.chunkOffsetsSize = copy.chunkOffsetsSize;
        this.compressionDictionary = copy.compressionDictionary;
        this.resolvedCompressor = copy.resolvedCompressor;
        this.fixedMaxUncompressedChunk = copy.fixedMaxUncompressedChunk;
    }

    public ICompressor compressor()
    {
        ICompressor result = resolvedCompressor;
        if (result != null)
            return result;

        synchronized (this)
        {
            result = resolvedCompressor;
            if (result == null)
            {
                result = resolveCompressor(parameters.getSstableCompressor(), compressionDictionary);
                resolvedCompressor = result;
            }
            return result;
        }
    }

    static ICompressor resolveCompressor(ICompressor compressor, CompressionDictionary dictionary)
    {
        if (dictionary == null)
            return compressor;

        // When the attached dictionary can be consumed by the current dictionary compressor
        if (compressor instanceof IDictionaryCompressor)
        {
            IDictionaryCompressor dictionaryCompressor = (IDictionaryCompressor) compressor;
            if (dictionaryCompressor.canConsumeDictionary(dictionary))
                return dictionaryCompressor.getOrCopyWithDictionary(dictionary);
        }

        // When the current compressor is not compatible with the dictionary. It could happen in the read path when:
        // 1. The current compressor is not a dictionary compressor, but there is dictionary attached
        // 2. The current dictionary compressor is a different type, e.g. table schema is changed
        // In those cases, we should get the compatible dictionary compressor based on the dictionary
        return dictionary.kind().createCompressor(dictionary);
    }

    public int chunkLength()
    {
        return parameters.chunkLength();
    }

    public int maxCompressedLength()
    {
        return parameters.maxCompressedLength();
    }

    /**
     * Returns the amount of memory in bytes used off heap.
     * @return the amount of memory in bytes used off heap
     */
    public long offHeapSize()
    {
        return chunkOffsets.size();
    }

    @Override
    public void addTo(Ref.IdentityCollection identities)
    {
        super.addTo(identities);
        identities.add(chunkOffsets);
        // Note: compressionDictionary ref is managed by WrappedSharedCloseable,
        // so it's already tracked through the parent's identity collection
    }

    @Override
    public CompressionMetadata sharedCopy()
    {
        return new CompressionMetadata(this);
    }

    /**
     * Read offsets of the individual chunks from the given input.
     *
     * @param input Source of the data.
     *
     * @return collection of the chunk offsets.
     */
    private static Memory readChunkOffsets(FileInputStreamPlus input)
    {
        final int chunkCount;
        try
        {
            chunkCount = input.readInt();
            if (chunkCount <= 0)
                throw new IOException("Compressed file with 0 chunks encountered: " + input);
        }
        catch (IOException e)
        {
            throw new FSReadError(e, input.file);
        }

        Memory offsets = Memory.allocate(chunkCount * 8L);
        int i = 0;
        try
        {

            for (i = 0; i < chunkCount; i++)
            {
                offsets.setLong(i * 8L, input.readLong());
            }

            return offsets;
        }
        catch (IOException e)
        {
            if (offsets != null)
                offsets.close();

            if (e instanceof EOFException)
            {
                String msg = String.format("Corrupted Index File %s: read %d but expected %d chunks.",
                                           input.file.path(), i, chunkCount);
                throw new CorruptSSTableException(new IOException(msg, e), input.file);
            }
            throw new FSReadError(e, input.file);
        }
    }

    /**
     * Get a chunk of compressed data (offset, length) corresponding to given position
     *
     * @param position Position in the file.
     * @return pair of chunk offset and length.
     */
    public Chunk chunkFor(long position)
    {
        if (parameters.usesFixedOutputChunks())
            return fixedOutputChunkFor(position);

        // position of the chunk
        long idx = 8 * (position / parameters.chunkLength());

        if (idx >= chunkOffsetsSize)
            throw new CorruptSSTableException(new EOFException(), chunksIndexFile);

        if (idx < 0)
            throw new CorruptSSTableException(new IllegalArgumentException(String.format("Invalid negative chunk index %d with position %d", idx, position)),
                                              chunksIndexFile);

        long chunkOffset = chunkOffsets.getLong(idx);
        long nextChunkOffset = (idx + 8 == chunkOffsetsSize)
                                ? compressedFileLength
                                : chunkOffsets.getLong(idx + 8);

        return new Chunk(chunkOffset, (int) (nextChunkOffset - chunkOffset - 4)); // "4" bytes reserved for checksum
    }

    /**
     * Fixed-output mode: chunk i occupies a fixed, block-aligned {@code S} bytes at compressed
     * offset {@code i * S}; the on-disk block carries its own length prefix, checksum, and padding,
     * so the returned {@link Chunk} length is the full aligned block size {@code S} (the reader
     * reads exactly that and parses the block internally).
     */
    private Chunk fixedOutputChunkFor(long position)
    {
        if (position < 0)
            throw new CorruptSSTableException(new IllegalArgumentException(String.format("Invalid negative position %d", position)),
                                              chunksIndexFile);
        if (position >= dataLength)
            throw new CorruptSSTableException(new EOFException(), chunksIndexFile);

        int idx = fixedChunkIndex(position);
        long s = parameters.compressedChunkLength();
        return new Chunk(idx * s, (int) s);
    }

    /**
     * Binary-searches the stored uncompressed-offset table for the index of the chunk containing
     * {@code position} (largest index whose uncompressed start &le; position). Fixed-output only.
     */
    private int fixedChunkIndex(long position)
    {
        long count = chunkOffsetsSize >> 3;
        long lo = 0;
        long hi = count - 1;
        long result = 0;
        while (lo <= hi)
        {
            long mid = (lo + hi) >>> 1;
            long off = chunkOffsets.getLong(mid << 3);
            if (off <= position)
            {
                result = mid;
                lo = mid + 1;
            }
            else
            {
                hi = mid - 1;
            }
        }
        return (int) result;
    }

    /**
     * @return the uncompressed offset of the start of the chunk containing {@code position}. Used by
     * the rebufferer/cache to align and key on the chunk base (the legacy path masks by chunkLength).
     */
    public long uncompressedChunkBase(long position)
    {
        if (parameters.usesFixedOutputChunks())
            return chunkOffsets.getLong((long) fixedChunkIndex(position) << 3);
        return (position / parameters.chunkLength()) * (long) parameters.chunkLength();
    }

    /**
     * @return the uncompressed byte length of the chunk containing {@code position} (the amount the
     * chunk decompresses to). In fixed-output mode this varies per chunk.
     */
    public int uncompressedChunkLength(long position)
    {
        if (parameters.usesFixedOutputChunks())
        {
            int idx = fixedChunkIndex(position);
            long count = chunkOffsetsSize >> 3;
            long base = chunkOffsets.getLong((long) idx << 3);
            long end = (idx + 1 < count) ? chunkOffsets.getLong((long) (idx + 1) << 3) : dataLength;
            return (int) (end - base);
        }
        long base = (position / parameters.chunkLength()) * (long) parameters.chunkLength();
        return (int) Math.min(parameters.chunkLength(), dataLength - base);
    }

    /**
     * @return the exact uncompressed length of a fixed-output {@code chunk}, derived from its compressed
     * offset (which is {@code chunkIndex * S}). The reader sizes the decompression destination to this
     * so that streaming-written frames (which carry no content-size header) decompress correctly; it is
     * also correct for frames that do carry a content size. Fixed-output only.
     */
    public int fixedOutputUncompressedLength(Chunk chunk)
    {
        long s = parameters.compressedChunkLength();
        int idx = (int) (chunk.offset / s);
        long count = chunkOffsetsSize >> 3;
        long base = chunkOffsets.getLong((long) idx << 3);
        long end = (idx + 1 < count) ? chunkOffsets.getLong((long) (idx + 1) << 3) : dataLength;
        return (int) (end - base);
    }

    /**
     * @return the size of the read/decompression buffer for one chunk: the fixed-output uncompressed
     * cap, or the (fixed) chunk length in legacy mode.
     */
    public int bufferSize()
    {
        return parameters.usesFixedOutputChunks() ? fixedMaxUncompressedChunk : parameters.chunkLength();
    }

    public long getDataOffsetForChunkOffset(long chunkOffset)
    {
        if (parameters.usesFixedOutputChunks())
        {
            long s = parameters.compressedChunkLength();
            long idx = chunkOffset / s;
            if (idx < 0 || (idx << 3) >= chunkOffsetsSize || chunkOffset % s != 0)
                throw new IllegalArgumentException("No chunk with offset " + chunkOffset);
            return chunkOffsets.getLong(idx << 3);
        }

        long l = 0;
        long h = (chunkOffsetsSize >> 3) - 1;
        long idx, offset;

        while (l <= h)
        {
            idx = (l + h) >>> 1;
            offset = chunkOffsets.getLong(idx << 3);

            if (offset < chunkOffset)
                l = idx + 1;
            else if (offset > chunkOffset)
                h = idx - 1;
            else
                return idx * parameters.chunkLength();
        }

        throw new IllegalArgumentException("No chunk with offset " + chunkOffset);
    }

    /**
     * @param sections Collection of sections in uncompressed file. Should not contain sections that overlap each other.
     * @return Total chunk size in bytes for given sections including checksum.
     */
    public long getTotalSizeForSections(Collection<SSTableReader.PartitionPositionBounds> sections)
    {
        long size = 0;
        long lastOffset = -1;
        for (SSTableReader.PartitionPositionBounds section : sections)
        {
            if (parameters.usesFixedOutputChunks())
            {
                if (section.upperPosition <= section.lowerPosition)
                    continue;
                long s = parameters.compressedChunkLength();
                int startIndex = fixedChunkIndex(section.lowerPosition);
                int endIndex = fixedChunkIndex(section.upperPosition - 1);
                for (int i = startIndex; i <= endIndex; i++)
                {
                    long chunkOffset = i * s;
                    if (chunkOffset > lastOffset)
                    {
                        lastOffset = chunkOffset;
                        size += s; // fixed-output block: checksum + padding are already inside S
                    }
                }
                continue;
            }

            int startIndex = (int) (section.lowerPosition / parameters.chunkLength());

            int endIndex = (int) (section.upperPosition / parameters.chunkLength());
            if (section.upperPosition % parameters.chunkLength() == 0)
                endIndex--;

            for (int i = startIndex; i <= endIndex; i++)
            {
                long offset = i * 8L;
                long chunkOffset = chunkOffsets.getLong(offset);
                if (chunkOffset > lastOffset)
                {
                    lastOffset = chunkOffset;
                    long nextChunkOffset = offset + 8 == chunkOffsetsSize
                                                   ? compressedFileLength
                                                   : chunkOffsets.getLong(offset + 8);
                    size += (nextChunkOffset - chunkOffset);
                }
            }
        }
        return size;
    }

    /**
     * @param sections Collection of sections in uncompressed file
     * @return Array of chunks which corresponds to given sections of uncompressed file, sorted by chunk offset
     */
    public Chunk[] getChunksForSections(Collection<SSTableReader.PartitionPositionBounds> sections)
    {
        // use SortedSet to eliminate duplicates and sort by chunk offset
        SortedSet<Chunk> offsets = new TreeSet<>((o1, o2) -> Longs.compare(o1.offset, o2.offset));

        for (SSTableReader.PartitionPositionBounds section : sections)
        {
            if (parameters.usesFixedOutputChunks())
            {
                if (section.upperPosition <= section.lowerPosition)
                    continue;
                long s = parameters.compressedChunkLength();
                int startIndex = fixedChunkIndex(section.lowerPosition);
                int endIndex = fixedChunkIndex(section.upperPosition - 1);
                for (int i = startIndex; i <= endIndex; i++)
                    offsets.add(new Chunk(i * s, (int) s)); // full aligned block (length prefix + CRC + pad inside)
                continue;
            }

            int startIndex = (int) (section.lowerPosition / parameters.chunkLength());

            int endIndex = (int) (section.upperPosition / parameters.chunkLength());
            if (section.upperPosition % parameters.chunkLength() == 0)
                endIndex--;

            for (int i = startIndex; i <= endIndex; i++)
            {
                long offset = i * 8L;
                long chunkOffset = chunkOffsets.getLong(offset);
                long nextChunkOffset = offset + 8 == chunkOffsetsSize
                                     ? compressedFileLength
                                     : chunkOffsets.getLong(offset + 8);
                offsets.add(new Chunk(chunkOffset, (int) (nextChunkOffset - chunkOffset - 4))); // "4" bytes reserved for checksum
            }
        }

        return offsets.toArray(new Chunk[offsets.size()]);
    }

    public static class Writer extends Transactional.AbstractTransactional implements Transactional
    {
        // path to the file
        private final CompressionParams parameters;
        private final File file;
        private int maxCount = 100;
        private SafeMemory offsets = new SafeMemory(maxCount * 8L);
        private int count = 0;

        // provided by user when setDescriptor
        private long dataLength, chunkCount;
        @Nullable
        private final CompressionDictionary compressionDictionary;
        @Nullable // Reference to keep dictionary alive during write
        private Ref<? extends CompressionDictionary> compressionDictionaryRef;

        private Writer(CompressionParams parameters, File file, CompressionDictionary compressionDictionary)
        {
            this.parameters = parameters;
            this.file = file;
            this.compressionDictionary = compressionDictionary;
            // Take a reference to ensure dictionary stays alive during SSTable write
            if (compressionDictionary != null)
            {
                this.compressionDictionaryRef = compressionDictionary.tryRef();
                if (compressionDictionaryRef == null)
                {
                    // Clean up offsets SafeMemory allocated in field initializer before throwing
                    // to prevent resource leak. The offsets field is initialized before constructor
                    // body runs, so it must be explicitly cleaned up if construction fails.
                    offsets.close();
                    throw new IllegalStateException("Failed to acquire reference to compression dictionary " + compressionDictionary.dictId());
                }
            }
        }

        /**
         * Creates a new Writer for compression metadata.
         *
         * Note on resource management: If this method throws an exception, all resources
         * are properly cleaned up. The Writer constructor ensures that if dictionary
         * reference acquisition fails, the offsets SafeMemory is released.
         */
        public static Writer open(CompressionParams parameters,
                                  File file,
                                  CompressionDictionary compressionDictionary)
        {
            return new Writer(parameters, file, compressionDictionary);
        }

        public void addOffset(long offset)
        {
            if (count == maxCount)
            {
                SafeMemory newOffsets = offsets.copy((maxCount *= 2L) * 8L);
                offsets.close();
                offsets = newOffsets;
            }
            offsets.setLong(8L * count++, offset);
        }

        private void writeHeader(DataOutput out, long dataLength, int chunks)
        {
            try
            {
                out.writeUTF(parameters.getSstableCompressor().serializedAs().getSimpleName());

                // Fixed-output params ride along as reserved keys in the option map so the mode is
                // known before the offset table is read, without a format-version bump.
                Map<String, String> headerOptions = parameters.getOtherOptions();
                if (parameters.usesFixedOutputChunks())
                {
                    headerOptions = new HashMap<>(headerOptions);
                    headerOptions.put(CompressionParams.FIXED_OUTPUT_LENGTH_MARKER, Integer.toString(parameters.compressedChunkLength()));
                    headerOptions.put(CompressionParams.FIXED_OUTPUT_MAX_UNCOMPRESSED_MARKER, Integer.toString(parameters.maxUncompressedChunkLength()));
                }
                out.writeInt(headerOptions.size());
                for (Map.Entry<String, String> entry : headerOptions.entrySet())
                {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }

                // store the length of the chunk
                out.writeInt(parameters.chunkLength());
                out.writeInt(parameters.maxCompressedLength());
                // store position and reserve a place for uncompressed data length and chunks count
                out.writeLong(dataLength);
                out.writeInt(chunks);
            }
            catch (IOException e)
            {
                throw new FSWriteError(e, file);
            }
        }

        private void writeCompressionDictionary(DataOutput out)
        {
            if (compressionDictionary == null)
                return;

            try
            {
                compressionDictionary.serialize(out);
            }
            catch (IOException e)
            {
                throw new FSWriteError(e, file);
            }
        }

        // we've written everything; wire up some final metadata state
        public Writer finalizeLength(long dataLength, int chunkCount)
        {
            this.dataLength = dataLength;
            this.chunkCount = chunkCount;
            return this;
        }

        @Override
        public void doPrepare()
        {
            assert chunkCount == count;

            // finalize the size of memory used if it won't now change;
            // unnecessary if already correct size
            if (offsets.size() != count * 8L)
            {
                SafeMemory tmp = offsets;
                offsets = offsets.copy(count * 8L);
                tmp.free();
            }

            // flush the data to disk
            try (FileOutputStreamPlus out = file.newOutputStream(File.WriteMode.OVERWRITE))
            {
                writeHeader(out, dataLength, count);
                for (int i = 0; i < count; i++)
                    out.writeLong(offsets.getLong(i * 8L));

                writeCompressionDictionary(out);
                out.flush();
                out.sync();
            }
            catch (FileNotFoundException | NoSuchFileException fnfe)
            {
                throw new RuntimeException(fnfe);
            }
            catch (IOException e)
            {
                throw new FSWriteError(e, file);
            }
        }

        public CompressionMetadata open(long dataLength, long compressedLength)
        {
            SafeMemory tOffsets = this.offsets.sharedCopy();

            int tCount;
            if (parameters.usesFixedOutputChunks())
            {
                // offsets hold uncompressed cumulative chunk starts; keep chunks whose start is
                // within the (possibly truncated) dataLength. Each chunk occupies a fixed S bytes.
                long s = parameters.compressedChunkLength();
                tCount = 0;
                while (tCount < this.count && tOffsets.getLong(tCount * 8L) < dataLength)
                    tCount++;
                assert tCount > 0;
                if (tCount < this.count)
                    compressedLength = tCount * s;
            }
            else
            {
                // calculate how many entries we need, if our dataLength is truncated
                tCount = (int) (dataLength / parameters.chunkLength());
                if (dataLength % parameters.chunkLength() != 0)
                    tCount++;

                assert tCount > 0;
                // grab our actual compressed length from the next offset from our the position we're opened to
                if (tCount < this.count)
                    compressedLength = tOffsets.getLong(tCount * 8L);
            }

            return new CompressionMetadata(file, parameters,
                                           tOffsets, tCount * 8L, dataLength,
                                           compressedLength, compressionDictionary);
        }

        /**
         * Get a chunk offset by it's index.
         *
         * @param chunkIndex Index of the chunk.
         *
         * @return offset of the chunk in the compressed file.
         */
        public long chunkOffsetBy(int chunkIndex)
        {
            return offsets.getLong(chunkIndex * 8L);
        }

        /**
         * Reset the writer so that the next chunk offset written will be the
         * one of {@code chunkIndex}.
         *
         * @param chunkIndex the next index to write
         */
        public void resetAndTruncate(int chunkIndex)
        {
            count = chunkIndex;
        }

        @Override
        protected Throwable doPostCleanup(Throwable failed)
        {
            return offsets.close(failed);
        }

        @Override
        protected Throwable doCommit(Throwable accumulate)
        {
            // Release the dictionary reference after successful write
            if (compressionDictionaryRef != null)
            {
                compressionDictionaryRef.release();
                compressionDictionaryRef = null;
            }
            return accumulate;
        }

        @Override
        protected Throwable doAbort(Throwable accumulate)
        {
            // Release the dictionary reference
            if (compressionDictionaryRef != null)
            {
                compressionDictionaryRef.release();
                compressionDictionaryRef = null;
            }
            return accumulate;
        }
    }

    /**
     * Holds offset and length of the file chunk
     */
    public static class Chunk
    {
        public static final IVersionedSerializer<Chunk> serializer = new ChunkSerializer();

        public final long offset;
        public final int length;

        public Chunk(long offset, int length)
        {
            assert(length > 0);

            this.offset = offset;
            this.length = length;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Chunk chunk = (Chunk) o;
            return length == chunk.length && offset == chunk.offset;
        }

        @Override
        public int hashCode()
        {
            int result = (int) (offset ^ (offset >>> 32));
            result = 31 * result + length;
            return result;
        }

        @Override
        public String toString()
        {
            return String.format("Chunk<offset: %d, length: %d>", offset, length);
        }

        /**
         * @return the end of the chunk in the file, including the checksum
         */
        public long chunkEnd()
        {
            return offset + length + 4;
        }
    }

    static class ChunkSerializer implements IVersionedSerializer<Chunk>
    {
        @Override
        public void serialize(Chunk chunk, DataOutputPlus out, int version) throws IOException
        {
            out.writeLong(chunk.offset);
            out.writeInt(chunk.length);
        }

        @Override
        public Chunk deserialize(DataInputPlus in, int version) throws IOException
        {
            return new Chunk(in.readLong(), in.readInt());
        }

        @Override
        public long serializedSize(Chunk chunk, int version)
        {
            long size = TypeSizes.sizeof(chunk.offset);
            size += TypeSizes.sizeof(chunk.length);
            return size;
        }
    }
}
