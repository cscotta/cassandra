import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.io.sstable.CQLSSTableWriter;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Offline generator of token-disjoint SSTables for the io_uring read benchmark.
 *
 * Emits N = T*P partitions with keys "&lt;ID&gt;.&lt;t&gt;.&lt;i&gt;" (t in [0,T), i in [0,P)) matching the exact
 * keys cassandra-easy-stress's IoUringReadBench queries with T client threads. Each value is a unique
 * high-entropy random head plus a shared-dictionary tail, tuned so the Zstd@8 on-disk ratio ~= TARGET_RATIO
 * (~0.45, matching production) rather than the old fully-incompressible profile.
 *
 * Keys are sorted by Murmur3 token (packed: high token bits + partition index), split into WRITERS contiguous
 * token chunks, and each chunk written in token order by its own CQLSSTableWriter -> globally token-disjoint
 * SSTables, so sstableofflinerelevel can build a valid LCS hierarchy without a compaction rewrite.
 */
public final class SSTableGen
{
    static final String ID = System.getProperty("gen.id", "001");
    static final int T = Integer.getInteger("gen.t", 16);                 // must equal --threads at read time
    static final int P = Integer.getInteger("gen.p", 4_000_000);          // must equal --partitions (-p) at read time
    static final long N = (long) T * P;      // 64,000,000 partitions ~= 200 GB logical at ~3.1 KB/value
    static final int VALUE_SIZE = 3100;      // ~3.1 KB/value; N stays <= 2^26 (INDEX_MASK)
    // Compressible value profile: each value is a unique high-entropy random "head" (IDs/blobs) plus a
    // "tail" copied from a shared dictionary that repeats across rows (the redundant structure real
    // records share). Within a compression chunk the unique heads stay ~incompressible and the identical
    // tails collapse, so the Zstd@8 chunk ratio ~= TARGET_RATIO. Verify with `nodetool tablestats` and
    // nudge TARGET_RATIO if needed. (Set to 1.0 for the old fully-incompressible profile.)
    static final double TARGET_RATIO = 0.42;
    static final int VALUE_RANDOM_HEAD = (int) (VALUE_SIZE * TARGET_RATIO);   // ~1395 unique random bytes
    static final byte[] VALUE_DICT = new byte[8 * 1024];                      // shared "vocabulary", generated once
    static { ThreadLocalRandom.current().nextBytes(VALUE_DICT); }
    static final int SSTABLE_MIB = 160;
    static final int WRITERS = Integer.getInteger("gen.writers", 10);     // all cores
    static final String KS = "iouring_bench";
    static final String TBL = "readbench";
    static final String OUT = System.getProperty("gen.out", "/home/cscotta/projects/cassandra/io_uring_bench/staging");
    static final long INDEX_MASK = 0x3FFFFFFL;                 // low 26 bits (N = 2^26)
    static final long TOKEN_MASK = ~INDEX_MASK;                // high 38 bits of the token

    static final String SCHEMA =
        "CREATE TABLE " + KS + '.' + TBL + " (key text PRIMARY KEY, value blob) " +
        "WITH compression = {'class':'ZstdCompressor','compression_level':'8','chunk_length_in_kb':'64'} " +
        "AND compaction = {'class':'LeveledCompactionStrategy','sstable_size_in_mb':'160'}";
    static final String INSERT = "INSERT INTO " + KS + '.' + TBL + " (key, value) VALUES (?, ?)";

    static String key(long j)
    {
        return ID + '.' + (j / P) + '.' + (j % P);
    }

    public static void main(String[] args) throws Exception
    {
        DatabaseDescriptor.clientInitialization(false);
        Murmur3Partitioner part = Murmur3Partitioner.instance;

        System.out.println("Computing " + N + " Murmur3 tokens...");
        long[] packed = new long[(int) N];
        IntStream.range(0, (int) N).parallel().forEach(j -> {
            long token = ((Murmur3Partitioner.LongToken) part.getToken(ByteBufferUtil.bytes(key(j)))).getLongValue();
            packed[j] = (token & TOKEN_MASK) | (j & INDEX_MASK);
        });

        System.out.println("Sorting by token...");
        Arrays.parallelSort(packed);

        System.out.println("Writing " + WRITERS + " token-disjoint shards to " + OUT + " ...");
        AtomicLong written = new AtomicLong();
        long startNanos = System.nanoTime();
        int per = (int) (N / WRITERS);
        Thread[] threads = new Thread[WRITERS];
        for (int w = 0; w < WRITERS; w++)
        {
            final int lo = w * per;
            final int hi = (w == WRITERS - 1) ? (int) N : (w + 1) * per;
            final int wid = w;
            threads[w] = new Thread(() -> {
                File dir = new File(OUT, "shard" + wid);
                dir.mkdirs();
                CQLSSTableWriter writer = CQLSSTableWriter.builder()
                                                          .inDirectory(dir.getPath())
                                                          .forTable(SCHEMA)
                                                          .using(INSERT)
                                                          .withPartitioner(part)
                                                          .withMaxSSTableSizeInMiB(SSTABLE_MIB)
                                                          .build();
                try
                {
                    for (int k = lo; k < hi; k++)
                    {
                        long j = packed[k] & INDEX_MASK;
                        byte[] val = new byte[VALUE_SIZE];   // unique random head + shared dictionary tail
                        ThreadLocalRandom.current().nextBytes(val);
                        System.arraycopy(VALUE_DICT, 0, val, VALUE_RANDOM_HEAD, VALUE_SIZE - VALUE_RANDOM_HEAD);
                        writer.addRow(key(j), ByteBuffer.wrap(val));
                        long c = written.incrementAndGet();
                        if (c % 5_000_000 == 0)
                        {
                            double secs = (System.nanoTime() - startNanos) / 1e9;
                            System.out.printf("  %,d / %,d  (%.0f rows/s)%n", c, N, c / secs);
                        }
                    }
                    writer.close();
                }
                catch (Exception e)
                {
                    throw new RuntimeException("shard " + wid + " failed", e);
                }
            }, "gen-shard-" + w);
            threads[w].start();
        }
        for (Thread th : threads)
            th.join();

        double secs = (System.nanoTime() - startNanos) / 1e9;
        System.out.printf("Done: %,d rows across %d shards in %.0fs -> %s%n", written.get(), WRITERS, secs, OUT);
    }
}
