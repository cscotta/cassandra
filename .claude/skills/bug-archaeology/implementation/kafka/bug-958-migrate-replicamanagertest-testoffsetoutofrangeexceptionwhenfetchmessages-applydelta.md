**Commit**: 991a10c19f · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Change `becomeLeaderOrFollower` to `applyDelta` in following test cases
* testOffsetOutOfRangeExceptionWhenFetchMessages
* testOffsetOutOfRangeExceptionWhenReadFromLog
* testOldFollowerLosesMetricsWhenReassignPartitions
* testOldLeaderLosesMetricsWhenReassignPartitions

Reviewers: Bolin Lin <linbolin1230@gmail.com>, Lan Ding
 <isDing_L@163.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    val controllerEpoch = 0
    val correlationId = 0
    val controllerId = 0
      val leaderAndIsrRequest1 = new LeaderAndIsrRequest.Builder(controllerId, 0, brokerEpoch,
        Seq(
          new LeaderAndIsrRequest.PartitionState()
            .setTopicName(tp0.topic)
            .setPartitionIndex(tp0.partition)
            .setControllerEpoch(controllerEpoch)
            .setLeader(0)
            .setLeaderEpoch(leaderEpoch)
            .setIsr(partition0Replicas)

// After
      val delta1 = createLeaderDelta(topicIds.get(topic), tp0, partition0Replicas.get(0), partition0Replicas, partition0Replicas, leaderEpoch)
      delta1.replay(new PartitionRecord()
        .setPartitionId(tp1.partition)
        .setTopicId(topicIds.get(topic))
        .setIsr(partition1Replicas)
        .setReplicas(partition1Replicas)
        .setRemovingReplicas(util.List.of())
        .setAddingReplicas(util.List.of())
        .setLeader(partition1Replicas.get(0))
        .setLeaderEpoch(leaderEpoch)
        .setPartitionEpoch(0)
      )
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 migrate replicamanagertest#testoffsetoutofrangeexceptionwhenfetchmessages to use applydelta (#19952).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
