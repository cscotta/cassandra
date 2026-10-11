**Commit**: 3dba3125e9 · **Ticket**: KAFKA-18601
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
3.3.0 was the first KRaft release that was deemed production-ready and also
when KIP-778 (KRaft to KRaft upgrades) landed. Given that, it's reasonable
for 4.x to only support upgrades from 3.3.0 or newer (the metadata version also
needs to be set to "3.3" or newer before upgrading). Noteworthy changes:
1. `AlterPartition` no longer includes topic names, which makes it possible to
simplify `AlterParitionManager` logic.

**Before/After**
```java
// Before
  //
  "validVersions": "0-3",
      { "name":  "TopicName", "type": "string", "versions": "0-1", "ignorable": true, "entityType": "topicName",
        "about": "The name of the topic to alter ISRs for." },
  //
  "validVersions": "0-3",
      { "name": "TopicName", "type": "string", "versions": "0-1", "ignorable": true, "entityType": "topicName",
        "about": "The name of the topic." },
    String topic = "test-topic";
        TopicData topicData = new TopicData()
            .setTopicId(topicId)
            .setTopicName(topic

// After
  // Versions 0-1 were removed in Apache Kafka 4.0, version 2 is the new baseline.

  "validVersions": "2-3",
  // Versions 0-1 were removed in Apache Kafka 4.0, version 2 is the new baseline.

  "validVersions": "2-3",
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18601: assume a baseline of 3.3 for server protocol versions (#18845).

**Test**
Test modified in AlterPartitionRequestTest.java covering the corrected behaviour.
