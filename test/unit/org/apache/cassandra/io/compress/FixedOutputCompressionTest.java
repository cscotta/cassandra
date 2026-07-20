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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.io.util.File;
import org.apache.cassandra.io.util.FileHandle;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.SequentialWriterOption;
import org.apache.cassandra.schema.CompressionParams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Round-trip and on-disk-layout checks for the fixed-output (block-aligned) compression mode:
 * every chunk occupies exactly {@code compressed_chunk_length_in_kb} bytes on disk, and reads must
 * reconstruct the written bytes exactly (sequential and random-access) through the variable-chunk
 * reader path.
 */
public class FixedOutputCompressionTest
{
    @BeforeClass
    public static void setupDD()
    {
        DatabaseDescriptor.daemonInitialization();
    }

    private static CompressionParams fixedOutput(int compressedKb, int maxUncompressedKb)
    {
        Map<String, String> opts = new HashMap<>();
        opts.put(CompressionParams.CLASS, "ZstdCompressor");
        opts.put(CompressionParams.COMPRESSED_CHUNK_LENGTH_IN_KB, Integer.toString(compressedKb));
        opts.put(CompressionParams.MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, Integer.toString(maxUncompressedKb));
        return CompressionParams.fromMap(opts);
    }

    @Test
    public void testRoundTripIncompressible() throws Exception
    {
        // random bytes ~ incompressible: each chunk packs ~S worth of input
        roundTrip(fill(new Random(42), 1_500_000, false), 16, 128);
    }

    @Test
    public void testRoundTripCompressible() throws Exception
    {
        // highly compressible: packing fills each block with far more than S of uncompressed input,
        // capped by max_uncompressed_chunk_length
        roundTrip(fill(new Random(7), 3_000_000, true), 16, 128);
    }

    @Test
    public void testRoundTripSmallerThanOneChunk() throws Exception
    {
        roundTrip(fill(new Random(1), 500, false), 16, 128);
    }

    @Test
    public void testLowPaddingOverhead() throws Exception
    {
        // Incompressible data spanning several write buffers. With the write buffer decoupled from the
        // per-chunk cap, flush-boundary short chunks are rare, so on-disk overhead (framing + padding)
        // stays small. A buffer sized to the cap would pad a short chunk at every flush (~12%).
        byte[] data = fill(new Random(5), 8_000_000, false);
        int s = 16 * 1024;
        File f = FileUtils.createTempFile("fixed_output_pad", ".db");
        File metadata = new File(f.absolutePath() + ".metadata");
        CompressionParams params = fixedOutput(16, 128);
        MetadataCollector collector = new MetadataCollector(new ClusteringComparator(BytesType.instance));
        try (CompressedSequentialWriter writer = new CompressedSequentialWriter(f, metadata, null,
                                                                                SequentialWriterOption.DEFAULT,
                                                                                params, collector))
        {
            writer.write(data);
            writer.finish();
        }
        try
        {
            assertEquals(0, f.length() % s); // whole number of S-blocks
            double overhead = (f.length() - data.length) / (double) data.length;
            assertTrue("fixed-output on-disk overhead too high: " + overhead, overhead < 0.05);
        }
        finally
        {
            f.tryDelete();
            metadata.tryDelete();
        }
    }

    @Test
    public void testStreamingCompressBoundedContract() throws Exception
    {
        // Directly exercises the Zstd streaming compressBounded override: bounded output, an exact-sized
        // round-trip through the no-content-size frame (mirroring CompressedChunkReader.uncompressFixedBlock),
        // pack-to-fill for incompressible and compressible input, and input-exhaustion for a tiny prefix.
        ZstdCompressor zstd = ZstdCompressor.getOrCreate(8);
        final int maxOut = 16 * 1024 - 8;   // fixedPayloadCapacity for S = 16 KB
        final int plenty = 512 * 1024;      // far more input than one block holds

        // (A) incompressible: ratio ~1, so ~maxOut of input fills the block; it must STOP at the fill
        //     boundary (consumed < plenty) rather than swallow the whole buffer.
        byte[] inc = fill(new Random(7), plenty, false);
        assertStreamingBounded(zstd, inc, maxOut, /*expectFilled*/ true, /*expectStopBeforeInputEnd*/ true);

        // (B) moderately compressible (~0.45): the FLUSH loop must GROW past maxOut bytes of input to fill
        //     the block, then stop; consumed must exceed the output size (compression happened) and the
        //     block must be well filled.
        byte[] mod = new byte[plenty];
        new Random(11).nextBytes(mod);
        for (int i = 0; i < plenty; i++)
            if ((i % 100) >= 45) mod[i] = 0; // ~55% zeros -> ~0.45 compressible
        int consumedMod = assertStreamingBounded(zstd, mod, maxOut, true, true);
        assertTrue("compressible: should pack more input than output", consumedMod > maxOut);

        // (C) tiny input: all consumed, no fill needed, still a valid exact-dst round-trip.
        byte[] tiny = fill(new Random(3), 100, false);
        int consumedTiny = assertStreamingBounded(zstd, tiny, maxOut, false, false);
        assertEquals("tiny input fully consumed", 100, consumedTiny);
    }

    /** Runs compressBounded on {@code data}, checks the bounded contract + exact-sized round-trip, returns consumed. */
    private static int assertStreamingBounded(ZstdCompressor zstd, byte[] data, int maxOut,
                                              boolean expectFilled, boolean expectStopBeforeInputEnd) throws Exception
    {
        ByteBuffer in = ByteBuffer.allocateDirect(data.length);
        in.put(data);
        in.flip();
        ByteBuffer out = ByteBuffer.allocateDirect(maxOut + 4096); // extra room so overflow would be visible

        int consumed = zstd.compressBounded(in, out, maxOut);
        int produced = out.position();

        assertTrue("must consume input", consumed > 0);
        assertEquals("input.position advanced by consumed", consumed, in.position());
        assertTrue("produced " + produced + " must not exceed maxOut " + maxOut, produced <= maxOut);
        if (expectFilled)
            assertTrue("block under-filled: produced=" + produced + " maxOut=" + maxOut, produced >= maxOut * 0.9);
        if (expectStopBeforeInputEnd)
            assertTrue("should stop at fill boundary, not exhaust input", consumed < data.length);

        // No content-size header on a streaming frame -> decompress into an EXACTLY-sized destination.
        out.flip();
        ByteBuffer dec = ByteBuffer.allocateDirect(consumed);
        zstd.uncompress(out, dec);
        dec.flip();
        assertEquals("decompressed length", consumed, dec.remaining());
        byte[] got = new byte[consumed];
        dec.get(got);
        byte[] expected = new byte[consumed];
        System.arraycopy(data, 0, expected, 0, consumed);
        assertArrayEquals("streaming round-trip mismatch", expected, got);
        return consumed;
    }

    @Test
    public void testValidation()
    {
        // valid: S multiple of 4 KB, cap >= S
        assertTrue(fixedOutput(16, 128).usesFixedOutputChunks());
        assertEquals(16 * 1024, fixedOutput(16, 128).compressedChunkLength());
        assertEquals(128 * 1024, fixedOutput(16, 128).maxUncompressedChunkLength());

        // default cap when unspecified = DEFAULT_MAX_UNCOMPRESSED_MULTIPLE * S
        Map<String, String> noCap = new HashMap<>();
        noCap.put(CompressionParams.CLASS, "ZstdCompressor");
        noCap.put(CompressionParams.COMPRESSED_CHUNK_LENGTH_IN_KB, "16");
        CompressionParams p = CompressionParams.fromMap(noCap);
        assertEquals(16 * 1024 * CompressionParams.DEFAULT_MAX_UNCOMPRESSED_MULTIPLE, p.maxUncompressedChunkLength());

        // invalid: S not a multiple of the 4 KB alignment floor (18 KB)
        assertRejected(m -> m.put(CompressionParams.COMPRESSED_CHUNK_LENGTH_IN_KB, "18"));
        // invalid: cap < S
        assertRejected(m -> {
            m.put(CompressionParams.COMPRESSED_CHUNK_LENGTH_IN_KB, "16");
            m.put(CompressionParams.MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, "8");
        });
        // invalid: cap given without the compressed target
        assertRejected(m -> m.put(CompressionParams.MAX_UNCOMPRESSED_CHUNK_LENGTH_IN_KB, "128"));
        // invalid: S above the upper bound
        assertRejected(m -> m.put(CompressionParams.COMPRESSED_CHUNK_LENGTH_IN_KB,
                                  Integer.toString(CompressionParams.MAX_FIXED_OUTPUT_LENGTH / 1024 + 4)));

        // legacy (no fixed-output option) still parses and reports non-fixed
        Map<String, String> legacy = new HashMap<>();
        legacy.put(CompressionParams.CLASS, "ZstdCompressor");
        legacy.put(CompressionParams.CHUNK_LENGTH_IN_KB, "16");
        assertTrue(!CompressionParams.fromMap(legacy).usesFixedOutputChunks());
    }

    @Test
    public void testMessagingSerializerRoundTrip() throws Exception
    {
        // The messaging serializer carries the fixed-output mode via option markers (Phase 4 streaming),
        // so the mode and its parameters survive a wire round-trip.
        CompressionParams p = fixedOutput(16, 128);
        try (org.apache.cassandra.io.util.DataOutputBuffer out = new org.apache.cassandra.io.util.DataOutputBuffer())
        {
            CompressionParams.serializer.serialize(p, out, org.apache.cassandra.net.MessagingService.current_version);
            assertEquals(out.getLength(), CompressionParams.serializer.serializedSize(p, org.apache.cassandra.net.MessagingService.current_version));
            try (org.apache.cassandra.io.util.DataInputBuffer in = new org.apache.cassandra.io.util.DataInputBuffer(out.buffer(), false))
            {
                CompressionParams back = CompressionParams.serializer.deserialize(in, org.apache.cassandra.net.MessagingService.current_version);
                assertTrue(back.usesFixedOutputChunks());
                assertEquals(p.compressedChunkLength(), back.compressedChunkLength());
                assertEquals(p.maxUncompressedChunkLength(), back.maxUncompressedChunkLength());
            }
        }
    }

    private static void assertRejected(java.util.function.Consumer<Map<String, String>> mutate)
    {
        Map<String, String> opts = new HashMap<>();
        opts.put(CompressionParams.CLASS, "ZstdCompressor");
        mutate.accept(opts);
        try
        {
            CompressionParams.fromMap(opts);
            org.junit.Assert.fail("expected ConfigurationException for options " + opts);
        }
        catch (org.apache.cassandra.exceptions.ConfigurationException expected)
        {
            // ok
        }
    }

    private static byte[] fill(Random r, int n, boolean compressible)
    {
        byte[] data = new byte[n];
        if (compressible)
        {
            // long runs of a few distinct values -> compresses well
            for (int i = 0; i < n; i++)
                data[i] = (byte) ((i / 512) % 4);
        }
        else
        {
            r.nextBytes(data);
        }
        return data;
    }

    private void roundTrip(byte[] data, int compressedKb, int maxUncompressedKb) throws Exception
    {
        int s = compressedKb * 1024;
        File f = FileUtils.createTempFile("fixed_output", ".db");
        File metadata = new File(f.absolutePath() + ".metadata");
        CompressionParams params = fixedOutput(compressedKb, maxUncompressedKb);
        assertTrue(params.usesFixedOutputChunks());

        MetadataCollector collector = new MetadataCollector(new ClusteringComparator(BytesType.instance));
        try (CompressedSequentialWriter writer = new CompressedSequentialWriter(f, metadata, null,
                                                                                SequentialWriterOption.DEFAULT,
                                                                                params, collector))
        {
            writer.write(data);
            writer.finish();
        }

        // Every chunk is exactly S bytes on disk -> the data file length is a multiple of S.
        assertEquals("data file must be a whole number of S-sized blocks", 0, f.length() % s);

        try (CompressionMetadata cm = CompressionMetadata.open(metadata, f.length(), true);
             FileHandle fh = new FileHandle.Builder(f).withCompressionMetadata(cm).complete();
             RandomAccessReader reader = fh.createReader())
        {
            assertTrue(cm.parameters.usesFixedOutputChunks());
            assertEquals(s, cm.parameters.compressedChunkLength());
            assertEquals("compressed file length must equal chunkCount * S", 0, cm.compressedFileLength % s);
            assertEquals(data.length, cm.dataLength);

            // sequential read
            byte[] readBack = new byte[data.length];
            reader.readFully(readBack);
            assertArrayEquals("sequential round-trip mismatch", data, readBack);

            // random access at chunk-boundary-crossing offsets
            Random rnd = new Random(99);
            for (int i = 0; i < 200 && data.length > 0; i++)
            {
                long pos = (long) (rnd.nextDouble() * data.length);
                if (pos >= data.length)
                    pos = data.length - 1;
                reader.seek(pos);
                assertEquals("random-access mismatch at " + pos, data[(int) pos] & 0xff, reader.readByte() & 0xff);
            }
        }
        finally
        {
            f.tryDelete();
            metadata.tryDelete();
        }
    }
}
