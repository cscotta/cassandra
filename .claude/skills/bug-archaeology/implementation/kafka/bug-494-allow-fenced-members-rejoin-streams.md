**Commit**: b395e30c1d · **Ticket**: KAFKA-19233
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
This fix allows members to rejoin a streams group with memberEpoch=0
after being fenced, as specified by KIP-848. Previously, the validation
in throwIfStreamsGroupMemberEpochIsInvalid rejected epoch=0 when the
member had a higher epoch on the server. Reviewers: Lianet Magrans <lmagrans@confluent.io>, Lucas Brutschy
 <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
  @ClusterTest
  def testFencedMemberCanRejoinWithEpochZero(): Unit = {
    val admin = cluster.admin()
    val memberId = "test-fenced-rejoin-member"
    val groupId = "test-fenced-rejoin-group"
    val topicName = "test-fenced-topic"

    try {
      TestUtils.createOffsetsTopicWithAdmin(
        admin = admin,
        brokers = cluster.brokers.values().asScala.toSeq,
        controllers = cluster.controllers().values().asScala.toSeq
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19233; allow fenced members to rejoin streams group with epoch 0 (#21312).

**Test**
Test modified in StreamsGroupHeartbeatRequestTest.scala covering the corrected behaviour.
