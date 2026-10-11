**Commit**: 0e1e68db8d · **Ticket**: KAFKA-19233
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
This patch adds tests to validate that duplicate full heartbeat requests
are  handled idempotently in all member states (STABLE,
UNREVOKED_PARTITIONS,  UNRELEASED_PARTITIONS). Reviewers: Dongnuo Lyu <dlyu@confluent.io>, Lianet Magrans
 <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After

  @ClusterTest
  def testDuplicateFullHeartbeatInStableState(): Unit = {
    createOffsetsTopic()

    val memberId = Uuid.randomUuid().toString
    val groupId = "test-duplicate-stable-grp"

    // Create topic first so member gets assignment immediately.
    val topicId = createTopic(topic = "foo", numPartitions = 3)

    // Join the group.
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19233: add tests for duplicate heartbeat request handling (#21319).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
