**Commit**: 22db7b9f4c · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Replaced more tests in `TaskManagerTest.java`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
        final Map<TaskId, Set<TopicPartition>> assignment = mkMap(
            mkEntry(taskId00, taskId00Partitions)
        );
        final Task task00 = new StateMachineTask(taskId00, taskId00Partitions, true, stateManager) {
            @Override
            public void completeRestoration(final java.util.function.Consumer<Set<TopicPartition>> offsetResetter) {
                throw new TimeoutException("timeout!");
            }
        };

        when(activeTaskCreator.createTasks(any(), eq(assignment))).thenReturn(singletonList(task00));
        taskManager.handleAssignment(assignment, emptyMap());

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
        final StreamTask task00 = statefulTask(taskId00, taskId00ChangelogPartitions)
            .withInputPartitions(taskId00Partitions)
            .inState(State.RESTORING)
            .build();
        final TasksRegistry tasks = mock(TasksRegistry.class);
        final TaskManager taskManager = setUpTaskManagerWithStateUpdater(ProcessingMode.AT_LEAST_ONCE, tasks);
        when(stateUpdater.restoresActiveTasks()).thenReturn(true);
        when(stateUpdater.drainRestoredActiveTasks(any(Duration.class))).thenReturn(Set.of(task00));
        final TimeoutException timeoutException = new TimeoutException("timeout!");
        doThrow(timeoutException).when(task00).completeRestoration(any());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: more test replacements [5/n] (#20818).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
