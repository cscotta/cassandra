**Commit**: 6d16f687aa · **Ticket**: KAFKA-19662
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This PR allows the kafka-share-groups.sh --reset-offsets tool to be used
to set offsets for topics which are not currently subscribed in a share
group. It also works if the share group does not yet exist. This brings
the capability in line with the equivalent function in
Kafka-consumer-groups.sh. The primary purpose is to allow offsets to be
set before the share group is first used as a way of initialising in a
known state.

**Before/After**
```java
// Before
    val fakeGroupId = "fake_group_id"
        // Test the fake group ID
        val fakeAlterResult = client.alterShareGroupOffsets(fakeGroupId, util.Map.of(tp1, 0, tp2, 0))
        assertFutureThrows(classOf[GroupIdNotFoundException], fakeAlterResult.all())
        assertFutureThrows(classOf[GroupIdNotFoundException], fakeAlterResult.partitionResult(tp1))
        assertFutureThrows(classOf[GroupIdNotFoundException], fakeAlterResult.partitionResult(tp2))
     * Make the following checks to make sure the AlterShareGroupOffsetsRequest request is valid:
     * 1. Checks whether the provided group is empty
     * 2. Checks the requested topics are presented in the metadataImage
     * 3. Checks the corresponding share partitions in AlterShareGroupOffsetsRequest are existing
        List<CoordinatorRecord> records = new ArrayList<>();
        ShareGroup group = groupMetadataManager.shareGroup(groupId);

// After
    val nonexistentGroupId = "nonexistent_group_id"
        // Test the non-existent group ID
        val nonexistentAlterResult = client.alterShareGroupOffsets(nonexistentGroupId, util.Map.of(tp1, 0, tp2, 0))
        assertFutureThrows(classOf[UnknownTopicOrPartitionException], nonexistentAlterResult.all())
        assertNull(nonexistentAlterResult.partitionResult(tp1).get())
        assertFutureThrows(classOf[UnknownTopicOrPartitionException], nonexistentAlterResult.partitionResult(tp2))
     * Alters the offsets for a share group.
        return groupMetadataManager.alterShareGroupOffsets(groupId, alterShareGroupOffsetsRequestData.topics());
import org.apache.kafka.common.errors.GroupNotEmptyException;
    /**
     * Checks whether the share group is empty.
     *
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19662: reset share group offsets for unsubscribed topics (#20708).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
