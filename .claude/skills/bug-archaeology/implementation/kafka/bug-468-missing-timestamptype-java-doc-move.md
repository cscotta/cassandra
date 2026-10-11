**Commit**: 0166a0342d · **Ticket**: KAFKA-20128
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
- Add TimestampType to the java doc, as it is a public API introduced as
part of KIP-32 (for ConsumerRecord) and KIP-65 (for SinkRecord). - Move all non-public classes to an "internal" subpackage

Reviewers: Andrew Schofield <aschofield@confluent.io>, PoAn Yang
 <payang@apache.org>, Maros Orsak <maros.orsak159@gmail.com>, Chia-Ping
 Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      <allow class="org.apache.kafka.common.record.CompressionType" />
      <allow class="org.apache.kafka.common.record.CompressionType.GZIP" />
      <allow class="org.apache.kafka.common.record.CompressionType.LZ4" />
      <allow class="org.apache.kafka.common.record.CompressionType.ZSTD" />
      <allow class="org.apache.kafka.common.record.RecordBatch" exact-match="true" />
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.AbstractRecords;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;

// After
    include "**/org/apache/kafka/common/record/*"
      <allow class="org.apache.kafka.common.record.internal.CompressionType" />
      <allow class="org.apache.kafka.common.record.internal.CompressionType.GZIP" />
      <allow class="org.apache.kafka.common.record.internal.CompressionType.LZ4" />
      <allow class="org.apache.kafka.common.record.internal.CompressionType.ZSTD" />
      <allow class="org.apache.kafka.common.record.internal.RecordBatch" exact-match="true" />
import org.apache.kafka.common.record.internal.MemoryRecords;
import org.apache.kafka.common.record.internal.SimpleRecord;
import org.apache.kafka.common.record.internal.CompressionType;
import org.apache.kafka.common.record.internal.AbstractRecords;
import org.apache.kafka.common.record.internal.CompressionType;
import org.a
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20128 add missing timestamptype to java doc and move non-public classes to internal subpackage  (#21412).

**Test**
Test modified in DeleteTopicTest.java covering the corrected behaviour.
