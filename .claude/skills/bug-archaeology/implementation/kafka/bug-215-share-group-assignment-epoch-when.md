**Commit**: 56be35588a · **Ticket**: KAFKA-20442
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
When the last member of a share group is fenced, the group epoch is
bumped but the assignment epoch is not updated. This leaves the empty
group with `groupEpoch > assignmentEpoch`, violating the invariant that
an empty group should have matching epochs. This patch writes a
`ShareGroupTargetAssignmentMetadataRecord` when the fence operation
results in an empty group so that the assignment epoch catches up to the
group epoch. Reviewers: Sushant Mahajan <smahajan@confluent.io>, Sean Quah
 <squah@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                        GroupCoordinatorRecordHelpers.newShareGroupEpochRecord(groupId, 3, 0)
                            GroupCoordinatorRecordHelpers.newShareGroupEpochRecord(groupId, 11, 0)

// After
import static org.apache.kafka.coordinator.group.GroupCoordinatorRecordHelpers.newShareGroupTargetAssignmentMetadataRecord;
        // If this is the last member, the group becomes empty so we must
        // also update the assignment epoch to match the group epoch. We
        // use a timestamp of zero to mimic the behavior of a new group
        // so that the assignment interval does not delay the next
        // assignment computation.
        if (group.members().size() == 1) {
            records.add(newShareGroupTargetAssignmentMetadataRecord(
                group.groupId(), groupEpoch, 0L));
        }

                        GroupCoordinatorRecordHelpers.newShareGroupEpochRecord(groupId, 3, 0),
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20442: update share group assignment epoch when last member leaves (#22046).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
