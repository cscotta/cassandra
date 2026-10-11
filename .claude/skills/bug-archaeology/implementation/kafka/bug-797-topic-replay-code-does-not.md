**Commit**: 92fe00e184 · **Ticket**: KAFKA-19497
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
There is a small logic bug in topic replay. If a topic is created and
then removed before the TopicsDelta is applied, we end up with the
deleted topic in createdTopics on the delta. Tis issue is fixed by
removing the topicName from createTopics when replaying
RemoveTopicRecord to make sure the deleted topics won't appear in
createTopics. Reviewers: José Armando García Sancio <jsancio@apache.org>, Kevin Wu
 <kevin.wu2412@gmail.com>, Alyssa Huang <ahuang@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            createdTopics.remove(topicName);

    @Test
    public void testTopicsDeltaCreateThenDelete() {
        TopicsDelta delta = new TopicsDelta(TopicsImage.EMPTY);
        delta.replay(new TopicRecord().setName("test").setTopicId(FOO_UUID));
        assertEquals(delta.createdTopicIds().contains(FOO_UUID), true);
        assertEquals(delta.deletedTopicIds().contains(FOO_UUID), false);
        delta.replay(new RemoveTopicRecord().setTopicId(FOO_UUID));
        assertEquals(delta.deletedTopicIds().contains(FOO_UUID), false);
        assertEquals(delta.createdTopicIds().contains(FOO_UUID), false);
    }
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19497; topic replay code does not handle creation and deletion in the same delta (#20242).

**Test**
Test modified in TopicsImageTest.java covering the corrected behaviour.
