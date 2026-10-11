**Commit**: f6f5b4cb27 · **Ticket**: KAFKA-19565
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Integration tests for Stream Admin related API

Previous PR: https://github.com/apache/kafka/pull/20244

This one adds:
- Integration test for Admin#listStreamsGroupOffsets API
- Integration test for Admin#deleteStreamsGroupOffsets API
- Integration test for Admin#alterStreamsGroupOffsets API

Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Lucas Brutschy
 <lucasbru@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After

  @Test
  def testListStreamsGroupOffsets(): Unit = {
    val streamsGroupId = "stream_group_id"
    val testTopicName = "test_topic"
    val testNumPartitions = 3

    val config = createConfig
    client = Admin.create(config)
    val producer = createProducer(configOverrides = new Properties())

    prepareTopics(List(testTopicName), testNumPartitions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19565: integration test for streams-related admin apis [2/n] (#20266).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
