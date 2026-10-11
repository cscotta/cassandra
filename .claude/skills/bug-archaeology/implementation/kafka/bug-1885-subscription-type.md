**Commit**: b7294d92e1 · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/group-coordinator-api
**Category**: Logic error in condition

**Root cause**
This is the last patch in the series which introduces regular expressions in the new consumer group protocol. The patch ensures that the subscription type of the group takes into account the regular expressions. Please refer to the code to see how they are included. Reviewers: Sean Quah <squah@confluent.io>, Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
     * of the group are subscribed to the same set of topics.
     * of the group are subscribed to the same set of topics.
            Map<String, Integer> subscribedTopicNamesMap = group.computeSubscribedTopicNames(
                updatedMember
            );
            subscriptionMetadata = group.computeSubscriptionMetadata(
                subscribedTopicNamesMap,
                metadataImage.topics(),
                metadataImage.cluster()
            );

            int numMembers = group.numMembers();

// After
     * of the group use the same subscription.
     * of the group use the same subscription.
import org.apache.kafka.coordinator.group.modern.SubscriptionCount;
    private static class UpdateSubscriptionMetadataResult {
        private final int groupEpoch;
        private final Map<String, TopicMetadata> subscriptionMetadata;
        private final SubscriptionType subscriptionType;

        UpdateSubscriptionMetadataResult(
            int groupEpoch,
            Map<String, TopicMetadata> subscriptionMetadata,
            SubscriptionType subscriptionType
```

**How to spot**
When reviewing group-coordinator/group-coordinator-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [11/11] update subscription type (#18020).

**Test**
Test modified in ConsumerGroupTest.java covering the corrected behaviour.
