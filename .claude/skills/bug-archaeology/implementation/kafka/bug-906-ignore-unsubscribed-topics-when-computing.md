**Commit**: da4fbba279 · **Ticket**: KAFKA-19468
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When the group coordinator is processing a heartbeat from a share
consumer, it must decide whether the recompute the assignment. Part of
this decision hinges on whether the assigned partitions match the
partitions initialised by the share coordinator. However, when the set
of subscribed topics changes, there may be initialised partitions which
are not currently assigned. Topics which are not subscribed should be
omitted from the calculation about whether to recompute the assignment.

**Before/After**
```java
// Before
    private boolean initializedAssignmentPending(ShareGroup group) {
        if (!shareGroupStatePartitionMetadata.containsKey(group.groupId())) {
        if (group.isEmpty()) {
            // No members then no point of computing assignment.
        // We need to check if all the group initialized share partitions are part of the group assignment.
        Map<Uuid, Set<Integer>> initializedTps = stripInitValue(shareGroupStatePartitionMetadata.get(group.groupId()).initializedTopics());
        return !initializedTps.equals(currentAssigned);

// After
    // Visibility for testing
    boolean initializedAssignmentPending(ShareGroup group) {
        if (group.isEmpty()) {
            // No members then no point in computing assignment.
            return false;
        }

        String groupId = group.groupId();

        if (!shareGroupStatePartitionMetadata.containsKey(groupId) ||
            shareGroupStatePartitionMetadata.get(groupId).initializedTopics().isEmpty()) {
        Set<String> subscribedTopicNames = group.subscribedTopicNames().keySet();
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19468: ignore unsubscribed topics when computing share assignment (#20101).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
