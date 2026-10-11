## Commit

d154a314e7 — KAFKA-14691: Extend OffsetFetch API to v10 with topic IDs (KIP-848)

## Ticket

KAFKA-14691

## Subsystem

Group coordinator / OffsetFetch API

## Category

Missing null / bounds check

## Root cause

`OffsetFetchRequest` only supported topic-name-based lookups up to version 9. Consumers using the KIP-848 new group protocol needed to fetch committed offsets by topic ID (UUID) to handle topic recreation correctly, since name alone cannot distinguish between the original and recreated topic. Without topic-ID support, the broker would return offsets for the old topic to a consumer that had joined for the newly created topic, silently producing incorrect start offsets.

## Before / After

```java
// Before — only topic-name builder
public static Builder forTopicNames(
    String groupId, List<TopicPartition> partitions) { ... }

// After — factory methods for both topic IDs and names
public static Builder forTopicIdsOrNames(
    String groupId,
    List<OffsetFetchRequestData.OffsetFetchRequestTopics> topicIdPartitions,
    List<TopicPartition> topicNamePartitions) { ... }
public static Builder forTopicNames(
    String groupId, List<TopicPartition> partitions) { ... }
```

## How to spot

Check `OffsetFetchRequest` builder factory methods for `forTopicIdsOrNames`. Its absence means the broker cannot serve offset-fetch requests that include topic IDs, causing the new group protocol to fall back to name-only matching and silently miss topic-recreation scenarios.

## Test

`OffsetFetchRequestTest` was extended with v10 serialization and deserialization tests covering the topic-ID path.
