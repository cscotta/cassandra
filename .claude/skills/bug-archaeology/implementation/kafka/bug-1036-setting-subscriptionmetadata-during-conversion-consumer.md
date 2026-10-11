**Commit**: fcb722dc88 · **Ticket**: KAFKA-18687
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
When a consumer protocol static member replaces an existing member in a
classic group, it's not necessary to recompute the assignment. However,
it happens anyway. In

[ConsumerGroup.fromClassicGroup](https://github.com/apache/kafka/blob/0ff4dafb7de4e24ddb7961d52e50e728f2eee4eb/group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java#L1140),
we don't set the group's subscriptionMetadata. Later in the consumer
group heartbeat, we [call

updateSubscriptionMetadata](https://github.com/apache/kafka/blob/0ff4dafb7de4e24ddb7961d52e50e728f2eee4eb/group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java#L1748),
which [no...

**Before/After**
```java
// Before
                metadataImage.topics()
     * @param topicsImage       The TopicsImage for topic id and topic name conversion.
        TopicsImage topicsImage

// After
                metadataImage.topics(),
                metadataImage.cluster()
import org.apache.kafka.image.ClusterImage;
     * @param topicsImage       The current metadata for all available topics.
     * @param clusterImage      The current metadata for the Kafka cluster.
        TopicsImage topicsImage,
        ClusterImage clusterImage
        consumerGroup.setSubscriptionMetadata(consumerGroup.computeSubscriptionMetadata(
            consumerGroup.subscribedTopicNames(),
            topicsImage,
            clusterImage
        ));
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18687: setting the subscriptionmetadata during conversion to consumer group (#19790).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
