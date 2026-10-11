**Commit**: 081deaa1a9 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Change becomeLeaderOrFollower to applyDelta in following test cases

- testReadCommittedFetchLimitedAtLSO
- testReceiveOutOfOrderSequenceExceptionWithLogStartOffset
- testRemoteFetchExpiresPerSecMetric
- testRemoteLogReaderMetrics

Reviewers: PoAn Yang <payang@apache.org>, Bolin Lin
<linbolin1230@gmail.com>, Yung <yungyung7654321@gmail.com>, Jimmy Wang
<48462172+JimmyWang6@users.noreply.github.com>, Ken Huang
<s7133700@gmail.com>, TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
      val delta = createLeaderDelta(topicIds(topic), topicPartition, brokerList.get(0), brokerList, brokerList)
      val delta1 = createLeaderDelta(topicIds(topic), topicPartition, brokerList.get(1), brokerList, brokerList, 1)
      val partition = replicaManager.createPartition(new TopicPartition(topic, 0))
      val leaderAndIsrRequest1 = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
        Seq(new LeaderAndIsrRequest.PartitionState()
          .setTopicName(topic)
          .setPartitionIndex(0)
          .setControllerEpoch(0)
          .setLeader(0)
          .setLeaderEpoch(0)
          .setIsr(brokerList)
          .setPartitionEpoch(0)

// After
      val delta = createLeaderDelta(topicId, topicPartition, brokerList.get(0), brokerList, brokerList)
      val delta1 = createLeaderDelta(topicId, topicPartition, brokerList.get(1), brokerList, brokerList, 1)
      val tp = new TopicPartition(topic, 0)
      val partition = replicaManager.createPartition(tp)
      val delta = createLeaderDelta(topicId, tp, 0, brokerList, brokerList)
      val leaderMetadataImage = imageFromTopics(delta.apply())

      replicaManager.applyDelta(delta, leaderMetadataImage)
      val delta = createLeaderDelta(topicId, new TopicPartition(topic, 0), 0, brokerList, brokerList)
      val leaderMetadataImage = imageFromTopics(delta.apply())

      replicaManager.applyDelta(delta, leaderMetadataImage)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486: migrate replicamanagertest to use applydelta (#19954).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
