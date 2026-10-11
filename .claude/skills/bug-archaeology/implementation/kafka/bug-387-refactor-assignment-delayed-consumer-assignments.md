**Commit**: ccfc37d374 · **Ticket**: KAFKA-20269
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Refactor the consumer target assignment update method to return both the
target assignment epoch and target assignment. When assignment batching
or assignment offload are implemented, the target assignment update
method may return the last target assignment, depending on timings and
the group coordinator config. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        final int targetAssignmentEpoch;
        final Assignment targetAssignment;

        if (groupEpoch > group.assignmentEpoch()) {
            targetAssignment = updateTargetAssignment(
                group,
                groupEpoch,
                member,
                updatedMember,
                subscriptionType,
                records
            );

// After
    private record UpdateTargetAssignmentResult<T>(
        int targetAssignmentEpoch,
        T targetAssignment
    ) {
        private static UpdateTargetAssignmentResult<Assignment> fromLastTargetAssignment(
            ConsumerGroup group,
            ConsumerGroupMember member
        ) {
            return new UpdateTargetAssignmentResult<>(
                group.assignmentEpoch(),
                group.targetAssignment(member.memberId(), member.instanceId())
            );
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20269 [1/n]: refactor assignment update for delayed consumer assignments (#21694).

**Test**
No test added.
