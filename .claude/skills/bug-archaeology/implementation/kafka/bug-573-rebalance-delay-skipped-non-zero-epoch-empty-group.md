**Commit**: 65a3bd26da · **Ticket**: KAFKA-19926

**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java

**Category**: Logic error in condition

**Root cause**
The initial rebalance delay was scheduled only when `group.groupEpoch() == 0`, meaning a group that had previously rebalanced and reached epoch > 0 would skip the delay even if all current members had left and the group was effectively empty again. Two related issues compounded this: (1) `targetAssignmentEpoch` was reset to 1 only when `assignmentEpoch == 0`, losing the correct base epoch when a non-zero-epoch empty group received new members; (2) the fix changes the guard to `group.isEmpty()` and uses `Math.max(1, group.assignmentEpoch())` to preserve a non-zero base epoch.

**Before**
```java
// Before
boolean isInitialRebalance = (group.groupEpoch() == 0);
if (isInitialRebalance) { scheduleInitialRebalanceDelay(); }
// ...
if (initialDelayActive && group.assignmentEpoch() == 0) {
    targetAssignmentEpoch = 1;
}
```

**After**
```java
// After
if (group.isEmpty()) { scheduleInitialRebalanceDelay(); }
// ...
if (initialDelayActive) {
    targetAssignmentEpoch = Math.max(1, group.assignmentEpoch());
}
```

**How to spot**
In `GroupMetadataManager`, search for the initial-rebalance-delay scheduling call and check whether it is gated on `groupEpoch() == 0` rather than `group.isEmpty()`; the former is incorrect for recycled groups.

**Test**
Create a consumer group, let it rebalance to epoch 2, remove all members, add a new member, and assert that the initial rebalance delay is still applied.
