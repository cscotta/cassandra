**Commit**: b043ca2074 · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This is the first part of cleaning up of the tests in `TaskManagerTest`
- Removed dead tests
- Added new tests as suggested earlier

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
    @Test
    public void shouldClassifyExistingTasksWithoutStateUpdater() {
        final TaskManager taskManager = setUpTaskManager(ProcessingMode.AT_LEAST_ONCE, false);
        final Map<TaskId, Set<TopicPartition>> runningActiveTasks = mkMap(mkEntry(taskId01, Set.of(t1p1)));
        final Map<TaskId, Set<TopicPartition>> standbyTasks = mkMap(mkEntry(taskId02, Set.of(t2p2)));
        final Map<TaskId, Set<TopicPartition>> restoringActiveTasks = mkMap(mkEntry(taskId03, Set.of(t1p3)));
        final Map<TaskId, Set<TopicPartition>> activeTasks = new HashMap<>(runningActiveTasks);
        activeTasks.putAll(restoringActiveTasks);
        handleAssignment(runningActiveTasks, standbyTasks, restoringActiveTasks);

        taskManager.handleAssignment(activeTasks, standbyTasks);


// After
    private final TopicPartition t1p1changelog2 = new TopicPartition("changelog2", 1);
    private TaskManager setUpTaskManager(final ProcessingMode processingMode, final TasksRegistry tasks) {
        return setUpTaskManager(processingMode, tasks, false);
    }

    public void shouldComputeOffsetSumForRunningStatefulTask() {
        final StreamTask runningStatefulTask = statefulTask(taskId00, taskId00ChangelogPartitions)
            .inState(State.RUNNING).build();
        final long changelogOffsetOfRunningTask = Task.LATEST_OFFSET;
        when(runningStatefulTask.changelogOffsets())
            .thenReturn(mkMap(mkEntry(t1p0changelog, changelogOffsetOfRunningTask)));
        final TasksRegistry tasks = mock(TasksRegistry.class);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: remove dead tests and modify tests in taskmanagertest [1/n] (#20501).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
