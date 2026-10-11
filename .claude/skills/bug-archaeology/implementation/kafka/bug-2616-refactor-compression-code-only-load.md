**Commit**: 7c5d339d07 · **Ticket**: KAFKA-17227
**Subsystem**: checkstyle/import-control.xml
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
      <allow class="org.apache.kafka.common.record.CompressionType" exact-match="true" />
import org.apache.kafka.common.compress.GzipCompression;
import org.apache.kafka.common.compress.Lz4Compression;
import org.apache.kafka.common.compress.ZstdCompression;
                                .define(COMPRESSION_GZIP_LEVEL_CONFIG, Type.INT, GzipCompression.DEFAULT_LEVEL, new GzipCompression.LevelValidator(), Importance.MEDIUM, COMPRESSION_GZIP_LEVEL_DOC)
                                .define(COMPRESSION_LZ4_LEVEL_CONFIG, Type.INT, Lz4Compression.DEFAULT_LEVEL, between(Lz4Compression.MIN_LEVEL, Lz4Compression.MAX_LEVEL), Importance.MEDIUM, COMPRESSION_LZ4_LEVEL_DOC)
                                .define(COMPRESSION_ZSTD_LEVEL_CONFIG, Type.INT, ZstdCompression.DEFAULT_LEVEL, between(ZstdCompression.MIN_LEVEL, ZstdCompression.MAX_LEVEL), Importance.MEDIUM, COMPRESSION_ZSTD_LEVEL_DOC)
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import java.util.zip.Deflater;
public class GzipCompression implements Compression {
    public static final int MIN_LEVEL = Deflater.BEST_SPEED;

// After
      <allow class="org.apache.kafka.common.record.CompressionType" />
      <allow class="org.apache.kafka.common.record.CompressionType.GZIP" />
      <allow class="org.apache.kafka.common.record.CompressionType.LZ4" />
      <allow class="org.apache.kafka.common.record.CompressionType.ZSTD" />
      <allow class="org.apache.kafka.common.config.ConfigDef.Range.between" exact-match="true" />
                                .define(COMPRESSION_GZIP_LEVEL_CONFIG, Type.INT, CompressionType.GZIP.defaultLevel(), CompressionType.GZIP.levelValidator(), Importance.MEDIUM, COMPRESSION_GZIP_LEVEL_DOC)
                                .define(COMPRESSION_LZ4_LEVEL_CONFIG, Type.INT, CompressionType.LZ4.defaultLevel(), CompressionType.LZ4.levelValidator(), Importance.MEDIUM, COMPRESSION_LZ4_LEVEL_DOC)
                                .define(COMPRESSION_ZSTD_LEVEL_CONFIG, Type.INT, CompressionType.ZSTD.defaultLevel(), CompressionType.ZSTD.levelValidator(), Importance.MEDIUM, COMPRESSION_ZSTD_LEVEL_DOC)
import static org.apache.kafka.common.record.CompressionType.GZIP;
public class GzipCompression implements Compression {
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17227: refactor compression code to only load codecs when used (#16782).

**Test**
Test modified in GzipCompressionTest.java covering the corrected behaviour.
