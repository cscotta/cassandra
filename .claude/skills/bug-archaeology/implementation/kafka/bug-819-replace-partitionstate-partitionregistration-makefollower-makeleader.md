**Commit**: cae9848160 · **Ticket**: KAFKA-19447
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Follow-up to
[KAFKA-18486](https://issues.apache.org/jira/browse/KAFKA-18486)
* Replace PartitionState with PartitionRegistration in
makeFollower/makeLeader
* Remove PartitionState.java since it is no longer referenced

Reviewers: TaiJuWu <tjwu1217@gmail.com>, Ken Huang <s7133700@gmail.com>,
 Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.{PartitionState => JPartitionState}
import org.apache.kafka.metadata.{LeaderAndIsr, LeaderRecoveryState, MetadataCache}
import org.apache.kafka.storage.internals.log.{AppendOrigin, AsyncOffsetReader, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, UnifiedLog, VerificationGuard}
  def makeLeader(partitionState: JPartitionState,
      // before a LeaderAndIsr request is received or before an update is received via
      if (partitionState.partitionEpoch < partitionEpoch) {
        stateChangeLogger.info(s"Skipped the become-leader state change for $topicPartition with topic id $topicId " +
          s"and partition state $partitionState since the leader is already at a newer partition epoch $partitionEpoch.")
      val isNewLeaderEpoch = partitionState.leaderEpoch > leaderEpoch
      val replicas = partitionState.replicas.asScala.map(_.toInt)
      val isr = partitionState.isr.asScala.map(_.toInt).toSet
      val addingReplicas = partitionState.addingReplicas.asScala.map(_.toInt)

// After
import org.apache.kafka.metadata.{LeaderAndIsr, LeaderRecoveryState, MetadataCache, PartitionRegistration}
import org.apache.kafka.storage.internals.log.{AppendOrigin, AsyncOffsetReader, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetsListener, LogOffsetSnapshot, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, UnifiedLog, VerificationGuard}
  def makeLeader(partitionRegistration: PartitionRegistration,
                 isNew: Boolean,
      // before a partitionRegistration is received or before an update is received via
      if (partitionRegistration.partitionEpoch < partitionEpoch) {
        stateChangeLogger.info(s"Skipped the become-leader state change for $topicPartition with topic id $topicId, " +
          s"partition registration $partitionRegistration and isNew=$isNew since the leader is already at a newer partition epoch $partitionEpoch.")
      val isNewLeaderEpoch = partitionRegistration.leaderEpoch > leaderEpoch
      val replicas = partitionRegistration.replicas
      val isr = partitionRegistration.isr.toSet
      val addingReplicas = partitionRegistration.addingReplicas
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19447 replace partitionstate with partitionregistration in makefollower/makeleader (#20335).

**Test**
Test modified in AbstractPartitionTest.scala covering the corrected behaviour.
