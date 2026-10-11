**Commit**: 8d29bc1fa8 · **Ticket**: KAFKA-17247
**Subsystem**: group-coordinator/src
**Category**: Wrong serialization / deserialization

**Root cause**
In KIP-932, the group coordinator does not persist assignments for share groups. While this sounds like a good idea in terms of minimising overhead for data which doesn't strictly need to be recoverable, it significantly adds to the complexity of working with the coordinator framework. This PR revises the definitions of the share group record schemas following more closely the schemas used for consumer groups, and eliminating the need to maintain soft state alongside the group coordinator's timeline structure. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.Group.GroupType;
    public static CoordinatorRecord newMemberSubscriptionRecord(
    public static CoordinatorRecord newMemberSubscriptionTombstoneRecord(
    public static CoordinatorRecord newGroupSubscriptionMetadataRecord(
    public static CoordinatorRecord newGroupSubscriptionMetadataTombstoneRecord(
    public static CoordinatorRecord newGroupEpochRecord(
        return newGroupEpochRecord(groupId, newGroupEpoch, GroupType.CONSUMER);
    }

    /**
     * Creates a ConsumerGroupMetadata record.
     *

// After
import org.apache.kafka.coordinator.group.generated.ShareGroupCurrentMemberAssignmentKey;
import org.apache.kafka.coordinator.group.generated.ShareGroupCurrentMemberAssignmentValue;
import org.apache.kafka.coordinator.group.generated.ShareGroupPartitionMetadataKey;
import org.apache.kafka.coordinator.group.generated.ShareGroupPartitionMetadataValue;
import org.apache.kafka.coordinator.group.generated.ShareGroupTargetAssignmentMemberKey;
import org.apache.kafka.coordinator.group.generated.ShareGroupTargetAssignmentMemberValue;
import org.apache.kafka.coordinator.group.generated.ShareGroupTargetAssignmentMetadataKey;
import org.apache.kafka.coordinator.group.generated.ShareGroupTargetAssignmentMetadataValue;
    public static CoordinatorRecord newConsumerGroupMemberSubscriptionRecord(
    public static CoordinatorRecord newConsumerGroupMemberSubscriptionTombstoneRecord(
    public static CoordinatorRecord newConsumerGroupSubscriptionMetadataRecord(
    public static CoordinatorRecord newConsumerGroupSubscriptionMetadataTombstoneRecord(
```

**How to spot**
When reviewing group-coordinator/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17247 revised share group record schemas (#16786).

**Test**
Test modified in CoordinatorRecordHelpersTest.java covering the corrected behaviour.
