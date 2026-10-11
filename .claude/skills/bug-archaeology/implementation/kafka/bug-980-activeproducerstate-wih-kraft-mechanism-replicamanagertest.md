**Commit**: 8a7e4a1423 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Description:
* replace RPC with KRaft mechanism to test activeProducerState in
ReplicaManagerTest

Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
      // This API is supported by both leaders and followers

      val barLeaderAndIsrRequest = makeLeaderAndIsrRequest(
        topicId = Uuid.randomUuid(),
        topicPartition = barPartition,
        replicas = Seq(brokerId),
        leaderAndIsr = new LeaderAndIsr(brokerId, List(brokerId).map(Int.box).asJava)
      )
      replicaManager.becomeLeaderOrFollower(0, barLeaderAndIsrRequest, (_, _) => ())
      val otherBrokerId = 1
      val bazLeaderAndIsrRequest = makeLeaderAndIsrRequest(
        topicId = Uuid.randomUuid(),

// After
      val barTopicId = Uuid.randomUuid()

      val leaderDelta = createLeaderDelta(barTopicId, barPartition, brokerId)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)

      val bazTopicId = Uuid.randomUuid()
      val otherBrokerId = 1

      val followerDelta = createFollowerDelta(bazTopicId, bazPartition, brokerId, otherBrokerId)
      val followerMetadataImage = imageFromTopics(followerDelta.apply())
      replicaManager.applyDelta(followerDelta, followerMetadataImage)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 update activeproducerstate wih kraft mechanism in replicamanagertest (#19890).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
