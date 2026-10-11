**Commit**: 07c87a8322 · **Ticket**: KAFKA-19752
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
testMaybeUpdateHighWatermarkAsFollower ~
testNonZeroSequenceOnFirstAppendNonZeroEpoch

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.{TopicPartition, Uuid}
import org.apache.kafka.common.internals.Topic
import org.apache.kafka.server.log.remote.metadata.storage.TopicBasedRemoteLogMetadataManagerConfig

import org.apache.kafka.storage.internals.log.{AppendOrigin, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetsListener, LogSegment, LogSegments, LogStartOffsetIncrementReason, OffsetResultHolder, ProducerStateManagerConfig, UnifiedLog, VerificationGuard}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{EnumSource, ValueSource}
  @Test
  def testMaybeUpdateHighWatermarkAsFollower(): Unit = {
    val logConfig = LogTestUtils.createLogConfig()
    val log = createLog(logDir, logConfig)

// After
import org.apache.kafka.common.Uuid
import org.apache.kafka.storage.internals.log.{AppendOrigin, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetsListener, LogSegment, LogSegments, OffsetResultHolder, ProducerStateManagerConfig, UnifiedLog, VerificationGuard}
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19752 move parts of unifiedlogtest to storage module (#21807).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
