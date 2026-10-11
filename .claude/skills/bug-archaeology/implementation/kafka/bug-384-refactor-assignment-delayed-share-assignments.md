**Commit**: 3dd5dff307 · **Ticket**: KAFKA-20269
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Refactor the share target assignment update method to return both the
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
                updatedMember,
                subscriptionType,
                records
            );
            targetAssignmentEpoch = groupEpoch;

// After

        private static UpdateTargetAssignmentResult<Assignment> fromLastTargetAssignment(
            ShareGroup group,
            ShareGroupMember member
        ) {
            return new UpdateTargetAssignmentResult<>(
                group.assignmentEpoch(),
                group.targetAssignment(member.memberId())
            );
        }
        UpdateTargetAssignmentResult<Assignment> updateTargetAssignmentResult = maybeUpdateTargetAssignment(
            group,
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20269 [2/n]: refactor assignment update for delayed share assignments (#21695).

**Test**
No test added.
