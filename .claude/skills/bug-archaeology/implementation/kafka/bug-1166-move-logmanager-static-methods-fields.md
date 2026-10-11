**Commit**: 7710d1c951 · **Ticket**: KAFKA-14487
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Move the static fields/methods

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
import kafka.server.metadata.BrokerMetadataPublisher.info
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogCleaner, LogConfig, LogDirFailureChannel, LogOffsetsListener, ProducerStateManagerConfig, RemoteIndexCache, UnifiedLog}
  import LogManager._

    (dir, new OffsetCheckpointFile(new File(dir, RecoveryPointCheckpointFile), logDirFailureChannel))).toMap
    (dir, new OffsetCheckpointFile(new File(dir, LogStartOffsetCheckpointFile), logDirFailureChannel))).toMap
        val lock = new FileLock(new File(dir, LockFileName))
        if (waitForAllToComplete(dirJobs,
  val LockFileName = ".lock"

  /**
   * Wait all jobs to complete

// After
    implementation project(':metadata')
        <allow pkg="org.apache.kafka.image" />
        <allow pkg="org.apache.kafka.metadata" />
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogCleaner, LogConfig, LogDirFailureChannel, LogManager => JLogManager, LogOffsetsListener, ProducerStateManagerConfig, RemoteIndexCache, UnifiedLog}
    (dir, new OffsetCheckpointFile(new File(dir, JLogManager.RECOVERY_POINT_CHECKPOINT_FILE), logDirFailureChannel))).toMap
    (dir, new OffsetCheckpointFile(new File(dir, JLogManager.LOG_START_OFFSET_CHECKPOINT_FILE), logDirFailureChannel))).toMap
        val lock = new FileLock(new File(dir, JLogManager.LOCK_FILE_NAME))
        if (JLogManager.waitForAllToComplete(dirJobs.toList.asJava,
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14487: move logmanager static methods/fields to storage module (#19302).

**Test**
Test modified in LogManagerTest.scala covering the corrected behaviour.
