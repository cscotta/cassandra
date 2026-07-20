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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.github.luben.zstd.EndDirective;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDictCompress;
import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ZstdCompressorBase implements ICompressor
{
    // These might change with the version of Zstd we're using
    public static final int FAST_COMPRESSION_LEVEL = Zstd.minCompressionLevel();
    public static final int BEST_COMPRESSION_LEVEL = Zstd.maxCompressionLevel();

    // Compressor Defaults
    public static final int DEFAULT_COMPRESSION_LEVEL = 3;
    public static final boolean ENABLE_CHECKSUM_FLAG = true;

    // Compressor option names
    public static final String COMPRESSION_LEVEL_OPTION_NAME = "compression_level";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final int compressionLevel;
    private final Set<ICompressor.Uses> recommendedUses;
    private final Set<String> supportedOptions;

    protected ZstdCompressorBase(int compressionLevel, Set<String> supportedOptions)
    {
        this.compressionLevel = compressionLevel;
        this.supportedOptions = Collections.unmodifiableSet(supportedOptions);
        this.recommendedUses = Set.of(ICompressor.Uses.GENERAL);
        logger.trace("Creating Zstd Compressor with compression level={}", compressionLevel);
    }

    @Override
    public int initialCompressedBufferLength(int chunkLength)
    {
        return (int) Zstd.compressBound(chunkLength);
    }

    @Override
    public BufferType preferredBufferType()
    {
        return BufferType.OFF_HEAP;
    }

    @Override
    public boolean supports(BufferType bufferType)
    {
        return bufferType == BufferType.OFF_HEAP;
    }

    @Override
    public Set<Uses> recommendedUses()
    {
        return recommendedUses;
    }

    @VisibleForTesting
    public int compressionLevel()
    {
        return compressionLevel;
    }

    @Override
    public Set<String> supportedOptions()
    {
        return supportedOptions;
    }

    /**
     * Decompress data using arrays
     *
     * @param input
     * @param inputOffset
     * @param inputLength
     * @param output
     * @param outputOffset
     * @return
     * @throws IOException
     */
    @Override
    public int uncompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
    throws IOException
    {
        long dsz;
        try
        {
            dsz = Zstd.decompressByteArray(output, outputOffset, output.length - outputOffset,
                                           input, inputOffset, inputLength);
        }
        catch (Exception e)
        {
            throw new IOException("Decompression failed", e);
        }

        if (Zstd.isError(dsz))
            throw new IOException("Decompression failed due to " + Zstd.getErrorName(dsz));

        return (int) dsz;
    }

    /**
     * Decompress data via ByteBuffers
     *
     * @param input
     * @param output
     * @throws IOException
     */
    @Override
    public void uncompress(ByteBuffer input, ByteBuffer output) throws IOException
    {
        try
        {
            Zstd.decompress(output, input);
        } catch (Exception e)
        {
            throw new IOException("Decompression failed", e);
        }
    }

    /**
     * Compress using ByteBuffers
     *
     * @param input
     * @param output
     * @throws IOException
     */
    @Override
    public void compress(ByteBuffer input, ByteBuffer output) throws IOException
    {
        try
        {
            Zstd.compress(output, input, compressionLevel(), ENABLE_CHECKSUM_FLAG);
        } catch (Exception e)
        {
            throw new IOException("Compression failed", e);
        }
    }

    /**
     * Fixed-output (pack-to-fill) compression via Zstd streaming: compress a prefix of {@code input}
     * into ONE frame that fills close to {@code maxOutputLen}, in roughly one forward pass over the
     * consumed input, instead of the interface default's repeated whole-prefix recompressions.
     *
     * The frame is built incrementally (feed a slice, FLUSH to observe how full the output is, feed
     * more, then END), so the total input size is not known when the frame header is written and the
     * frame carries no content-size field. The fixed-output read path
     * ({@code CompressedChunkReader.uncompressFixedBlock}) accounts for this by decompressing into a
     * destination sized to the chunk's exact uncompressed length (from the metadata offset table).
     */
    @Override
    public int compressBounded(ByteBuffer input, ByteBuffer output, int maxOutputLen) throws IOException
    {
        return streamingCompressBounded(input, output, maxOutputLen, null);
    }

    /**
     * Shared streaming pack-to-fill implementation. {@code dictionary} is applied to the compression
     * context when non-null (the dictionary-aware subclass supplies it), so dictionary compression
     * gets the same single-pass packing.
     */
    protected int streamingCompressBounded(ByteBuffer input, ByteBuffer output, int maxOutputLen,
                                           ZstdDictCompress dictionary) throws IOException
    {
        final int available = input.remaining();
        if (available == 0 || maxOutputLen <= 0)
            return 0;

        final int inputStart = input.position();
        final int inputLimit = input.limit();
        final int outputStart = output.position();
        final int outputLimit = output.limit();
        // Never let streaming write past maxOutputLen bytes of output.
        final int boundedLimit = Math.min(outputLimit, outputStart + maxOutputLen);
        // Reserve headroom so closing the frame (END: trailing block + epilogue) fits within maxOutputLen.
        final int endMargin = Math.min(Math.max(1, maxOutputLen / 4), COMPRESS_BOUNDED_OVERHEAD_MARGIN);
        final int fillTargetPos = outputStart + Math.max(1, maxOutputLen - endMargin);

        ZstdCompressCtx ctx = new ZstdCompressCtx();
        try
        {
            ctx.setLevel(compressionLevel());
            ctx.setChecksum(ENABLE_CHECKSUM_FLAG);
            if (dictionary != null)
                ctx.loadDict(dictionary);

            int consumed = 0;
            double ratio = 1.0; // compressed/uncompressed; seed conservative (incompressible), refine below

            // Feed input into one frame until the output nears the fill target. FLUSH after each feed so
            // output.position() reflects all consumed input (lets us size the next feed and decide to stop).
            for (int iter = 0; iter < MAX_COMPRESS_BOUNDED_TRIALS && consumed < available; iter++)
            {
                int remainingFill = fillTargetPos - output.position();
                if (remainingFill <= 0)
                    break;
                long est = (long) Math.ceil(remainingFill / Math.max(ratio, 1e-3));
                int step = (int) Math.min(available - consumed, Math.max(1L, est));

                input.limit(inputStart + consumed + step);
                output.limit(boundedLimit);
                boolean flushed = ctx.compressDirectByteBufferStream(output, input, EndDirective.FLUSH);
                consumed = input.position() - inputStart;
                int produced = output.position() - outputStart;
                if (consumed > 0 && produced > 0)
                    ratio = produced / (double) consumed;
                if (!flushed)
                    break; // output region full: cannot take more input
            }

            // Close the frame (epilogue) within the bounded output region. With everything already
            // flushed, END emits only the small frame footer, which fits in endMargin.
            input.limit(inputStart + consumed);
            output.limit(boundedLimit);
            boolean ended = false;
            for (int i = 0; i < MAX_COMPRESS_BOUNDED_TRIALS && !ended; i++)
                ended = ctx.compressDirectByteBufferStream(output, input, EndDirective.END);

            if (!ended || consumed == 0)
            {
                // Could not fill/close within maxOutputLen (rare: expanding data + tiny margin). Reset and
                // fall back to the interface default (multi-trial), which produces a content-size frame.
                input.position(inputStart);
                input.limit(inputLimit);
                output.position(outputStart);
                output.limit(outputLimit);
                return ICompressor.super.compressBounded(input, output, maxOutputLen);
            }

            input.position(inputStart + consumed);
            input.limit(inputLimit);
            output.limit(outputLimit);
            return consumed;
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IOException("Streaming bounded compression failed", e);
        }
        finally
        {
            ctx.close();
        }
    }

    /**
     * Check if the given compression level is valid. This can be a negative value as well.
     *
     * @param level compression level
     */
    public static void validateCompressionLevel(int level)
    {
        if (level < FAST_COMPRESSION_LEVEL || level > BEST_COMPRESSION_LEVEL)
        {
            throw new IllegalArgumentException(String.format("%s=%d is invalid", COMPRESSION_LEVEL_OPTION_NAME, level));
        }
    }

    /**
     * Get the supplied compression level; otherwise, use the default
     *
     * @param options compression options
     * @return compression level
     */
    public static int getOrDefaultCompressionLevel(Map<String, String> options)
    {
        if (options == null)
            return DEFAULT_COMPRESSION_LEVEL;

        String val = options.get(COMPRESSION_LEVEL_OPTION_NAME);

        if (val == null)
            return DEFAULT_COMPRESSION_LEVEL;

        return Integer.parseInt(val);
    }
}
