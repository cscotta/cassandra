**Commit**: 12a16ecf28 · **Ticket**: KAFKA-16733
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
This patch adds support for decoding the new KIP-932 record schemas in kafka-dump-log.sh

Reviewers: David Arthur <mumrah@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.generated.{ConsumerGroupCurrentMemberAssignmentKey, ConsumerGroupCurrentMemberAssignmentKeyJsonConverter, ConsumerGroupCurrentMemberAssignmentValue, ConsumerGroupCurrentMemberAssignmentValueJsonConverter, ConsumerGroupMemberMetadataKey, ConsumerGroupMemberMetadataKeyJsonConverter, ConsumerGroupMemberMetadataValue, ConsumerGroupMemberMetadataValueJsonConverter, ConsumerGroupMetadataKey, ConsumerGroupMetadataKeyJsonConverter, ConsumerGroupMetadataValue, ConsumerGroupMetadataValueJsonConverter, ConsumerGroupPartitionMetadataKey, ConsumerGroupPartitionMetadataKeyJsonConverter, ConsumerGroupPartitionMetadataValue, ConsumerGroupPartitionMetadataValueJsonConverter, ConsumerGroupTargetAssignmentMemberKey, ConsumerGroupTargetAssignmentMemberKeyJsonConverter, ConsumerGroupTargetAssignmentMemberValue, ConsumerGroupTargetAssignmentMemberValueJsonConverter, ConsumerGroupTargetAssignmentMetadataKey, ConsumerGroupTargetAssignmentMetadataKeyJsonConverter, ConsumerGroupTargetAssignmentMetadataValue, ConsumerGroupTargetAssignmentMetadataValueJsonConverter, GroupMetadataKey, GroupMetadataKeyJsonConverter, GroupMetadataValue, GroupMetadataValueJsonConverter, OffsetCommitKey, OffsetCommitKeyJsonConverter, OffsetCommitValue, OffsetCommitValueJsonConverter}

// After
import org.apache.kafka.coordinator.group.generated.{ConsumerGroupCurrentMemberAssignmentKey, ConsumerGroupCurrentMemberAssignmentKeyJsonConverter, ConsumerGroupCurrentMemberAssignmentValue, ConsumerGroupCurrentMemberAssignmentValueJsonConverter, ConsumerGroupMemberMetadataKey, ConsumerGroupMemberMetadataKeyJsonConverter, ConsumerGroupMemberMetadataValue, ConsumerGroupMemberMetadataValueJsonConverter, ConsumerGroupMetadataKey, ConsumerGroupMetadataKeyJsonConverter, ConsumerGroupMetadataValue, ConsumerGroupMetadataValueJsonConverter, ConsumerGroupPartitionMetadataKey, ConsumerGroupPartitionMetadataKeyJsonConverter, ConsumerGroupPartitionMetadataValue, ConsumerGroupPartitionMetadataValueJsonConverter, ConsumerGroupTargetAssignmentMemberKey, ConsumerGroupTargetAssignmentMemberKeyJsonConverter, ConsumerGroupTargetAssignmentMemberValue, ConsumerGroupTargetAssignmentMemberValueJsonConverter, ConsumerGroupTargetAssignmentMetadataKey, ConsumerGroupTargetAssignmentMetadataKeyJsonConverter, ConsumerGroupTargetAssignmentMetadataValue, ConsumerGroupTargetAssignmentMetadataValueJsonConverter, GroupMetadataKey, GroupMetadataKeyJsonConverter, GroupMetadataValue, GroupMetadataValueJsonConverter, OffsetCommitKey, OffsetCommitKeyJsonConverter, OffsetCommitValue, OffsetCommitValueJsonConverter, ShareGroupCurrentMemberAssignmentKey, ShareGroupCurrentMemberAssignmentKeyJsonConverter, ShareGroupCurrentMemberAssignmentValue, ShareGroupCurrentMemberAssignmentValueJsonConverter, ShareGroupMemberMetadataKey, ShareGroupMemberMetadataKeyJsonConverter, ShareGroupMemberMetadataValue, ShareGroupMemberMetadataValueJsonConverter, ShareGroupMetadataKey, ShareGroupMetadataKeyJsonConverter, ShareGroupMetadataValue, ShareGroupMetadataValueJsonConverter, ShareGroupPartitionMetadataKey, ShareGroupPartitionMetadataKeyJsonConverter, ShareGroupPartitionMetadataValue, ShareGroupPartitionMetadataValueJsonConverter, ShareGroupStatePartitionMetadataKey, ShareGroupStatePartitionMetadataKeyJsonConverter, ShareGroupStatePartitionMetadataValue, ShareGroupStatePartitionMetadataValueJsonConverter, ShareGroupTargetAssignmentMemberKey, ShareGroupTargetAssignmentMemberKeyJsonConverter, ShareGroupTargetAssignmentMemberValue, ShareGroupTargetAssignmentMemberValueJsonConverter, ShareGroupTargetAssignmentMetadataKey, ShareGroupTargetAssignmentMetadataKeyJsonConverter, ShareGroupTargetAssignmentMetadataValue, ShareGroupTargetAssignmentMetadataValueJsonConverter}
        case m: ShareGroupMetadataKey =>
          ShareGroupMetadataKeyJsonConverter.write(m, version)
        case m: ShareGroupPartitionMetadataKey =>
          ShareGroupPartitionMetadataKeyJsonConverter.write(m, version)
        case m: ShareGroupMemberMetadataKey =>
          ShareGroupMemberMetadataKeyJsonConverter.write(m, version)
        case m: ShareGroupTargetAssignmentMetadataKey =>
          ShareGroupTargetAssignmentMetadataKeyJsonConverter.write(m, version)
        case m: ShareGroupTargetAssignmentMemberKey =>
          ShareGroupTargetAssignmentMemberKeyJsonConverter.write(m, version)
        case m: ShareGroupCurrentMemberAssignmentKey =>
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16733: add share group record support to offsetsmessageparser (#17282).

**Test**
No test added.
