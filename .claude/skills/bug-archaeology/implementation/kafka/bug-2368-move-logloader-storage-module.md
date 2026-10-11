**Commit**: f1c011a8b5 · **Ticket**: KAFKA-14482
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              files="(LogValidator|RemoteLogManagerConfig|RemoteLogManager).java"/>
              files="(LogValidator|RemoteLogManager|RemoteIndexCache).java"/>
              files="(LogAppendInfo|RemoteLogManagerConfig).java"/>
import org.apache.kafka.storage.internals.log.{AbortedTxn, FetchDataInfo, LogConfig, LogDirFailureChannel, LogFileUtils, LogOffsetMetadata, LogSegment, LogSegments, OffsetPosition}
import java.io.{File, IOException}
import scala.collection.{Seq, immutable}
/**
 * Holds the result of splitting a segment into one or more segments, see LocalLog.splitOverflowedSegment().
 *
 * @param deletedSegments segments deleted when splitting a segment
 * @param newSegments new segments created when splitting a segment
 */

// After
              files="(LogLoader|LogValidator|RemoteLogManagerConfig|RemoteLogManager).java"/>
              files="(LogLoader|LogValidator|RemoteLogManager|RemoteIndexCache).java"/>
              files="(LogAppendInfo|LogLoader|RemoteLogManagerConfig).java"/>
import org.apache.kafka.storage.internals.log.{AbortedTxn, FetchDataInfo, LogConfig, LogDirFailureChannel, LogFileUtils, LogOffsetMetadata, LogSegment, LogSegments, OffsetPosition, LocalLog => JLocalLog}
import java.io.File
import java.util.Collections.singletonList
import scala.collection.Seq
    JLocalLog.maybeHandleIOException(logDirFailureChannel, parentDir, () => msg, () => fun)
  private[log] def deleteAllSegments(): util.List[LogSegment] = {
      val deletableSegments = new util.ArrayList(segments.values)
      JLocalLog.deleteSegmentFiles(toDelete.asJava, asyncDelete, dir, topicPartition, config, scheduler, logDirFailureChannel, logIdent)
    JLocalLog.deleteSegmentFiles(singletonList(segmentToDelete), asyncDelete, dir, topicPartition, config, scheduler, logDirFailureChannel, logIdent)
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14482 move logloader to storage module (#17042).

**Test**
Test modified in PartitionLockTest.scala covering the corrected behaviour.
