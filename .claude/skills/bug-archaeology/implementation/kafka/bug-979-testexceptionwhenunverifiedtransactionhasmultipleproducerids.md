**Commit**: c5e06f6e7a · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
- Replace the deprecated `becomeLeaderOrFollower` with the
metadata-based `applyDelta` method. - Add overloaded `topicsCreateDelta` to support custom topic name and
topicId. Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi
 <kitingiao@gmail.com>, Nick Guo <lansg0504@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
      replicaManager.becomeLeaderOrFollower(1,
        makeLeaderAndIsrRequest(topicIds(tp0.topic), tp0, Seq(0, 1), new LeaderAndIsr(1, List(0, 1).map(Int.box).asJava)),
        (_, _) => ())

      replicaManager.becomeLeaderOrFollower(1,
        makeLeaderAndIsrRequest(topicIds(tp1.topic), tp1, Seq(0, 1), new LeaderAndIsr(1, List(0, 1).map(Int.box).asJava)),
        (_, _) => ())
      val leaderTopicsDelta: TopicsDelta = topicsCreateDelta(localId, true, partition = 0, directoryIds = directoryIds)
      val leaderTopicsDelta = topicsCreateDelta(localId, true, partition = 0, directoryIds = directoryIds)
      val followerTopicsDelta = topicsCreateDelta(localId, false, partition = 1, directoryIds = directoryIds)
      val leaderTopicsDelta = topicsCreateDelta(localId, true, partition = 0, directoryIds = List(DirectoryId.UNASSIGNED, DirectoryId.UNASSIGNED))
      val followerTopicsDelta = topicsCreateDelta(localId, false, partition = 1, directoryIds = List(DirectoryId.UNASSIGNED, DirectoryId.UNASSIGNED))

// After
    val localId = 1
      val leaderDelta = topicsCreateDelta(localId, isStartIdLeader = true, partitions = List(0, 1), List.empty, topic, topicIds(topic))
      val leaderImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderImage)
      val leaderTopicsDelta: TopicsDelta = topicsCreateDelta(localId, true, partitions = List(0), directoryIds = directoryIds)
      val leaderTopicsDelta = topicsCreateDelta(localId, true, partitions = List(0), directoryIds = directoryIds)
      val followerTopicsDelta = topicsCreateDelta(localId, false, partitions = List(1), directoryIds = directoryIds)
      val leaderTopicsDelta = topicsCreateDelta(localId, true, partitions = List(0), directoryIds = List(DirectoryId.UNASSIGNED, DirectoryId.UNASSIGNED))
      val followerTopicsDelta = topicsCreateDelta(localId, false, partitions = List(1), directoryIds = List(DirectoryId.UNASSIGNED, DirectoryId.UNASSIGNED))
      val followerTopicsDelta = topicsCreateDelta(localId, false, partitions = List(1), directoryIds = List(DirectoryId.LOST, DirectoryId.LOST))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 update testexceptionwhenunverifiedtransactionhasmultipleproducerids  (#19883).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
