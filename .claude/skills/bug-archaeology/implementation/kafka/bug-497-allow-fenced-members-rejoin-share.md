**Commit**: 8dc39b570e · **Ticket**: KAFKA-19233
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Apply the same fix for Share groups. The
throwIfShareGroupMemberEpochIsInvalid method was rejecting epoch=0 for
existing members, preventing fenced members from rejoining. Changes:
- Add early return for receivedMemberEpoch=0 in Share group validation
- Add unit test testShareGroupMemberCanRejoinWithEpochZero
- Add integration test in ShareGroupHeartbeatRequestTest

Reviewers: Lianet Magrans <lmagrans@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
  @ClusterTest(
    serverProperties = Array(
      new ClusterConfigProperty(key = "offsets.topic.num.partitions", value = "1"),
      new ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1")
    ))
  def testFencedMemberCanRejoinWithEpochZero(): Unit = {
    val admin = cluster.admin()

    try {
      TestUtils.createOffsetsTopicWithAdmin(
        admin = admin,
        brokers = cluster.brokers.values().asScala.toSeq,
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19233; allow fenced members to rejoin share group with epoch 0 (#21309).

**Test**
Test modified in ShareGroupHeartbeatRequestTest.scala covering the corrected behaviour.
