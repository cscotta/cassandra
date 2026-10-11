**Commit**: 2a7457f2dd · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Refactor testPartitionMetadataFile to use applyDelta and share
class-level partitions

- Replace deprecated becomeLeaderOrFollower with topicsCreateDelta +
applyDelta
- Test still asserts partition exists, local log exists, and verifies
partitionMetadataFile version (0) and topicId

Reviewers: TaiJuWu <tjwu1217@gmail.com>, Ken Huang <s7133700@gmail.com>,
 Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      val brokerList = Seq[Integer](0, 1).asJava
      val topicPartition = new TopicPartition(topic, 0)
      val topicIds = Collections.singletonMap(topic, Uuid.randomUuid())
      val topicNames = topicIds.asScala.map(_.swap).asJava

      def leaderAndIsrRequest(epoch: Int, topicIds: java.util.Map[String, Uuid]): LeaderAndIsrRequest =
        new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
          Seq(new LeaderAndIsrRequest.PartitionState()
            .setTopicName(topic)
            .setPartitionIndex(0)
            .setControllerEpoch(0)
            .setLeader(0)

// After
  private val topicPartition = new TopicPartition(topic, 0)
      val leaderDelta = topicsCreateDelta(0, isStartIdLeader = true, partitions = List(0),
        topicName = topic, topicId = topicIds(topic))
      val leaderImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderImage)
      assertTrue(replicaManager.getPartition(topicPartition).isInstanceOf[HostedPartition.Online])
      val id = topicIds(topicPartition.topic)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 migrate testpartitionmetadatafile to use applydelta in place of deprecated becomeleaderorfollower (#19947).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
