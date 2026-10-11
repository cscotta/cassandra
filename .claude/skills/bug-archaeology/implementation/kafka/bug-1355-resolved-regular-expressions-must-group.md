**Commit**: 1df4a42b40 · **Ticket**: KAFKA-18916
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When regular expressions are resolved, they do not update the group by
topics data structure. Hence, topic changes (e.g. deletion) do not
trigger a rebalance of the group. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.Uuid
      // to an nonexistent topic.
      val expectedAssignment = new ConsumerGroupHeartbeatResponseData.Assignment()
            ConsumerGroup group = getOrMaybeCreatePersistedConsumerGroup(groupId, true);
            group.updateResolvedRegularExpression(
            try {
                ConsumerGroup group = getOrMaybeCreatePersistedConsumerGroup(groupId, false);
                group.removeResolvedRegularExpression(regex);
            } catch (GroupIdNotFoundException ex) {
                // If the group does not exist, we can ignore the tombstone.
            }

// After
import org.apache.kafka.common.{TopicCollection, Uuid}
      // to a nonexistent topic.
      var expectedAssignment = new ConsumerGroupHeartbeatResponseData.Assignment()

      // Delete the topic.
      admin.deleteTopics(TopicCollection.ofTopicIds(List(topicId).asJava)).all.get

      // Prepare the next heartbeat.
      consumerGroupHeartbeatRequest = new ConsumerGroupHeartbeatRequest.Builder(
        new ConsumerGroupHeartbeatRequestData()
          .setGroupId("grp")
          .setMemberId(consumerGroupHeartbeatResponse.data.memberId)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18916; resolved regular expressions must update the group by topics data structure (#19088).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
