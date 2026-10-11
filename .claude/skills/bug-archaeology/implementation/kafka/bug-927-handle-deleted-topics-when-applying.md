**Commit**: dcc9320bf6 · **Ticket**: KAFKA-19383
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-19383  When applying the
ClearElrRecord, it may pick up the topicId in the image without checking
if the topic has been deleted. This can cause the creation of a new
TopicRecord with an old topic ID. Reviewers: Alyssa Huang <ahuang@confluent.io>, Artem Livshits <alivshits@confluent.io>, Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
            Uuid topicId;
            if (image.getTopic(record.topicName()) != null) {
                topicId = image.getTopic(record.topicName()).id();
            } else {
            TopicDelta topicDelta = getOrCreateTopicDelta(topicId);
            topicDelta.replay(record);
                TopicDelta topicDelta = getOrCreateTopicDelta(topicId);
                topicDelta.replay(record);

// After
    private void maybeReplayClearElrRecord(Uuid topicId, ClearElrRecord record) {
        // Only apply the record if the topic is not deleted.
        if (!deletedTopicIds.contains(topicId)) {
            TopicDelta topicDelta = getOrCreateTopicDelta(topicId);
            topicDelta.replay(record);
        }
    }

    // When replaying the ClearElrRecord, we need to first find the latest topic ID associated with the topic(s) because
    // multiple topic IDs for the same topic in a TopicsDelta is possible in the event of topic deletion and recreation.
    // Second, we should not add the topicDelta if the given topic ID has been deleted. So that we don't leak the
    // deleted topic ID.
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19383: handle the deleted topics when applying clearelrrecord (#19916).

**Test**
Test modified in TopicsImageTest.java covering the corrected behaviour.
