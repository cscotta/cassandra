**Commit**: c58de75712 · **Ticket**: KAFKA-19204
**Subsystem**: group-coordinator/src
**Category**: Race condition / missing synchronization

**Root cause**
1. Currently, the code allows for retrying any initializing topics in
subsequent heartbeats. This can result in duplicate calls to persister
if multiple share consumers join the same group concurrently. Furthermore, only one of these will succeed as the others will have a
lower state epoch and will be fenced.

**Before/After**
```java
// Before
        Map<Uuid, Map.Entry<String, Set<Integer>>> initializingTopics,
        Map<Uuid, Map.Entry<String, Set<Integer>>> initializedTopics,
                .setTopicName(entry.getValue().getKey())
                .setPartitions(entry.getValue().getValue().stream().toList()))
                .setTopicName(entry.getValue().getKey())
                .setPartitions(entry.getValue().getValue().stream().toList()))

// After
import org.apache.kafka.coordinator.group.modern.share.ShareGroup.InitMapValue;
        Map<Uuid, InitMapValue> initializingTopics,
        Map<Uuid, InitMapValue> initializedTopics,
                .setTopicName(entry.getValue().name())
                .setPartitions(entry.getValue().partitions().stream().toList()))
                .setTopicName(entry.getValue().name())
                .setPartitions(entry.getValue().partitions().stream().toList()))
import org.apache.kafka.coordinator.group.modern.share.ShareGroup.InitMapValue;
import org.apache.kafka.coordinator.group.modern.share.ShareGroup.ShareGroupStatePartitionMetadataInfo;
```

**How to spot**
When reviewing group-coordinator/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19204: add timestamp to share state metadata init maps [1/n] (#19781).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
