**Commit**: dfab5999f6 · **Ticket**: KAFKA-19752
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
From `testReadWithMinMessage` to
`testFetchLatestTieredTimestampWithRemoteStorage`

Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    testImplementation libs.jqwik
      includeEngines 'jqwik', 'junit-jupiter'
              files="(UnifiedLog|RemoteLogManager|RemoteLogManagerTest).java"/>
import org.apache.kafka.common.{InvalidRecordException, TopicPartition, Uuid}
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, AsyncOffsetReader, Cleaner, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogSegment, LogSegments, LogStartOffsetIncrementReason, LogToClean, OffsetResultHolder, OffsetsOutOfOrderException, ProducerStateManager, ProducerStateManagerConfig, RecordValidationException, UnifiedLog, VerificationGuard}
import org.apache.kafka.storage.log.metrics.{BrokerTopicMetrics, BrokerTopicStats}
import org.junit.jupiter.params.provider.ArgumentsSource
import net.jqwik.api.AfterFailureMode
import net.jqwik.api.ForAll
import net.jqwik.api.Property
  @Test
  def testReadWithMinMessage(): Unit = {

// After
      includeEngines 'junit-jupiter'
              files="(UnifiedLog|RemoteLogManager|RemoteLogManagerTest|UnifiedLogTest).java"/>
import org.apache.kafka.common.{TopicPartition, Uuid}
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, AsyncOffsetReader, Cleaner, LogConfig, LogFileUtils, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogSegment, LogSegments, LogStartOffsetIncrementReason, LogToClean, OffsetResultHolder, OffsetsOutOfOrderException, ProducerStateManager, ProducerStateManagerConfig, UnifiedLog, VerificationGuard}
import org.apache.kafka.storage.log.metrics.BrokerTopicStats
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19752 move parts of unifiedlogtest to storage module (#21686).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
