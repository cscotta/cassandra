**Commit**: 36f17cfee5 · **Ticket**: KAFKA-19765
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
## What
Ticket: https://issues.apache.org/jira/browse/KAFKA-19765
In KIP-1071, there are configurations that affect the assignment, and
that can be configured dynamically. - The previous assignment config of streams group is stored as part of
the StreamsGroupMetadata as a collection of key-value pairs. - If the assignment configuration is changed, group epoch is also
bumped. Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
            records.add(newStreamsGroupMetadataRecord(groupId, groupEpoch, metadataHash, validatedTopologyEpoch));
                records
        List<CoordinatorRecord> records
        Map<String, String> assignmentConfigs = streamsGroupAssignmentConfigs(group.groupId());
        records.add(newStreamsGroupMetadataRecord(group.groupId(), groupEpoch, group.metadataHash(), group.validatedTopologyEpoch()));
        int validatedTopologyEpoch

// After
        // Check if assignment configurations have changed
        Map<String, String> currentAssignmentConfigs = streamsGroupAssignmentConfigs(groupId);
        Map<String, String> storedAssignmentConfigs = group.lastAssignmentConfigs();
        if (!bumpGroupEpoch && !currentAssignmentConfigs.equals(storedAssignmentConfigs)) {
            log.info("[GroupId {}][MemberId {}] Assignment configurations changed to {}. Triggering rebalance.",
                groupId, memberId, currentAssignmentConfigs);
            bumpGroupEpoch = true;
        }

            records.add(newStreamsGroupMetadataRecord(groupId, groupEpoch, metadataHash, validatedTopologyEpoch, currentAssignmentConfigs));
                records,
                currentAssignmentConfigs
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19765: store the last used assignment configuration in the group metadata (#20702).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
