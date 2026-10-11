**Commit**: 351203873d · **Ticket**: KAFKA-19661
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
In the original algorithm, standby tasks are assigned to a  process that
previously owned the task only if it is  “load-balanced”, meaning the
process has fewer tasks that  members, or it is the least loaded
process. This is strong  requirement, and will cause standby tasks to
often not get  assigned to process that previously owned it. Furthermore,  the condition “is the least loaded process” is hard to
evaluate efficiently in this context. We propose to instead use the same “below-quota” condition  as in active
task assignment.

**Before/After**
```java
// Before
    private int activeTaskCount;
    public int activeTaskCount() {
        return activeTaskCount;
    }

            activeTaskCount += 1;
        //standby
        final int numStandbyReplicas =
            groupSpec.assignmentConfigs().isEmpty() ? 0
                : Integer.parseInt(groupSpec.assignmentConfigs().get("num.standby.replicas"));
        if (numStandbyReplicas > 0) {
            assignStandby(statefulTasks, numStandbyReplicas);

// After
        if (localState.numStandbyReplicas > 0) {
            assignStandby(statefulTasks);
        localState.numStandbyReplicas =
            groupSpec.assignmentConfigs().isEmpty() ? 0
                : Integer.parseInt(groupSpec.assignmentConfigs().get("num.standby.replicas"));

        // Helpers for computing active tasks per member, and tasks per member
        localState.totalActiveTasks = 0;
        localState.totalTasks = 0;
            localState.totalTasks += numberOfPartitions;
            localState.totalActiveTasks += numberOfPartitions;
            if (topologyDescriber.isStateful(subtopology))
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19661 [5/n]: use below-quota as a condition for standby task assignment (#20458).

**Test**
Test modified in StickyTaskAssignorTest.java covering the corrected behaviour.
