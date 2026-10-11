**Commit**: 06a2515109 · **Ticket**: KAFKA-19752
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
UnifiedLogTest is ~4000 lines, let's convert it in smaller chunks. Here
is roughly the first 1000 lines. Reviewers: Christo Lolov <lolovc@amazon.com>, Kuan-Po Tseng
<brandboat@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.record.internal.MemoryRecords.RecordFilter
import org.apache.kafka.common.utils.{BufferSupplier, Time, Utils}
import org.apache.kafka.server.util.{KafkaScheduler, MockTime, Scheduler}
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, AsyncOffsetReader, Cleaner, EpochEntry, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogSegment, LogSegments, LogStartOffsetIncrementReason, LogToClean, OffsetResultHolder, OffsetsOutOfOrderException, ProducerStateManager, ProducerStateManagerConfig, RecordValidationException, UnifiedLog, VerificationGuard}
import scala.collection.immutable.SortedSet
  @Test
  def testHighWatermarkMetadataUpdatedAfterSegmentRoll(): Unit = {
    val logConfig = LogTestUtils.createLogConfig(segmentBytes = 1024 * 1024)
    val log = createLog(logDir, logConfig)

    def assertFetchSizeAndOffsets(fetchOffset: Long,
                                  expectedSize: Int,

// After
import org.apache.kafka.common.utils.{Time, Utils}
import org.apache.kafka.server.util.{MockTime, Scheduler}
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, AsyncOffsetReader, Cleaner, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogSegment, LogSegments, LogStartOffsetIncrementReason, LogToClean, OffsetResultHolder, OffsetsOutOfOrderException, ProducerStateManager, ProducerStateManagerConfig, RecordValidationException, UnifiedLog, VerificationGuard}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19752 move parts of unifiedlogtest to storage module (#21644).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
