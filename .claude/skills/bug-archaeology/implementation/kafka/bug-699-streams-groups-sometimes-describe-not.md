**Commit**: 4cb5f1ca12 · **Ticket**: KAFKA-19661
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Streams groups sometimes describe as NOT_READY when STABLE. That is, the
group is configured and all topics exist, but when you use LIST_GROUP
and STREAMS_GROUP_DESCRIBE, the group will show up as not ready. The root cause seems to be that
https://github.com/apache/kafka/pull/19802 moved the creation of the
soft state configured topology from the replay path to the heartbeat. This way, LIST_GROUP and STREAMS_GROUP_DESCRIBE, may not show the
configured topology, because the configured topology that is created in
the heartbeat is "thrown away", and the new group is recreated on the
replay-path.

**Before/After**
```java
// Before
                               inputTopic: String,
          util.Set.of(inputTopic),
          util.Map.of(inputTopic + "-store-changelog", new StreamsRebalanceData.TopicInfo(Optional.of(1), Optional.empty(), util.Map.of())),
    consumer.subscribe(util.Set.of(inputTopic),

      inputTopic = testTopicName,
      inputTopic = testTopicName,
      }, "Stream group not stable yet")
      inputTopic = testTopicName,

// After
                               inputTopics: Set[String],
                               changelogTopics: Set[String] = Set(),
          inputTopics.asJava,
          changelogTopics.map(c => (c, new StreamsRebalanceData.TopicInfo(Optional.empty(), Optional.empty(), util.Map.of()))).toMap.asJava,
    consumer.subscribe(inputTopics.asJava,
      inputTopics = Set(testTopicName),
      changelogTopics = Set(testTopicName + "-changelog"),
      inputTopics = Set(testTopicName),
      changelogTopics = Set(testTopicName + "-changelog"),
      }, "Streams group did not transition to STABLE before timeout")
      inputTopics = Set(testTopicName),
      changelogTopics = Set(testTopicName + "-changelog"),
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19661: streams groups sometimes describe as not_ready when stable (#20600).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
