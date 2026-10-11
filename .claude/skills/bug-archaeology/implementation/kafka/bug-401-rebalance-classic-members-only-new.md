**Commit**: 6ef10a9bbe · **Ticket**: KAFKA-20272
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
When assignment batching or offload are enabled, the target assignment
can lag behind the group epoch. We must only tell classic members in
mixed groups to rejoin when a newer target assignment is available. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        // If the group epoch is greater than the member epoch, there is a new rebalance triggered and the member
        if (group.groupEpoch() > member.memberEpoch() && !member.state().equals(MemberState.UNREVOKED_PARTITIONS)) {
        // 1) The group epoch is bumped so the member need to rejoin to catch up.
        if (member.memberEpoch() < group.groupEpoch() ||
        // The group epoch is greater than the member epoch.
                    .build()))

// After
        // If the target assignment epoch is greater than the member epoch, there is a new rebalance triggered and the member
        if (group.assignmentEpoch() > member.memberEpoch() && !member.state().equals(MemberState.UNREVOKED_PARTITIONS)) {
        // 1) The target assignment epoch is bumped so the member needs to rejoin to catch up.
        if (member.memberEpoch() < group.assignmentEpoch() ||
        // The target assignment epoch is greater than the member epoch.
                    .build())
                .withAssignmentEpoch(11))
    @Test
    public void testClassicGroupSyncToConsumerGroupDuringAssignmentDelay() {
        String groupId = "fooup";
        String memberId1 = Uuid.randomUuid().toString();
        String memberId2 = Uuid.randomUuid().toString();
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20272: rebalance classic members only on new assignment (#21662).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
