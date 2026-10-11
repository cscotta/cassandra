**Commit**: d6ee83a893 · **Ticket**: KAFKA-17747
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Add `topicHashCache` to `GroupMetadataManager`. * Remove topic hash from cache if related topic image is updated. * Ignore topic hash 0 when calculating group metadata hash. * Add `metadataHash` to `ModernGroup`.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.generated.ConsumerGroupPartitionMetadataValue;
    /**
     * Creates a ConsumerGroupPartitionMetadata record.
     *
     * @param groupId                   The consumer group id.
     * @param newSubscriptionMetadata   The subscription metadata.
     * @return The record.
     */
    public static CoordinatorRecord newConsumerGroupSubscriptionMetadataRecord(
        String groupId,
        Map<String, TopicMetadata> newSubscriptionMetadata
    ) {

// After
import static org.apache.kafka.coordinator.group.GroupCoordinatorRecordHelpers.newConsumerGroupSubscriptionMetadataTombstoneRecord;
    /**
     * The cache for topic hash value by topic name.
     * A topic hash is calculated when there is a group subscribes to it.
     * A topic hash is removed when it's updated in MetadataImage or there is no group subscribes to it.
     */
    private final Map<String, Long> topicHashCache;

        this.topicHashCache = new HashMap<>();
                topicHashCache,
                metadataImage
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17747: [4/n] replace subscription metadata with metadata hash in consumer group (#19761).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
