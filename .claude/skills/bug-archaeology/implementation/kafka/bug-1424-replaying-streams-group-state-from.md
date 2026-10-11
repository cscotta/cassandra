**Commit**: d0c65a1fd2 · **Ticket**: KAFKA-18730
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Adds streams group to the GroupMetadataManager, and implements loading
the records from the offset topic into state. The state also contains
two timers (rebalance timeout and session timeout) that are started
after the group coordinator has been loaded. Reviewers: Bruno Cadonna <bruno@confluent.io>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
              files="(ConsumerGroupMember|GroupMetadataManager|GeneralUniformAssignmentBuilder|GroupCoordinatorRecordSerde).java"/>
              files="(GroupMetadataManager|ConsumerGroupTest|ShareGroupTest|GroupMetadataManagerTest|GroupMetadataManagerTestContext|GeneralUniformAssignmentBuilder).java"/>
     * The classic and consumer group size counter key to schedule a timer task.

// After
              files="(ConsumerGroupMember|GroupMetadataManager|GeneralUniformAssignmentBuilder|GroupCoordinatorRecordSerde|GroupMetadataManagerTestContext).java"/>
              files="(GroupMetadataManager|ConsumerGroupTest|ShareGroupTest|GroupMetadataManagerTest|GroupMetadataManagerTestContext|GeneralUniformAssignmentBuilder|GroupCoordinatorShard).java"/>
import org.apache.kafka.coordinator.group.generated.StreamsGroupCurrentMemberAssignmentKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupCurrentMemberAssignmentValue;
import org.apache.kafka.coordinator.group.generated.StreamsGroupMemberMetadataKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupMemberMetadataValue;
import org.apache.kafka.coordinator.group.generated.StreamsGroupMetadataKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupMetadataValue;
import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupPartitionMetadataValue;
import org.apache.kafka.coordinator.group.generated.StreamsGroupTargetAssignmentMemberKey;
import org.apache.kafka.coordinator.group.generated.StreamsGroupTargetAssignmentMemberValue;
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18730: add replaying streams group state from offset topic (#18809).

**Test**
Test modified in GroupCoordinatorShardTest.java covering the corrected behaviour.
