**Commit**: 904b459dca · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
- Removed tests that were tagged for removal
- Rewrote more tests

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import java.util.Collection;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.fail;
        final ProcessorStateManager stateManager = mock(ProcessorStateManager.class);
        final AtomicBoolean enforcedCheckpoint = new AtomicBoolean(false);
        final StateMachineTask task00 = new StateMachineTask(taskId00, taskId00Partitions, true, stateManager) {
            @Override
            public void postCommit(final boolean enforceCheckpoint) {
                if (enforceCheckpoint) {
                    enforcedCheckpoint.set(true);
                }
                super.postCommit(enforceCheckpoint);

// After
        final StreamTask task00 = statefulTask(taskId00, taskId00ChangelogPartitions)
            .withInputPartitions(taskId00Partitions)
            .inState(State.RUNNING)
            .build();
        final TasksRegistry tasks = mock(TasksRegistry.class);
        when(tasks.task(taskId00)).thenReturn(task00);
        when(tasks.allTasksPerId()).thenReturn(singletonMap(taskId00, task00));
        when(tasks.activeTaskIds()).thenReturn(Set.of(taskId00));
        when(task00.prepareCommit(false)).thenReturn(emptyMap());
        doNothing().when(task00).postCommit(anyBoolean());
        when(task00.changelogPartitions()).thenReturn(taskId00ChangelogPartitions);
        when(consumer.assignment()).thenReturn(taskId00Partitions);
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: more cleanup and rewrite [4/n] (#20777).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
