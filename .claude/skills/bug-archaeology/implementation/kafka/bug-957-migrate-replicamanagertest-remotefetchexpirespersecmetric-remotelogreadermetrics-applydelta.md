**Commit**: 94807bcd15 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Replace `becomeLeaderOrFollower` with `applyDelta` in method
RemoteFetchExpiresPerSecMetric and RemoteLogReaderMetrics

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      val partition0Replicas = Seq[Integer](0, 1).asJava
      val topicIds = Map(tp0.topic -> topicId).asJava
      val leaderAndIsrRequest = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
        Seq(
          new LeaderAndIsrRequest.PartitionState()
            .setTopicName(tp0.topic)
            .setPartitionIndex(tp0.partition)
            .setControllerEpoch(0)
            .setLeader(leaderEpoch)
            .setLeaderEpoch(0)
            .setIsr(partition0Replicas)
            .setPartitionEpoch(0)

// After

      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 0, leaderEpoch = leaderEpoch)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)

      val leaderDelta = createLeaderDelta(topicId, tp0, leaderId = 0, leaderEpoch = leaderEpoch)
      val leaderMetadataImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderMetadataImage)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 migrate replicamanagertest remotefetchexpirespersecmetric and remotelogreadermetrics with applydelta (#19960.

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
