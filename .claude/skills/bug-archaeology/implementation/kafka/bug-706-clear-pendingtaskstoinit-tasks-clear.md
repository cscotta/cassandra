**Commit**: d1b160b695 · **Ticket**: KAFKA-19510
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Clear pendingTasksToInit on tasks clear. It matters in situations when
we shutting down a thread in PARTITIONS_ASSIGNED state. In this case we
may have locked some unassigned task directories (see
TaskManager#tryToLockAllNonEmptyTaskDirectories). Then we may have
gotten assigned to one or multiple of those tasks.

**Before/After**
```java
// Before
// (no removals)

// After
        pendingTasksToInit.clear();
        pendingActiveTasksToCreate.clear();
        pendingStandbyTasksToCreate.clear();

    Map<TaskId, Set<TopicPartition>> pendingActiveTasksToCreate() {
        return pendingActiveTasksToCreate;
    }

    Map<TaskId, Set<TopicPartition>> pendingStandbyTasksToCreate() {
        return pendingStandbyTasksToCreate;
    }

```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19510: clear pendingtaskstoinit on tasks clear. (#20646).

**Test**
Test modified in TasksTest.java covering the corrected behaviour.
