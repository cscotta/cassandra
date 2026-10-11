**Commit**: c8882979a9 · **Ticket**: KAFKA-19779
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
This PR implements assignment epoch tracking for streams groups. Fundamentally, this replaces the representation of current assignments
and pending revocations from Map<String, Set<Integer>> to Map<String,
Map<Integer, Integer>> where the  value of the inner map is the
assignment epoch. Target assignments, and the tasks reported  as
currently owned, still use the former representation, since they do not
include assignment epochs. This change only applies to active tasks.

**Before/After**
```java
// Before
                || !areOwnedTasksContainedInAssignedTasks(ownedActiveTasks, member.assignedTasks().activeTasks())

// After
import org.apache.kafka.coordinator.group.streams.TasksTupleWithEpochs;
     *                      If this is null, it indicates that we do not know which
     *                      tasks are owned by the member, so we return false.
    /**
     * Checks whether all the tasks contained in the list are included in the provided assignment with epochs.
     *
     * @param ownedTasks              The tasks provided by the streams group member in the request.
     *                                If this is null, it indicates that we do not know which
     *                                tasks are owned by the member, so we return false.
     * @param assignedTasksWithEpochs The tasks that the member should have (with epochs).
     * @return A boolean indicating whether the owned partitions are a subset or not.
     */
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19779: track assignment epochs in streams groups [3/n] (#20730).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
