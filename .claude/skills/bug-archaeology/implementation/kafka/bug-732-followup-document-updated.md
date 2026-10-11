**Commit**: 41611b4bd2 · **Ticket**: KAFKA-19112
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Some sections are not very clear, and we need to update the
documentation. Reviewers: TengYao Chi <kitingiao@gmail.com>, Jun Rao
 <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (!leaderLog.config.delete)
import org.apache.kafka.common.errors.{ApiException, FencedLeaderEpochException, InconsistentTopicIdException, InvalidTxnStateException, NotLeaderOrFollowerException, OffsetNotAvailableException, OffsetOutOfRangeException, UnknownLeaderEpochException}
import org.apache.kafka.storage.internals.log.{AppendOrigin, CleanerConfig, EpochEntry, LocalLog, LogAppendInfo, LogDirFailureChannel, LogLoader, LogOffsetMetadata, LogOffsetsListener, LogReadInfo, LogSegments, LogStartOffsetIncrementReason, ProducerStateManager, ProducerStateManagerConfig, UnifiedLog, VerificationGuard}
                The <code>cleanup.policy</code> is empty and <code>remote.storage.enable</code> is set to true, the

// After
        if (!leaderLog.config.delete && leaderLog.config.compact)
import org.apache.kafka.common.errors.{ApiException, FencedLeaderEpochException, InconsistentTopicIdException, InvalidTxnStateException, NotLeaderOrFollowerException, OffsetNotAvailableException, OffsetOutOfRangeException, PolicyViolationException, UnknownLeaderEpochException}
import org.apache.kafka.storage.internals.log.{AppendOrigin, CleanerConfig, EpochEntry, LocalLog, LogAppendInfo, LogConfig, LogDirFailureChannel, LogLoader, LogOffsetMetadata, LogOffsetsListener, LogReadInfo, LogSegments, LogStartOffsetIncrementReason, ProducerStateManager, ProducerStateManagerConfig, UnifiedLog, VerificationGuard}

  @Test
  def testDeleteRecordsOnLeaderWithEmptyPolicy(): Unit = {
    val leaderEpoch = 5
    val partition = setupPartitionWithMocks(leaderEpoch, isLeader = true)

    val emptyPolicyConfig = new LogConfig(util.Map.of(
      TopicConfig.CLEANUP_POLICY_CONFIG, ""
    ))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: followup kafka-19112 document updated (#20492).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
