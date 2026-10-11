**Commit**: 2ce7c44612 · **Ticket**: KAFKA-19198
**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java
**Category**: Missing null / bounds check

**Root cause**: `attachTopicName(Set<Uuid>)` and `attachTopicName(Map<Uuid, Set<Integer>>)` called `topicsImage.getTopic(topicId).name()` without guarding against a `null` return. `getTopic()` returns `null` when the topic has been deleted and is no longer present in the metadata image, causing an `NullPointerException`. The fix adds a null check and substitutes `"<UNKNOWN>"` as a placeholder name for deleted topics.

**Before/After**

```java
// Before – NPE when topic deleted
return topicIds.stream()
    .map(topicId -> Map.entry(topicId, topicsImage.getTopic(topicId).name()))
    .collect(...);

// After – null guard
TopicImage topicImage = topicsImage.getTopic(topicId);
String topicName = (topicImage != null) ? topicImage.name() : "<UNKNOWN>";
finalMap.put(topicId, topicName);
```

**How to spot**: Every call site of `TopicsImage.getTopic(id)` must null-check the result, as the topic may have been deleted between when the assignment was made and when the name is looked up.

**Test**: No test added in this commit; noted as a temporary fix pending a proper topic-deletion handling PR.
