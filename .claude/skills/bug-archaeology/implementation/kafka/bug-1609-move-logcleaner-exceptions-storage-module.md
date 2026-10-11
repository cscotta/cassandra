**Commit**: 6eb44ad869 · **Ticket**: KAFKA-14485
**Subsystem**: core/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
import kafka.common._
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, OffsetMap, SkimpyOffsetMap, TransactionIndex}
import kafka.common.LogCleaningAbortedException
import org.apache.kafka.storage.internals.log.LogDirFailureChannel
import kafka.common._
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, CleanerConfig, LocalLog, LogAppendInfo, LogConfig, LogDirFailureChannel, LogFileUtils, LogLoader, LogSegment, LogSegments, LogStartOffsetIncrementReason, OffsetMap, ProducerStateManager, ProducerStateManagerConfig}
    val disctinctValuesBySegmentBeforeClean = distinctValuesBySegment
    assertTrue(disctinctValuesBySegmentBeforeClean.zip(distinctValuesBySegmentAfterClean)
    assertTrue(disctinctValuesBySegmentBeforeClean.zip(distinctValuesBySegmentAfterClean)
    def createRecorcs =

// After
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogCleaningAbortedException, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, OffsetMap, SkimpyOffsetMap, ThreadShutdownException, TransactionIndex}
import org.apache.kafka.storage.internals.log.{LogCleaningAbortedException, LogDirFailureChannel}
import org.apache.kafka.storage.internals.log.{AbortedTxn, AppendOrigin, CleanerConfig, LocalLog, LogAppendInfo, LogCleaningAbortedException, LogConfig, LogDirFailureChannel, LogFileUtils, LogLoader, LogSegment, LogSegments, LogStartOffsetIncrementReason, OffsetMap, ProducerStateManager, ProducerStateManagerConfig}
    val distinctValuesBySegmentBeforeClean = distinctValuesBySegment
    assertTrue(distinctValuesBySegmentBeforeClean.zip(distinctValuesBySegmentAfterClean)
    assertTrue(distinctValuesBySegmentBeforeClean.zip(distinctValuesBySegmentAfterClean)
```

**How to spot**
When reviewing core/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14485: move logcleaner exceptions to storage module (#18534).

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
