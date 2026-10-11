**Commit**: 86419e9b8a · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Change becomeLeaderOrFollower to applyDelta in following test cases:

- testTransactionAddPartitionRetry
- testTransactionVerificationBlocksOutOfOrderSequence
- testTransactionVerificationDynamicDisablement
- testTransactionVerificationFlow
- testTransactionVerificationGuardOnMultiplePartitions
- testTransactionVerificationRejectsLowerProducerEpoch

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
      replicaManager.becomeLeaderOrFollower(1,
        makeLeaderAndIsrRequest(topicIds(tp0.topic), tp0, Seq(0, 1), new LeaderAndIsr(1, List(0, 1).map(Int.box).asJava)),
        (_, _) => ())
      replicaManager.becomeLeaderOrFollower(1,
        makeLeaderAndIsrRequest(topicIds(tp0.topic), tp0, Seq(0, 1), new LeaderAndIsr(1, List(0, 1).map(Int.box).asJava)),
        (_, _) => ())
      replicaManager.becomeLeaderOrFollower(1,
        makeLeaderAndIsrRequest(topicIds(tp0.topic), tp0, Seq(0, 1), new LeaderAndIsr(1, List(0, 1).map(Int.box).asJava)),
        (_, _) => ())
      replicaManager.becomeLeaderOrFollower(
        1,
        makeLeaderAndIsrRequest(

// After
    val brokerList = Seq[Integer](0, 1).asJava
      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 1, replicas = brokerList, isr = brokerList)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
    val brokerList = Seq[Integer](0, 1).asJava
      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 1, replicas = brokerList, isr = brokerList)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
    val brokerList = Seq[Integer](0, 1).asJava
      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 1, replicas = brokerList, isr = brokerList)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 migrate replicamanagertest#testtransactionaddpartitionretry and other similar methods to use applydelta (#19.

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
