**Commit**: ff010a402d · **Ticket**: KAFKA-20269
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Refactor the streams target assignment update method to return both the
target assignment epoch and target assignment. When assignment batching
or assignment offload are implemented, the target assignment update
method may return the last target assignment, depending on timings and
the group coordinator config. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, David Jacot
 <djacot@confluent.io>

**Before/After**
```java
// Before
        int targetAssignmentEpoch;
        TasksTuple targetAssignment;
        if (groupEpoch > group.assignmentEpoch()) {
            boolean initialDelayActive = timer.isScheduled(streamsInitialRebalanceKey(groupId));
            if (initialDelayActive) {
                // During initial rebalance delay, return empty assignment to first joining members.
                targetAssignmentEpoch = Math.max(1, group.assignmentEpoch());
                targetAssignment = TasksTuple.EMPTY;

                returnedStatus.add(
                    new Status()
                        .setStatusCode(StreamsGroupHeartbeatResponse.Status.ASSIGNMENT_DELAYED.code())

// After

        private static UpdateTargetAssignmentResult<TasksTuple> fromLastTargetAssignment(
            StreamsGroup group,
            Optional<StreamsGroupMember> member
        ) {
            if (member.isPresent()) {
                return new UpdateTargetAssignmentResult<>(
                    group.assignmentEpoch(),
                    group.targetAssignment(member.get().memberId())
                );
            } else {
                return new UpdateTargetAssignmentResult<>(
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20269 [3/n]: refactor assignment update for delayed streams assignments (#21696).

**Test**
No test added.
