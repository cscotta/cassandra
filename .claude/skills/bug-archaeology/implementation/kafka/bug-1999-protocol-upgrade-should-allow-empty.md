**Commit**: 8ccb26de2e · **Ticket**: KAFKA-17733
**Subsystem**: group-coordinator/src
**Category**: Wrong serialization / deserialization

**Root cause**
During conversion from classic to consumer group, if a member has empty assignment (e.g., the member just joined and has never synced), the conversion will fail because of the buffer underflow error when deserializing the member assignment. This patch allows empty assignment while deserializing the member assignment. Reviewers: Jeff Kim <jeff.kim@confluent.io>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
            Map<Uuid, Set<Integer>> assignedPartitions = toTopicPartitionMap(
                ConsumerProtocol.deserializeConsumerProtocolAssignment(
                    ByteBuffer.wrap(classicGroupMember.assignment())
                ),
                topicsImage
            );

// After
import java.util.Arrays;
import static org.apache.kafka.coordinator.group.classic.ClassicGroupMember.EMPTY_ASSIGNMENT;
            // The assigned partition can be empty if the member just joined and has never synced.
            // We should accept the empty assignment.
            Map<Uuid, Set<Integer>> assignedPartitions;
            if (Arrays.equals(classicGroupMember.assignment(), EMPTY_ASSIGNMENT)) {
                assignedPartitions = Collections.emptyMap();
            } else {
                assignedPartitions = toTopicPartitionMap(
                    ConsumerProtocol.deserializeConsumerProtocolAssignment(
                        ByteBuffer.wrap(classicGroupMember.assignment())
                    ),
```

**How to spot**
When reviewing group-coordinator/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17733: protocol upgrade should allow empty member assignment in group conversion (#17853).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
