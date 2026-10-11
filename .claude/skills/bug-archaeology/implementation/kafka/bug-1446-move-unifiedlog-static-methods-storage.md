**Commit**: ece91e9247 · **Ticket**: KAFKA-14484
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.log.UnifiedLog
import org.apache.kafka.storage.internals.log.LogOffsetMetadata
    logStartOffset = UnifiedLog.UnknownOffset,
          logStartOffset = UnifiedLog.UnknownOffset,
          lastFetchLeaderLogEndOffset = UnifiedLog.UnknownOffset,
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogCleaningAbortedException, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, OffsetMap, SkimpyOffsetMap, ThreadShutdownException, TransactionIndex}
    val cleaned = UnifiedLog.createNewCleanedSegment(log.dir, log.config, segments.head.baseOffset)
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogConfig, LogDirFailureChannel, ProducerStateManagerConfig, RemoteIndexCache}
    val topicPartition = UnifiedLog.parseTopicPartitionName(logDir)

// After
        <allow pkg="org.apache.kafka.storage.log.metrics"/>
import org.apache.kafka.storage.internals.log.{LogOffsetMetadata, UnifiedLog}
    logStartOffset = UnifiedLog.UNKNOWN_OFFSET,
          logStartOffset = UnifiedLog.UNKNOWN_OFFSET,
          lastFetchLeaderLogEndOffset = UnifiedLog.UNKNOWN_OFFSET,
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogCleaningAbortedException, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, OffsetMap, SkimpyOffsetMap, ThreadShutdownException, TransactionIndex, UnifiedLog => JUnifiedLog}
    val cleaned = JUnifiedLog.createNewCleanedSegment(log.dir, log.config, segments.head.baseOffset)
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogConfig, LogDirFailureChannel, ProducerStateManagerConfig, UnifiedLog => JUnifiedLog, RemoteIndexCache}
    val topicPartition = JUnifiedLog.parseTopicPartitionName(logDir)
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14484: move unifiedlog static methods to storage (#18039).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
