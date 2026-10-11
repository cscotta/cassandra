**Commit**: 6eddaeba58 · **Ticket**: KAFKA-18532
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import kafka.zookeeper.ZooKeeperClientException
      try {
        createLogInAssignedDirectoryId(partitionState, highWatermarkCheckpoints, topicId, targetDirectoryId)
      } catch {
        case e: ZooKeeperClientException =>
          stateChangeLogger.error(s"A ZooKeeper client exception has occurred and makeLeader will be skipping the " +
            s"state change for the partition $topicPartition with leader epoch: $leaderEpoch.", e)
          return false
      }
      try {
        createLogInAssignedDirectoryId(partitionState, highWatermarkCheckpoints, topicId, targetLogDirectoryId)
      } catch {

// After
      createLogInAssignedDirectoryId(partitionState, highWatermarkCheckpoints, topicId, targetDirectoryId)
      createLogInAssignedDirectoryId(partitionState, highWatermarkCheckpoints, topicId, targetLogDirectoryId)
          applyLocalLeadersDelta(leaderChangedPartitions, delta, lazyOffsetCheckpoints, localChanges.leaders.asScala, localChanges.directoryIds.asScala)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18532: clean partition.scala zookeeper logic (#18594).

**Test**
No test added.
