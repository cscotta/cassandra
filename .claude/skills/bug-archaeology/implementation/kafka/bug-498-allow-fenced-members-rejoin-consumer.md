**Commit**: 384586e52c · **Ticket**: KAFKA-19233
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
This fix allows members to rejoin a consumer group with memberEpoch=0
after being fenced, as specified by KIP-848. Previously, the validation
in throwIfConsumerGroupMemberEpochIsInvalid rejected epoch=0 when the
member had a higher epoch on the server. Changes:
- Add early return for receivedMemberEpoch=0 in validation method
- Add unit tests for rejoin in STABLE, UNREVOKED_PARTITIONS, and
UNRELEASED_PARTITIONS states
- Add integration test for fenced member rejoin flow

Reviewers: Dongnuo Lyu <dlyu@confluent.io>, Sean Quah
 <squah@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After

  @ClusterTest
  def testFencedMemberCanRejoinWithEpochZero(): Unit = {
    // Creates the __consumer_offsets topics because it won't be created automatically
    // in this test because it does not use FindCoordinator API.
    createOffsetsTopic()

    val memberId = Uuid.randomUuid().toString
    val groupId = "test-fenced-rejoin-grp"

    // Heartbeat request to join the group.
    var consumerGroupHeartbeatRequest = new ConsumerGroupHeartbeatRequest.Builder(
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19233; allow fenced members to rejoin consumer group with epoch 0 (#21305).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
