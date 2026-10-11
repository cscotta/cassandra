**Commit**: c4823daa00 · **Ticket**: KAFKA-20165
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Adding integration tests to show/validate the expected responses when
fetching offsets for a deleted topic, which changes depending on the
version (topic names vs topic Ids used). Reviewers: David Jacot <djacot@confluent.io>, Lan Ding
 <isDing_L@163.com>, David Jacot <david.jacot@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  /**
   * Helper to set up two topics (foo with 3 partitions, bar with 2 partitions),
   * join a consumer group, and commit offsets to both topics.
   */
  private def setupTopicsJoinAndCommit(): (Uuid, Uuid, String, Int) = {
    createOffsetsTopic()

    // Create two topics.
    val fooTopicId = createTopic(topic = "foo", numPartitions = 3)
    val barTopicId = createTopic(topic = "bar", numPartitions = 2)

    // Join the consumer group.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20165 [2]: integration tests to validate different offsetfetch response behaviour on topic deleted (#21509).

**Test**
Test modified in OffsetFetchRequestTest.scala covering the corrected behaviour.
