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
package org.apache.cassandra.schema;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import org.apache.cassandra.config.CassandraRelevantProperties;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.compression.CompressionDictionary;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.compress.CompressorRegistry;
import org.apache.cassandra.io.compress.DeflateCompressor;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.io.compress.IDictionaryCompressor;
import org.apache.cassandra.io.compress.LZ4Compressor;
import org.apache.cassandra.io.compress.NoopCompressor;
import org.apache.cassandra.io.compress.SnappyCompressor;
import org.apache.cassandra.io.compress.ZstdCompressor;
import org.apache.cassandra.io.compress.ZstdDictionaryCompressor;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;

import static java.lang.String.format;

public final class CompressionParams
{
    public static final int DEFAULT_CHUNK_LENGTH = 1024 * 16;
    public static final double DEFAULT_MIN_COMPRESS_RATIO = 0.0;        // Since pre-4.0 versions do not understand the
                                                                        // new compression parameter we can't use a
                                                                        // different default value.
    public static final IVersionedSerializer<CompressionParams> serializer = new Serializer();

    public static final String CLASS = "class";
    public static final String CHUNK_LENGTH_IN_KB = "chunk_length_in_kb";
    public static final String ENABLED = "enabled";
    public static final String MIN_COMPRESS_RATIO = "min_compress_ratio";

    // Fixed-output (block-aligned) compression mode. When present, each compressed chunk is
    // written to a fixed, block-aligned on-disk size (COMPRESSED_CHUNK_LENGTH_IN_KB) by packing a
    // variable amount of uncompressed input until the compressed output fills the target. The
    // uncompressed size per chunk therefore varies, capped by MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB
    // (which bounds the decompression buffer). Absence of COMPRESSED_CHUNK_LENGTH_IN_KB selects the
    // legacy fixed-uncompressed-input mode.
    public static final String COMPRESSED_CHUNK_LENGTH_IN_KB = "compressed_chunk_length_in_kb";
    public static final String MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB = "max_uncompressed_chunk_length_in_kb";

    // The fixed-output target must be a multiple of this floor so it is a whole number of device
    // blocks (typical block size 512 or 4096). Buffered reads additionally benefit when it is a
    // multiple of the OS page size; that is an operator choice (this class validates only the block
    // floor, since the runtime page size is not known at schema-parse time).
    public static final int FIXED_OUTPUT_ALIGNMENT_FLOOR = 4096;
    // Upper bound on the fixed-output target and the uncompressed cap. Both size direct buffers held
    // per writer and per read thread, so bound them well below the ~2 GB int-parse ceiling.
    public static final int MAX_FIXED_OUTPUT_LENGTH = 256 * 1024 * 1024;
    // Default cap on the uncompressed bytes packed into one fixed-output chunk, as a multiple of the
    // compressed target, used when MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB is not given.
    public static final int DEFAULT_MAX_UNCOMPRESSED_MULTIPLE = 8;

    // Reserved keys used ONLY inside the -CompressionInfo.db option map to record fixed-output
    // parameters without a format-version bump (the option map is self-delimiting and read before
    // the chunk-offset table, so the mode is known in time to interpret the offsets). These are
    // never exposed to compressors or to the schema map. Both values are byte counts.
    public static final String FIXED_OUTPUT_LENGTH_MARKER = "org.apache.cassandra.fixed_output_chunk_length";
    public static final String FIXED_OUTPUT_MAX_UNCOMPRESSED_MARKER = "org.apache.cassandra.fixed_output_max_uncompressed";

    private static final CompressorRegistry registry = CompressorRegistry.instance;
    public static final CompressionParams DEFAULT = !CassandraRelevantProperties.DETERMINISM_SSTABLE_COMPRESSION_DEFAULT.getBoolean()
                                                    ? noCompression()
                                                    : new CompressionParams(LZ4Compressor.create(Collections.emptyMap()),
                                                                            DEFAULT_CHUNK_LENGTH,
                                                                            calcMaxCompressedLength(DEFAULT_CHUNK_LENGTH, DEFAULT_MIN_COMPRESS_RATIO),
                                                                            DEFAULT_MIN_COMPRESS_RATIO,
                                                                            Collections.emptyMap());

    public static final CompressionParams NOOP = new CompressionParams(NoopCompressor.create(Collections.emptyMap()),
                                                                       // 4 KiB is often the underlying disk block size
                                                                       1024 * 4,
                                                                       Integer.MAX_VALUE,
                                                                       DEFAULT_MIN_COMPRESS_RATIO,
                                                                       Collections.emptyMap());

    private final ICompressor sstableCompressor;
    private final int chunkLength;
    private final int maxCompressedLength;  // In content we store max length to avoid rounding errors causing compress/decompress mismatch.
    private final double minCompressRatio;  // In configuration we store min ratio, the input parameter.
    private final ImmutableMap<String, String> otherOptions; // Unrecognized options, can be used by the compressor

    // Fixed-output mode (0 = disabled/legacy). When > 0, the block-aligned on-disk size of every
    // compressed chunk, and maxUncompressedChunkLength caps the uncompressed bytes packed into one.
    private final int compressedChunkLength;
    private final int maxUncompressedChunkLength;

    public static CompressionParams fromMap(Map<String, String> opts)
    {
        Map<String, String> options = copyOptions(opts);

        String sstableCompressionClass;

        if (!opts.isEmpty() && isEnabled(opts) && !options.containsKey(CLASS))
            throw new ConfigurationException(format("Missing sub-option '%s' for the 'compression' option.", CLASS));

        if (!removeEnabled(options) && !options.isEmpty())
            throw new ConfigurationException(format("If the '%s' option is set to false no other options must be specified", ENABLED));
        else
            sstableCompressionClass = removeSSTableCompressionClass(options);

        int chunkLength = removeChunkLength(options);
        double minCompressRatio = removeMinCompressRatio(options);
        int compressedChunkLength = removeCompressedChunkLength(options);
        int maxUncompressedChunkLength = removeMaxUncompressedChunkLength(options, compressedChunkLength);

        CompressionParams cp = new CompressionParams(sstableCompressionClass, options, chunkLength, minCompressRatio,
                                                     compressedChunkLength, maxUncompressedChunkLength);
        cp.validate();

        return cp;
    }

    public Class<? extends ICompressor> klass()
    {
        return sstableCompressor.getClass();
    }

    public static CompressionParams noCompression()
    {
        return new CompressionParams(null, DEFAULT_CHUNK_LENGTH, Integer.MAX_VALUE, 0.0, Collections.emptyMap());
    }

    // The shorthand methods below are only used for tests. They are a little inconsistent in their choice of
    // parameters -- this is done on purpose to test out various compression parameter combinations.

    @VisibleForTesting
    public static CompressionParams snappy()
    {
        return snappy(DEFAULT_CHUNK_LENGTH);
    }

    @VisibleForTesting
    public static CompressionParams snappy(int chunkLength)
    {
        return snappy(chunkLength, 1.1);
    }

    @VisibleForTesting
    public static CompressionParams snappy(int chunkLength, double minCompressRatio)
    {
        return new CompressionParams(SnappyCompressor.instance, chunkLength, calcMaxCompressedLength(chunkLength, minCompressRatio), minCompressRatio, Collections.emptyMap());
    }

    @VisibleForTesting
    public static CompressionParams deflate()
    {
        return deflate(DEFAULT_CHUNK_LENGTH);
    }

    @VisibleForTesting
    public static CompressionParams deflate(int chunkLength)
    {
        return new CompressionParams(DeflateCompressor.instance, chunkLength, Integer.MAX_VALUE, 0.0, Collections.emptyMap());
    }

    @VisibleForTesting
    public static CompressionParams lz4()
    {
        return lz4(DEFAULT_CHUNK_LENGTH);
    }

    @VisibleForTesting
    public static CompressionParams lz4(int chunkLength)
    {
        return lz4(chunkLength, chunkLength);
    }

    @VisibleForTesting
    public static CompressionParams lz4(int chunkLength, int maxCompressedLength)
    {
        return new CompressionParams(LZ4Compressor.create(Collections.emptyMap()), chunkLength, maxCompressedLength, calcMinCompressRatio(chunkLength, maxCompressedLength), Collections.emptyMap());
    }

    @VisibleForTesting
    public static CompressionParams zstd()
    {
        return zstd(DEFAULT_CHUNK_LENGTH, false);
    }

    @VisibleForTesting
    public static CompressionParams zstd(Integer chunkLength)
    {
        return zstd(chunkLength, false);
    }

    @VisibleForTesting
    public static CompressionParams zstd(Integer chunkLength, boolean useDictionary)
    {
        return zstd(chunkLength, useDictionary, Collections.emptyMap());
    }

    @VisibleForTesting
    public static CompressionParams zstd(Integer chunkLength, boolean useDictionary, Map<String, String> options)
    {
        ICompressor compressor = useDictionary
                                 ? ZstdDictionaryCompressor.create(options)
                                 : ZstdCompressor.create(options);
        return new CompressionParams(compressor, chunkLength, Integer.MAX_VALUE, DEFAULT_MIN_COMPRESS_RATIO, options);
    }

    @VisibleForTesting
    public static CompressionParams noop()
    {
        return noop(DEFAULT_CHUNK_LENGTH);
    }

    @VisibleForTesting
    public static CompressionParams noop(int chunkLength)
    {
        NoopCompressor compressor = NoopCompressor.create(Collections.emptyMap());
        return new CompressionParams(compressor, chunkLength, Integer.MAX_VALUE, DEFAULT_MIN_COMPRESS_RATIO, Collections.emptyMap());
    }

    public CompressionParams(String sstableCompressorClass, Map<String, String> otherOptions, int chunkLength, double minCompressRatio) throws ConfigurationException
    {
        this(sstableCompressorClass, otherOptions, chunkLength, minCompressRatio, 0, 0);
    }

    public CompressionParams(String sstableCompressorClass, Map<String, String> otherOptions, int chunkLength, double minCompressRatio, int compressedChunkLength, int maxUncompressedChunkLength) throws ConfigurationException
    {
        this(createCompressor(parseCompressorClass(sstableCompressorClass), otherOptions), chunkLength, calcMaxCompressedLength(chunkLength, minCompressRatio), minCompressRatio, otherOptions, compressedChunkLength, maxUncompressedChunkLength);
    }

    static int calcMaxCompressedLength(int chunkLength, double minCompressRatio)
    {
        return (int) Math.ceil(Math.min(chunkLength / minCompressRatio, Integer.MAX_VALUE));
    }

    public CompressionParams(String sstableCompressorClass, int chunkLength, int maxCompressedLength, Map<String, String> otherOptions) throws ConfigurationException
    {
        this(sstableCompressorClass, chunkLength, maxCompressedLength, otherOptions, 0, 0);
    }

    public CompressionParams(String sstableCompressorClass, int chunkLength, int maxCompressedLength, Map<String, String> otherOptions, int compressedChunkLength, int maxUncompressedChunkLength) throws ConfigurationException
    {
        this(createCompressor(parseCompressorClass(sstableCompressorClass), otherOptions), chunkLength, maxCompressedLength, calcMinCompressRatio(chunkLength, maxCompressedLength), otherOptions, compressedChunkLength, maxUncompressedChunkLength);
    }

    static double calcMinCompressRatio(int chunkLength, int maxCompressedLength)
    {
        if (maxCompressedLength == Integer.MAX_VALUE)
            return 0;
        return chunkLength * 1.0 / maxCompressedLength;
    }

    private CompressionParams(ICompressor sstableCompressor, int chunkLength, int maxCompressedLength, double minCompressRatio, Map<String, String> otherOptions) throws ConfigurationException
    {
        this(sstableCompressor, chunkLength, maxCompressedLength, minCompressRatio, otherOptions, 0, 0);
    }

    private CompressionParams(ICompressor sstableCompressor, int chunkLength, int maxCompressedLength, double minCompressRatio, Map<String, String> otherOptions, int compressedChunkLength, int maxUncompressedChunkLength) throws ConfigurationException
    {
        this.sstableCompressor = sstableCompressor;
        this.chunkLength = chunkLength;
        this.otherOptions = ImmutableMap.copyOf(otherOptions);
        this.minCompressRatio = minCompressRatio;
        this.maxCompressedLength = maxCompressedLength;
        this.compressedChunkLength = compressedChunkLength;
        this.maxUncompressedChunkLength = maxUncompressedChunkLength;
    }

    public CompressionParams copy()
    {
        return new CompressionParams(sstableCompressor, chunkLength, maxCompressedLength, minCompressRatio, otherOptions, compressedChunkLength, maxUncompressedChunkLength);
    }

    /**
     * Checks if compression is enabled.
     * @return {@code true} if compression is enabled, {@code false} otherwise.
     */
    public boolean isEnabled()
    {
        return sstableCompressor != null;
    }

    /**
     * Checks if dictionary compression is enabled for this configuration.
     * Dictionary compression is enabled when both compression is enabled and
     * the compressor supports dictionary-based compression.
     *
     * @return {@code true} if dictionary compression is enabled, {@code false} otherwise.
     */
    public boolean isDictionaryCompressionEnabled()
    {
        return isEnabled() && sstableCompressor instanceof IDictionaryCompressor;
    }

    /**
     * @return kind of compression dictionary the compressor accepts, or null if none
     */
    public CompressionDictionary.Kind getCompressionDictionaryKind()
    {
        if (isDictionaryCompressionEnabled())
        {
            return ((IDictionaryCompressor<?>) sstableCompressor).acceptableDictionaryKind();
        }

        return null;
    }

    /**
     * Returns the SSTable compressor.
     * @return the SSTable compressor or {@code null} if compression is disabled.
     */
    public ICompressor getSstableCompressor()
    {
        return sstableCompressor;
    }

    public ImmutableMap<String, String> getOtherOptions()
    {
        return otherOptions;
    }

    public int chunkLength()
    {
        return chunkLength;
    }

    public int maxCompressedLength()
    {
        return maxCompressedLength;
    }

    /**
     * @return {@code true} if this configuration uses the fixed-output (block-aligned) chunk mode.
     */
    public boolean usesFixedOutputChunks()
    {
        return compressedChunkLength > 0;
    }

    /**
     * @return the fixed, block-aligned on-disk size of each compressed chunk, or 0 in legacy mode.
     */
    public int compressedChunkLength()
    {
        return compressedChunkLength;
    }

    /**
     * @return the cap on uncompressed bytes packed into a single fixed-output chunk (sizes the
     * decompression buffer), or 0 in legacy mode.
     */
    public int maxUncompressedChunkLength()
    {
        return maxUncompressedChunkLength;
    }

    private static Class<?> parseCompressorClass(String className) throws ConfigurationException
    {
        if (className == null || className.isEmpty())
            return null;

        className = className.contains(".") ? className : "org.apache.cassandra.io.compress." + className;
        try
        {
            return Class.forName(className);
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Could not create Compression for type " + className, e);
        }
    }

    private static ICompressor createCompressor(Class<?> compressorClass, Map<String, String> compressionOptions) throws ConfigurationException
    {
        if (compressorClass == null)
        {
            if (!compressionOptions.isEmpty())
                throw new ConfigurationException("Unknown compression options (" + compressionOptions.keySet() + ") since no compression class found");
            return null;
        }

        return registry.getCompressor(compressorClass, compressionOptions);
    }

    public static ICompressor createCompressor(ParameterizedClass compression) throws ConfigurationException
    {
        return createCompressor(parseCompressorClass(compression.class_name), copyOptions(compression.parameters));
    }

    private static Map<String, String> copyOptions(Map<? extends CharSequence, ? extends CharSequence> co)
    {
        if (co == null || co.isEmpty())
            return Collections.emptyMap();

        Map<String, String> compressionOptions = new HashMap<>();
        for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry : co.entrySet())
            compressionOptions.put(entry.getKey().toString(), entry.getValue().toString());
        return compressionOptions;
    }

    /**
     * Parse the chunk length (in KiB) and returns it as bytes.
     *
     * @param chLengthKB the length of the chunk to parse
     * @return the chunk length in bytes
     * @throws ConfigurationException if the chunk size is too large
     */
    private static Integer parseChunkLength(String chLengthKB) throws ConfigurationException
    {
        if (chLengthKB == null)
            return null;

        try
        {
            int parsed = Integer.parseInt(chLengthKB);
            if (parsed > Integer.MAX_VALUE / 1024)
                throw new ConfigurationException(format("Value of %s is too large (%s)", CHUNK_LENGTH_IN_KB,parsed));
            return 1024 * parsed;
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException("Invalid value for " + CHUNK_LENGTH_IN_KB, e);
        }
    }

    /**
     * Removes the chunk length option from the specified set of option.
     *
     * @param options the options
     * @return the chunk length value
     */
    private static int removeChunkLength(Map<String, String> options)
    {
        if (options.containsKey(CHUNK_LENGTH_IN_KB))
        {
            return parseChunkLength(options.remove(CHUNK_LENGTH_IN_KB));
        }

        return DEFAULT_CHUNK_LENGTH;
    }

    /**
     * Removes and parses the fixed-output compressed-chunk length option (in KiB), returning the
     * target on-disk chunk size in bytes, or 0 when the option is absent (legacy mode).
     */
    private static int removeCompressedChunkLength(Map<String, String> options) throws ConfigurationException
    {
        String value = options.remove(COMPRESSED_CHUNK_LENGTH_IN_KB);
        if (value == null)
            return 0;

        try
        {
            int parsed = Integer.parseInt(value);
            if (parsed > Integer.MAX_VALUE / 1024)
                throw new ConfigurationException(format("Value of %s is too large (%s)", COMPRESSED_CHUNK_LENGTH_IN_KB, parsed));
            return 1024 * parsed;
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException("Invalid value for " + COMPRESSED_CHUNK_LENGTH_IN_KB, e);
        }
    }

    /**
     * Removes and parses the max-uncompressed-chunk length option (in KiB) for fixed-output mode,
     * returning the cap in bytes. Defaults to {@link #DEFAULT_MAX_UNCOMPRESSED_MULTIPLE} times the
     * compressed target when absent. Returns 0 in legacy mode (compressedChunkLength == 0).
     */
    private static int removeMaxUncompressedChunkLength(Map<String, String> options, int compressedChunkLength) throws ConfigurationException
    {
        String value = options.remove(MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB);
        if (compressedChunkLength <= 0)
        {
            if (value != null)
                throw new ConfigurationException(format("%s requires %s to be set", MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, COMPRESSED_CHUNK_LENGTH_IN_KB));
            return 0;
        }

        if (value == null)
            return (int) Math.min((long) compressedChunkLength * DEFAULT_MAX_UNCOMPRESSED_MULTIPLE, Integer.MAX_VALUE);

        try
        {
            int parsed = Integer.parseInt(value);
            if (parsed > Integer.MAX_VALUE / 1024)
                throw new ConfigurationException(format("Value of %s is too large (%s)", MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, parsed));
            return 1024 * parsed;
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException("Invalid value for " + MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, e);
        }
    }

    /**
     * Removes the min compress ratio option from the specified set of option.
     *
     * @param options the options
     * @return the min compress ratio, used to calculate max chunk size to write compressed
     */
    private static double removeMinCompressRatio(Map<String, String> options)
    {
        String ratio = options.remove(MIN_COMPRESS_RATIO);
        if (ratio != null)
        {
            return Double.parseDouble(ratio);
        }
        return DEFAULT_MIN_COMPRESS_RATIO;
    }

    /**
     * Removes the option specifying the name of the compression class
     *
     * @param options the options
     * @return the name of the compression class
     */
    private static String removeSSTableCompressionClass(Map<String, String> options)
    {
        if (options.containsKey(CLASS))
        {
            String clazz = options.remove(CLASS);

            if (clazz == null || clazz.isEmpty())
                throw new ConfigurationException(format("The '%s' option must not be empty. To disable compression use 'enabled' : false", CLASS));

            return clazz;
        }

        return null;
    }

    /**
     * Returns {@code true} if the options contains the {@code enabled} option and that its value is
     * {@code true}, otherwise returns {@code false}.
     *
     * @param options the options
     * @return {@code true} if the options contains the {@code enabled} option and that its value is
     * {@code true}, otherwise returns {@code false}.
     */
    public static boolean isEnabled(Map<String, String> options)
    {
        String enabled = options.get(ENABLED);
        return enabled == null || Boolean.parseBoolean(enabled);
    }

    /**
     * Removes the {@code enabled} option from the specified options.
     *
     * @param options the options
     * @return the value of the {@code enabled} option
     */
    private static boolean removeEnabled(Map<String, String> options)
    {
        String enabled = options.remove(ENABLED);
        return enabled == null || Boolean.parseBoolean(enabled);
    }

    // chunkLength must be a power of 2 because we assume so when
    // computing the chunk number from an uncompressed file offset (see
    // CompressedRandomAccessReader.decompresseChunk())
    public void validate() throws ConfigurationException
    {
        // if chunk length was not set (chunkLength == null), this is fine, default will be used
        if (chunkLength <= 0)
            throw new ConfigurationException("Invalid negative or null " + CHUNK_LENGTH_IN_KB);

        if (usesFixedOutputChunks())
        {
            // Fixed-output mode: the compressed target must be a whole number of device blocks; the
            // uncompressed size per chunk is variable and only capped. Chunk indexing is by a stored
            // uncompressed-offset table (binary search), so chunkLength need not be a power of 2.
            if (compressedChunkLength % FIXED_OUTPUT_ALIGNMENT_FLOOR != 0)
                throw new ConfigurationException(format("%s must be a multiple of %d bytes (a whole number of device blocks)",
                                                        COMPRESSED_CHUNK_LENGTH_IN_KB, FIXED_OUTPUT_ALIGNMENT_FLOOR));

            if (compressedChunkLength > MAX_FIXED_OUTPUT_LENGTH)
                throw new ConfigurationException(format("%s must not exceed %d bytes", COMPRESSED_CHUNK_LENGTH_IN_KB, MAX_FIXED_OUTPUT_LENGTH));

            if (maxUncompressedChunkLength < compressedChunkLength)
                throw new ConfigurationException(format("%s must be greater than or equal to %s",
                                                        MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, COMPRESSED_CHUNK_LENGTH_IN_KB));

            if (maxUncompressedChunkLength > MAX_FIXED_OUTPUT_LENGTH)
                throw new ConfigurationException(format("%s must not exceed %d bytes", MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, MAX_FIXED_OUTPUT_LENGTH));
            return;
        }

        if ((chunkLength & (chunkLength - 1)) != 0)
            throw new ConfigurationException(CHUNK_LENGTH_IN_KB + " must be a power of 2");

        if (maxCompressedLength < 0)
            throw new ConfigurationException("Invalid negative " + MIN_COMPRESS_RATIO);

        if (maxCompressedLength > chunkLength && maxCompressedLength < Integer.MAX_VALUE)
            throw new ConfigurationException(MIN_COMPRESS_RATIO + " can either be 0 or greater than or equal to 1");
    }

    public Map<String, String> asMap()
    {
        if (!isEnabled())
            return Collections.singletonMap(ENABLED, "false");

        Map<String, String> options = new HashMap<>(otherOptions);
        // Use the one saved in the registry, we don't want to save the name of the service provider compressor here!
        options.put(CLASS, sstableCompressor.serializedAs().getName());
        options.put(CHUNK_LENGTH_IN_KB, chunkLengthInKB());
        if (minCompressRatio != DEFAULT_MIN_COMPRESS_RATIO)
            options.put(MIN_COMPRESS_RATIO, String.valueOf(minCompressRatio));
        if (usesFixedOutputChunks())
        {
            options.put(COMPRESSED_CHUNK_LENGTH_IN_KB, String.valueOf(compressedChunkLength / 1024));
            options.put(MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, String.valueOf(maxUncompressedChunkLength / 1024));
        }

        return options;
    }

    public String chunkLengthInKB()
    {
        return String.valueOf(chunkLength() / 1024);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (!(obj instanceof CompressionParams))
            return false;

        CompressionParams cp = (CompressionParams) obj;

        return Objects.equal(sstableCompressor, cp.sstableCompressor)
            && chunkLength == cp.chunkLength
            && otherOptions.equals(cp.otherOptions)
            && minCompressRatio == cp.minCompressRatio
            && compressedChunkLength == cp.compressedChunkLength
            && maxUncompressedChunkLength == cp.maxUncompressedChunkLength;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(29, 1597)
            .append(sstableCompressor)
            .append(chunkLength)
            .append(otherOptions)
            .append(minCompressRatio)
            .append(compressedChunkLength)
            .append(maxUncompressedChunkLength)
            .toHashCode();
    }

    /**
     * Options serialized to the messaging/streaming wire and to the -CompressionInfo.db header. In
     * fixed-output mode the mode's parameters ride along as reserved marker keys (byte counts) so the
     * mode is self-describing without a wire/format version bump; a peer that does not understand
     * fixed-output rejects the unknown compressor options (fails loudly, does not misdecode).
     */
    Map<String, String> serializedOptions()
    {
        if (!usesFixedOutputChunks())
            return otherOptions;
        Map<String, String> opts = new HashMap<>(otherOptions);
        opts.put(FIXED_OUTPUT_LENGTH_MARKER, Integer.toString(compressedChunkLength));
        opts.put(FIXED_OUTPUT_MAX_UNCOMPRESSED_MARKER, Integer.toString(maxUncompressedChunkLength));
        return opts;
    }

    static class Serializer implements IVersionedSerializer<CompressionParams>
    {
        public void serialize(CompressionParams parameters, DataOutputPlus out, int version) throws IOException
        {
            assert version >= MessagingService.VERSION_40;
            out.writeUTF(parameters.sstableCompressor.serializedAs().getSimpleName());
            Map<String, String> options = parameters.serializedOptions();
            out.writeInt(options.size());
            for (Map.Entry<String, String> entry : options.entrySet())
            {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }
            out.writeInt(parameters.chunkLength());
            out.writeInt(parameters.maxCompressedLength);
        }

        public CompressionParams deserialize(DataInputPlus in, int version) throws IOException
        {
            assert version >= MessagingService.VERSION_40;
            String compressorName = in.readUTF();
            int optionCount = in.readInt();
            Map<String, String> options = new HashMap<>();
            for (int i = 0; i < optionCount; ++i)
            {
                String key = in.readUTF();
                String value = in.readUTF();
                options.put(key, value);
            }
            int chunkLength = in.readInt();
            int maxCompressedLength = in.readInt();

            // Fixed-output markers ride in the option map (byte counts); strip and apply them.
            int compressedChunkLength = 0;
            int maxUncompressedChunkLength = 0;
            String fixedOutputMarker = options.remove(FIXED_OUTPUT_LENGTH_MARKER);
            if (fixedOutputMarker != null)
            {
                compressedChunkLength = Integer.parseInt(fixedOutputMarker);
                String capMarker = options.remove(FIXED_OUTPUT_MAX_UNCOMPRESSED_MARKER);
                maxUncompressedChunkLength = capMarker != null
                                             ? Integer.parseInt(capMarker)
                                             : (int) Math.min((long) compressedChunkLength * DEFAULT_MAX_UNCOMPRESSED_MULTIPLE, Integer.MAX_VALUE);
            }

            CompressionParams parameters;
            try
            {
                parameters = new CompressionParams(compressorName, chunkLength, maxCompressedLength, options,
                                                   compressedChunkLength, maxUncompressedChunkLength);
            }
            catch (ConfigurationException e)
            {
                throw new RuntimeException("Cannot create CompressionParams for parameters", e);
            }
            return parameters;
        }

        public long serializedSize(CompressionParams parameters, int version)
        {
            assert version >= MessagingService.VERSION_40;
            long size = TypeSizes.sizeof(parameters.sstableCompressor.serializedAs().getSimpleName());
            Map<String, String> options = parameters.serializedOptions();
            size += TypeSizes.sizeof(options.size());
            for (Map.Entry<String, String> entry : options.entrySet())
            {
                size += TypeSizes.sizeof(entry.getKey());
                size += TypeSizes.sizeof(entry.getValue());
            }
            size += TypeSizes.sizeof(parameters.chunkLength());
            size += TypeSizes.sizeof(parameters.maxCompressedLength());
            return size;
        }
    }
}
