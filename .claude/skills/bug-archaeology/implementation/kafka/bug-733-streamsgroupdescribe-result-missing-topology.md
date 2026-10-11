**Commit**: fb0518c34e · **Ticket**: KAFKA-19730
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When toology not configured. In the streams group heartbeat, we validate the topology set for the
group against the topic metadata, to generate the "configured topology"
which has a specific number of partitions for each topic. In streams group describe, we use the configured topology to expose this
information to the user. However, we leave the topology information as
null in the streams group describe response, if the topology is not
configured.

**Before/After**
```java
// Before
            .setTopology(configuredTopology.get(committedOffset).map(ConfiguredTopology::asStreamsGroupDescribeTopology).orElse(null));

// After
  @Test
  def testDescribeStreamsGroupsNotReady(): Unit = {
    val streamsGroupId = "stream_group_id"
    val testTopicName = "test_topic"

    val config = createConfig
    client = Admin.create(config)

    val streams = createStreamsGroup(
      inputTopic = testTopicName,
      streamsGroupId = streamsGroupId
    )
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19730: streamsgroupdescribe result is missing topology (#20574).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
