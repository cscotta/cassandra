**Commit**: 84b8fec089 · **Ticket**: KAFKA-14486
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Move LogCleanerManager and related classes to storage module and rewrite
in Java. Reviewers: TengYao Chi <kitingiao@gmail.com>, Jun Rao
<junrao@gmail.com>, Mickael Maison <mickael.maison@gmail.com>, Chia-Ping
Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.Date
import kafka.utils.{Logging, Pool}
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogCleaningAbortedException, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, OffsetMap, SkimpyOffsetMap, ThreadShutdownException, TransactionIndex, UnifiedLog}
 * @param logs The pool of logs
                 val logs: Pool[TopicPartition, UnifiedLog],
  private[log] val cleanerManager = new LogCleanerManager(logDirs, logs, logDirFailureChannel)
    cleanerManager.updateCheckpoints(dataDir, partitionToRemove = partitionToRemove)
    cleanerManager.resumeCleaning(topicPartitions)
    def isCleaned = cleanerManager.allCleanerCheckpoints.get(topicPartition).fold(false)(_ >= offset)

// After
        <allow pkg="kafka.utils"/>
import java.util.{Date, Optional}
import kafka.utils.Logging
import org.apache.kafka.storage.internals.log.{AbortedTxn, CleanerConfig, LastRecord, LogCleanerManager, LogCleaningAbortedException, LogCleaningException, LogDirFailureChannel, LogSegment, LogSegmentOffsetOverflowException, LogToClean, OffsetMap, PreCleanStats, SkimpyOffsetMap, ThreadShutdownException, TransactionIndex, UnifiedLog}
import scala.jdk.OptionConverters.{RichOption, RichOptional}
 * @param logs The map of logs
                 val logs: util.concurrent.ConcurrentMap[TopicPartition, UnifiedLog],
  private[log] val cleanerManager = new LogCleanerManager(logDirs.asJava, logs, logDirFailureChannel)
    cleanerManager.updateCheckpoints(dataDir, Optional.empty(), partitionToRemove.toJava)
    cleanerManager.resumeCleaning(topicPartitions.toList.asJava)
    def isCleaned = Option(cleanerManager.allCleanerCheckpoints.get(topicPartition)).fold(false)(_ >= offset)
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14486 move logcleanermanager to storage module (#19216).

**Test**
Test modified in AbstractLogCleanerIntegrationTest.scala covering the corrected behaviour.
