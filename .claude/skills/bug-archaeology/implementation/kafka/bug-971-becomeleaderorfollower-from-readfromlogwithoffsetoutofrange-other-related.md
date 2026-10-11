**Commit**: 3a0a1705a1 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
refactor out becomeLeaderOrFollower in below tests
- readFromLogWithOffsetOutOfRange
- testBecomeFollowerWhileNewClientFetchInPurgatory
- testBecomeFollowerWhileOldClientFetchInPurgatory
- testBuildRemoteLogAuxStateMetricsThrowsException

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ken Huang
 <s7133700@gmail.com>, TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
      val partition0Replicas = Seq[Integer](0, 1).asJava
      val becomeLeaderRequest = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
        Seq(new LeaderAndIsrRequest.PartitionState()
          .setTopicName(tp0.topic)
          .setPartitionIndex(tp0.partition)
          .setControllerEpoch(0)
          .setLeader(0)
          .setLeaderEpoch(1)
          .setIsr(partition0Replicas)
          .setPartitionEpoch(0)
          .setReplicas(partition0Replicas)
          .setIsNew(true)).asJava,

// After
      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 0)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
      val followerDelta = createFollowerDelta(topicId, tp0, followerId = 0, leaderId = 1, leaderEpoch = 2)
      val followerMetadataImage = imageFromTopics(followerDelta.apply())
      replicaManager.applyDelta(followerDelta, followerMetadataImage)
      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 0, leaderEpoch = 1, replicas = partition0Replicas, isr = partition0Replicas)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
      val followerDelta = createFollowerDelta(topicId, tp0, followerId = 0, leaderId = 1, leaderEpoch = 2)
      val followerMetadataImage = imageFromTopics(followerDelta.apply())
      replicaManager.applyDelta(followerDelta, followerMetadataImage)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 remove becomeleaderorfollower from readfromlogwithoffsetoutofrange and other related methods. (#19929).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
