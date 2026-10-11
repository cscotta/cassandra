**Commit**: 2977cb17d0 · **Ticket**: KAFKA-17747
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Use metadata hash to replace subscription metadata. * Remove `ShareGroupPartitionMetadataKey` and
`ShareGroupPartitionMetadataValue`. * Use `subscriptionTopicNames` and `metadataImage` to replace
`subscriptionMetadata` in `subscribedTopicsChangeMap` function. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Jacot
<djacot@confluent.io>, Andrew Schofield <aschofield@confluent.io>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.generated.ShareGroupPartitionMetadataKey;
import org.apache.kafka.coordinator.group.generated.ShareGroupPartitionMetadataValue;
import org.apache.kafka.coordinator.group.modern.TopicMetadata;
    /**
     * Creates a ShareGroupPartitionMetadata record.
     *
     * @param groupId                   The group id.
     * @param newSubscriptionMetadata   The subscription metadata.
     * @return The record.
     */
    public static CoordinatorRecord newShareGroupSubscriptionMetadataRecord(
        String groupId,

// After
// (no additions)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17747: [6/n] replace subscription metadata with metadata hash in share group (#19796).

**Test**
Test modified in Assertions.java covering the corrected behaviour.
