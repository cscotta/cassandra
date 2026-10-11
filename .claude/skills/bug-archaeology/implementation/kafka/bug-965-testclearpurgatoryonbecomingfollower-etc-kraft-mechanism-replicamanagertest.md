**Commit**: 7c715c02c0 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
update the following test to avoid using `becomeLeaderOrFollower`
- testClearPurgatoryOnBecomingFollower
- testDelayedFetchIncludesAbortedTransactions
- testDisabledTransactionVerification
- testFailedBuildRemoteLogAuxStateMetrics

Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
      val topicIds = Collections.singletonMap(topic, topicId)
      val partition = rm.createPartition(new TopicPartition(topic, 0))
      val leaderAndIsrRequest1 = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
        Seq(new LeaderAndIsrRequest.PartitionState()
          .setTopicName(topic)
          .setPartitionIndex(0)
          .setControllerEpoch(0)
          .setLeader(0)
          .setLeaderEpoch(0)
          .setIsr(brokerList)
          .setPartitionEpoch(0)
          .setReplicas(brokerList)

// After
      val topicPartition = new TopicPartition(topic, 0)
      val partition = rm.createPartition(topicPartition)
      val delta = createLeaderDelta(topicIds(topic), topicPartition, brokerList.get(0), brokerList, brokerList)
      val leaderMetadataImage = imageFromTopics(delta.apply())
      rm.applyDelta(delta, leaderMetadataImage)
      val delta1 = createLeaderDelta(topicIds(topic), topicPartition, brokerList.get(1), brokerList, brokerList, 1)
      val followerMetadataImage = imageFromTopics(delta1.apply())
      rm.applyDelta(delta1, followerMetadataImage)
      val delta = topicsCreateDelta(brokerList.get(0), isStartIdLeader = true, partitions = List(0), List.empty, topic, topicIds(topic))
      val leaderMetadataImage = imageFromTopics(delta.apply())
      replicaManager.applyDelta(delta, leaderMetadataImage)
      val delta = topicsCreateDelta(0, isStartIdLeader = true, partitions = List(0), List.empty, topic, topicIds(topic))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 update testclearpurgatoryonbecomingfollower etc with kraft mechanism in replicamanagertest (#19924).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
