**Commit**: b38573fcaa · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Replace `leaderAndIsrRequest` and `becomeLeaderOrFollower` with
`TopicsDelta`, `MetadataImage` and `ReplicaManager#applyDelta` for the
following tests:
* testPartitionListener
* testPartitionMarkedOfflineIfLogCantBeCreated
* testPartitionMetadataFileNotCreated
* testPartitionsWithLateTransactionsCount
* testPreferredReplicaAsFollower
* testPreferredReplicaAsLeader
* testPreferredReplicaAsLeaderWhenSameRackFollowerIsOutOfIsr
* testProducerIdCountMetrics

Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
      val partition0 = replicaManager.createPartition(new TopicPartition(topic, 0))
      val partition1 = replicaManager.createPartition(new TopicPartition(topic, 1))
      Seq(0, 1).foreach { partition =>
        val leaderAndIsrRequest = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
          Seq(new LeaderAndIsrRequest.PartitionState()
            .setTopicName(topic)
            .setPartitionIndex(partition)
            .setControllerEpoch(0)
            .setLeader(0)
            .setLeaderEpoch(0)
            .setIsr(brokerList)
            .setPartitionEpoch(0)

// After
      val tp0 = new TopicPartition(topic, 0)
      val tp1 = new TopicPartition(topic, 1)
      val partition0 = replicaManager.createPartition(tp0)
      val partition1 = replicaManager.createPartition(tp1)
      Seq(tp0, tp1).foreach { tp =>
        val delta = createLeaderDelta(
          topicId = topicId,
          partition = tp,
          leaderId = 0,
          replicas = brokerList,
          isr = brokerList
        )
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 remove becomeleaderorfollower from testpartition*, testpreferredreplicaas* (#20009).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
