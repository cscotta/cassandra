**Commit**: e23c8cea07 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Use `applyDelta` replace `becomeLeaderOrFollower`

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
      val leaderAndIsrRequest = makeLeaderAndIsrRequest(
        topicId = topicId,
        topicPartition = tp,
        replicas = Seq(0, 1),
        leaderAndIsr = new LeaderAndIsr(0, List(0, 1).map(Int.box).asJava),
      )
      replicaManager.becomeLeaderOrFollower(0, leaderAndIsrRequest, (_, _) => ())

// After
    val localId = 0
      val leaderDelta = topicsCreateDelta(localId, isStartIdLeader = true, partitions = List(0, 1), List.empty, topic, topicIds(topic))
      val leaderImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderImage)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 remove replicamanager#becomeleaderorfollower from `testreplicaalterlogdirs` (#19922).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
