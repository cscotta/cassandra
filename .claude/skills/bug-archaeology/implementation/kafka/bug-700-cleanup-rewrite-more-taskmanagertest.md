**Commit**: 225660a43d · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
Cleanup and rewrote more tests in `TaskManagerTest.java`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
    public void shouldPauseAllTopicsWithoutStateUpdaterOnRebalanceComplete() {

    public void shouldNotPauseReadyTasksWithStateUpdaterOnRebalanceComplete() {
        expectLockObtainedFor(taskId00, taskId01, taskId02);
        expectDirectoryNotEmpty(taskId00, taskId01, taskId02);

        makeTaskFolders(
            taskId00.toString(),  // active task
            taskId01.toString(),  // standby task
            taskId02.toString()   // unassigned but able to lock
        );
        taskManager.handleRebalanceStart(singleton("topic"));

// After
import static org.mockito.Mockito.atLeastOnce;
    public void shouldPauseAllTopicsOnRebalanceComplete() {
        final TaskManager taskManager = setUpTaskManagerWithStateUpdater(ProcessingMode.AT_LEAST_ONCE, null);
    public void shouldNotPauseReadyTasksOnRebalanceComplete() {
        final Map<TopicPartition, Long> changelogOffsets = mkMap(
            mkEntry(t1p0changelog, changelogOffsetOfRunningTask)
        );
        when(runningStatefulTask.changelogOffsets()).thenReturn(changelogOffsets);
        final TaskManager taskManager = setUpTaskManagerWithStateUpdater(ProcessingMode.AT_LEAST_ONCE, tasks);
        final StreamTask restoringStatefulTask = statefulTask(taskId00, taskId00ChangelogPartitions)
            .inState(State.RESTORING).build();
        final Map<TaskId, Long> expectedOffsetSums = mkMap(
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: cleanup and rewrite more tests in taskmanagertest [3/n] (#20692).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
