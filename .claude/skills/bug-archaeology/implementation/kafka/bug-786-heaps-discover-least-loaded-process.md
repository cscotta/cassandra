**Commit**: 6247fd9eb3 · **Ticket**: KAFKA-19478
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The original implementation uses a linear search to find the least
loaded process in O(n), and we can replace this by look-ups in a heap is
O(log(n)), as described below

Active tasks: For active tasks, we can do exactly the same assignment as
in the original algorithm by first building a heap (by load) of all
processes. When we assign a task, we pick the head off the heap, assign
the task to it, update the load, and re-insert it into the heap in
O(log(n)). Standby tasks: For standby tasks, we cannot do this optimization
directly, because of the order in which we assign tasks:

1. We first try to assign task A to a process that previously owned A.

**Before/After**
```java
// Before
        GroupAssignment assignments =  doAssign(groupSpec, topologyDescriber);
        Set<TaskId> activeTasks = taskIds(topologyDescriber, true);
            Set<TaskId> statefulTasks = taskIds(topologyDescriber, false);
        Set<TaskId> ret = new HashSet<>();
        for (String subtopology : topologyDescriber.subtopologies()) {
                int numberOfPartitions = topologyDescriber.maxNumInputPartitions(subtopology);
        for (String subtopology : topologyDescriber.subtopologies()) {
            int numberOfPartitions = topologyDescriber.maxNumInputPartitions(subtopology);

// After
import java.util.ArrayList;
import java.util.PriorityQueue;
        final GroupAssignment assignments =  doAssign(groupSpec, topologyDescriber);
        final Set<TaskId> activeTasks = taskIds(topologyDescriber, true);
            final Set<TaskId> statefulTasks = taskIds(topologyDescriber, false);
        final Set<TaskId> ret = new HashSet<>();
        for (final String subtopology : topologyDescriber.subtopologies()) {
                final int numberOfPartitions = topologyDescriber.maxNumInputPartitions(subtopology);
        for (final String subtopology : t
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19478 [3/n]: use heaps to discover the least loaded process (#20172).

**Test**
Test modified in StickyTaskAssignorTest.java covering the corrected behaviour.
