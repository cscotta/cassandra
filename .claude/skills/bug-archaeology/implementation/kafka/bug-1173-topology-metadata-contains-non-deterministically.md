**Commit**: 5f80de3923 · **Ticket**: KAFKA-19162
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Topology description sent to broker in KIP-1071 contains
non-deterministically ordered topic configs. Since the topology is
compared to the groups topology upon joining we may run into
`INVALID_REQUEST: Topology updates are not supported yet` failures if
the topology sent by the  application does not match the group topology
due to different topic config order. This PR ensures that topic configs are ordered, to avoid an
`INVALID_REQUEST` error. Reviewers: Matthias J.

**Before/After**
```java
// Before
        REPARTITION_SOURCE_TOPIC_1, new StreamsRebalanceData.TopicInfo(Optional.of(2), Optional.of((short) 1), Map.of("config1", "value1")),
        CHANGELOG_TOPIC_3, new StreamsRebalanceData.TopicInfo(Optional.empty(), Optional.of((short) 3), Map.of("config2", "value2"))

// After
                repartitionTopicInfo.topicConfigs().sort(Comparator.comparing(StreamsGroupHeartbeatRequestData.KeyValue::key));
                changelogTopicInfo.topicConfigs().sort(Comparator.comparing(StreamsGroupHeartbeatRequestData.KeyValue::key));
        REPARTITION_SOURCE_TOPIC_1, new StreamsRebalanceData.TopicInfo(Optional.of(2), Optional.of((short) 1), Map.of("config3", "value3", "config1", "value1")),
        CHANGELOG_TOPIC_3, new StreamsRebalanceData.TopicInfo(Optional.empty(), Optional.of((short) 3), Map.of("config4", "value4", "config2", "value2"))
            assertTrue(isSorted(topicInfo.topicConfigs(), Comparator.comparing(StreamsGroupHeartbeatRequestData.KeyValue::key)));
            assertTrue(isSorted(topicInfo.topicConfigs(), Comparator.comparing(StreamsGroupHeartbeatRequestData.KeyValue::key)));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19162: topology metadata contains non-deterministically ordered topic configs (#19491).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
