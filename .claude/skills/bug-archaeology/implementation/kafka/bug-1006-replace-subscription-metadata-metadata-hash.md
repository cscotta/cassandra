**Commit**: 425f028556 · **Ticket**: KAFKA-17747
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Use metadata hash to replace subscription metadata. * Remove `StreamsGroupPartitionMetadataKey` and
`StreamsGroupPartitionMetadataValue`. * Check whether `configuredTopology` is empty. If it's, call
`InternalTopicManager.configureTopics` and set the result to the group.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataValue;
            case STREAMS_GROUP_PARTITION_METADATA:
                groupMetadataManager.replay(
                    (StreamsGroupPartitionMetadataKey) key,
                    (StreamsGroupPartitionMetadataValue) Utils.messageOrNull(value)
                );
                break;

import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataValue;
import static org.apache.kafka.coordinator.group.streams.StreamsCoordinatorRecordHelpers.newStreamsGroupPartitionMetadataRecord;

// After
        // 3. Determine any internal topics if needed.
        long metadataHash = group.metadataHash();
        if (reconfigureTopology || group.configuredTopology().isEmpty() || group.hasMetadataExpired(currentTimeMs)) {
            metadataHash = group.computeMetadataHash(
                metadataImage,
                topicHashCache,
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17747: [5/n] replace subscription metadata with metadata hash in stream group (#19802).

**Test**
Test modified in GroupCoordinatorShardTest.java covering the corrected behaviour.
