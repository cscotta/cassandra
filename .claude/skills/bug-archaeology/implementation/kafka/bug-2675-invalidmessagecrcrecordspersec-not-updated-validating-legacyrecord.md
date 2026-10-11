**Commit**: 4fa1c21940 · **Ticket**: KAFKA-17104
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * Throw an InvalidRecordException if isValid is false for this record
import org.apache.kafka.common.errors.{InvalidTimestampException, UnsupportedCompressionTypeException, UnsupportedForMessageFormatException}

// After
     * Throw an CorruptRecordException if isValid is false for this record
import org.apache.kafka.common.errors.{CorruptRecordException, InvalidTimestampException, UnsupportedCompressionTypeException, UnsupportedForMessageFormatException}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
  @ParameterizedTest
  @CsvSource(Array("0,gzip", "1,gzip", "0,lz4", "1,lz4", "0,snappy", "1,snappy"))
  def testInvalidChecksum(code: Byte, compression: String): Unit = {
    checkInvalidChecksum(code, Compression.of(compression).build(), CompressionType.forName(compression))
  }

  private def checkInvalidChecksum(magic: Byte, compression: Compression , compressionType: CompressionType): Unit = {
    val record: LegacyRecord = LegacyRecord.create(magic, 0L, null, "hello".getBytes)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17104 invalidmessagecrcrecordspersec is not updated in validating legacyrecord (#16558).

**Test**
Test modified in LogValidatorTest.scala covering the corrected behaviour.
