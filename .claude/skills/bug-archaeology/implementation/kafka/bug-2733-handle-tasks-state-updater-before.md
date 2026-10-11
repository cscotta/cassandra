**Commit**: d7fbdcfd83 · **Ticket**: KAFKA-17085
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
When a active tasks are revoked they land as suspended tasks in the
task registry. If they are then reassigned, the tasks are resumed
and put into restoration. On assignment, we first handle the tasks
in the task registry and then the tasks in the state updater. That
means that if a task is re-assigned after a revocation, we remove
the suspended task from the task registry, resume it, add it
to the state updater, and then remove it from the list of tasks
to create.

**Before/After**
```java
// Before
        handleRunningAndSuspendedTasks(activeTasksToCreate, standbyTasksToCreate, tasksToRecycle, tasksToCloseClean);

// After
        handleRunningAndSuspendedTasks(activeTasksToCreate, standbyTasksToCreate, tasksToRecycle, tasksToCloseClean);
    @Test
    public void shouldFirstHandleTasksInStateUpdaterThenSuspendedActiveTasksInTaskRegistry() {
        final StreamTask reassignedActiveTask1 = statefulTask(taskId03, taskId03ChangelogPartitions)
            .inState(State.SUSPENDED)
            .withInputPartitions(taskId03Partitions).build();
        final StreamTask reassignedActiveTask2 = statefulTask(taskId02, taskId02ChangelogPartitions)
            .inState(State.RESTORING)
            .withInputPartitions(taskId02Partitions).build();
        final TasksRegistry tasks = mock(TasksRegistry.class);
        final TaskManager taskManager = setUpTaskManager(ProcessingMode.AT_LEAST_ONCE, tasks, true);
        when(tasks.allTasks()).thenReturn(mkSet(reassignedActiveTask1));
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17085: handle tasks in state updater before tasks in task registry (#16555) (#16561).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
