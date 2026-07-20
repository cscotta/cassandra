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
import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public interface ICompressor
{
    // Max full-compression trials the default compressBounded will run to converge on the input that
    // fills the target output size. Bounds write-side CPU; the best fitting trial is used. Raised from 5
    // to 8 so the tighter fill threshold below has enough grow/shrink steps to converge on compressible
    // data (each trial is a write-time recompression only; the read path is unaffected).
    int MAX_COMPRESS_BOUNDED_TRIALS = 8;
    // Bytes shaved off the first guess to leave room for compressor framing overhead, so incompressible
    // data fits on the first try (one compression) instead of overshooting by a few framing bytes.
    int COMPRESS_BOUNDED_OVERHEAD_MARGIN = 256;
    // "Filled enough" threshold (produced/maxOutputLen >= NUM/DEN) at which the default stops growing
    // and accepts the chunk, trading a little tail padding for fewer compressions. 63/64 (~98.4%) keeps
    // per-block padding near ~1.5% instead of the ~6.25% a looser threshold leaves; the extra trials cost
    // write-time CPU only.
    int COMPRESS_BOUNDED_FILL_NUM = 63;
    int COMPRESS_BOUNDED_FILL_DEN = 64;

    /**
     * Ways that a particular instance of ICompressor should be used internally in Cassandra.
     *
     * GENERAL: Suitable for general use
     * FAST_COMPRESSION: Suitable for use in particularly latency sensitive compression situations (flushes).
     */
    enum Uses {
        GENERAL,
        FAST_COMPRESSION
    }

    /**
     * Get the maximum compressed size in the worst case scenario
     * @param chunkLength input data (chunk) size
     * @return compressed size upper bound in the worse case
     */
    public int initialCompressedBufferLength(int chunkLength);

    public int uncompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset) throws IOException;

    /**
     * Compression for ByteBuffers.
     *
     * The data between input.position() and input.limit() is compressed and placed into output starting from output.position().
     * Positions in both buffers are moved to reflect the bytes read and written. Limits are not changed.
     */
    public void compress(ByteBuffer input, ByteBuffer output) throws IOException;

    /**
     * Fixed-output (pack-to-fill) compression: compress a prefix of {@code input} (from its position
     * to its limit) into {@code output}, packing as much input as fits without the produced
     * compressed bytes exceeding {@code maxOutputLen}. On return, {@code input.position()} has
     * advanced by the number of input bytes consumed (the return value) and {@code output} holds the
     * compressed bytes for exactly that prefix (a self-contained, independently decompressible unit).
     *
     * The default implementation is compressor-agnostic: it estimates the fitting input from a ratio
     * near 1, compresses the candidate directly into {@code output}, and keeps it if it fits (no
     * throwaway scratch, no separate final compress). Incompressible data fills the target on the
     * first try — one compression; it grows only when a chunk is under-filled and shrinks on the rare
     * overshoot. Compressors with a streaming/bounded-output API (e.g. Zstd) can override for a
     * single-pass fill that also packs highly compressible data densely.
     *
     * @param maxOutputLen the hard ceiling on produced compressed bytes (typically the fixed-output
     *                     chunk size minus framing/checksum/padding headroom)
     * @return number of input bytes consumed (0 only if not even a minimal prefix fits)
     */
    default int compressBounded(ByteBuffer input, ByteBuffer output, int maxOutputLen) throws IOException
    {
        int available = input.remaining();
        if (available == 0 || maxOutputLen <= 0)
            return 0;

        int inputStart = input.position();
        int outputStart = output.position();

        // Start just under the target (assume ratio ~1, leave a margin for framing overhead) and
        // compress directly into output, keeping the result if it fits. Grow only when under-filled;
        // shrink on overshoot. The common (incompressible) case is one compression.
        int margin = Math.min(maxOutputLen / 2, COMPRESS_BOUNDED_OVERHEAD_MARGIN);
        int guess = Math.min(available, Math.max(1, maxOutputLen - margin));
        int acceptedConsumed = 0;
        int acceptedProduced = -1;

        for (int iter = 0; iter < MAX_COMPRESS_BOUNDED_TRIALS && guess >= 1; iter++)
        {
            output.position(outputStart);
            ByteBuffer trial = input.duplicate();
            trial.position(inputStart).limit(inputStart + guess);
            compress(trial, output);
            int produced = output.position() - outputStart;
            double ratio = produced / (double) guess; // compressed / uncompressed

            if (produced <= maxOutputLen)
            {
                acceptedConsumed = guess;
                acceptedProduced = produced;
                // Stop if we packed everything available or filled the target well enough.
                if (guess == available || (long) produced * COMPRESS_BOUNDED_FILL_DEN >= (long) maxOutputLen * COMPRESS_BOUNDED_FILL_NUM)
                    break;
                // Under-filled with input to spare: grow toward the target using the observed ratio.
                int grown = (int) Math.min(available, Math.max(guess + 1L, (long) (maxOutputLen / Math.max(ratio, 1e-9))));
                if (grown <= guess)
                    break;
                guess = grown;
            }
            else
            {
                // Overshoot: shrink to just under the ratio-implied target.
                int shrunk = Math.max(1, (int) (maxOutputLen / Math.max(ratio, 1e-9) * 0.98));
                if (shrunk >= guess)
                    shrunk = guess - 1;
                guess = shrunk;
            }
        }

        if (acceptedConsumed == 0)
        {
            output.position(outputStart);
            return 0;
        }

        // The last compress may have been a rejected grow/overshoot; ensure output holds exactly the
        // accepted (fitting) result before returning.
        if (output.position() - outputStart != acceptedProduced)
        {
            output.position(outputStart);
            ByteBuffer prefix = input.duplicate();
            prefix.position(inputStart).limit(inputStart + acceptedConsumed);
            compress(prefix, output);
        }
        input.position(inputStart + acceptedConsumed);
        return acceptedConsumed;
    }

    /**
     * Decompression for DirectByteBuffers.
     *
     * The data between input.position() and input.limit() is uncompressed and placed into output starting from output.position().
     * Positions in both buffers are moved to reflect the bytes read and written. Limits are not changed.
     */
    public void uncompress(ByteBuffer input, ByteBuffer output) throws IOException;

    /**
     * Returns the preferred (most efficient) buffer type for this compressor.
     */
    public BufferType preferredBufferType();

    /**
     * Checks if the given buffer would be supported by the compressor. If a type is supported, the compressor must be
     * able to use it in combination with all other supported types.
     *
     * Direct and memory-mapped buffers must be supported by all compressors.
     */
    public boolean supports(BufferType bufferType);

    public Set<String> supportedOptions();

    /**
     * Hints to Cassandra which uses this compressor is recommended for. For example a compression algorithm which gets
     * good compression ratio may trade off too much compression speed to be useful in certain compression heavy use
     * cases such as flushes or mutation hints.
     *
     * Note that Cassandra may ignore these recommendations, it is not a strict contract.
     */
    default Set<Uses> recommendedUses()
    {
        return ImmutableSet.copyOf(EnumSet.allOf(Uses.class));
    }

    /**
     * Compressor class recorded in the table schema and SSTable compression metadata for
     * compressors built by this instance. Plugin wrappers should override this to return the
     * built-in they substitute for, so schema and on-disk format stay portable to peers and
     * restarts without the plugin.
     * <p>
     * Contract: the returned class must (a) live in {@code org.apache.cassandra.io.compress}.
     * Serializers write only its simple name; deserialization prepends that package.
     * (b) have a non-empty simple name (no anonymous/synthetic classes).
     */
    default Class<? extends ICompressor> serializedAs()
    {
        return getClass();
    }
}
