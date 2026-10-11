**Commit**: 959818f982 · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
Rewrote more tests in `TaskManagerTest.java`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import java.util.concurrent.atomic.AtomicBoolean;
        final TaskManager taskManager = setUpTaskManagerWithoutStateUpdater(ProcessingMode.AT_LEAST_ONCE, tasks, false);
        final TaskManager taskManager = setUpTaskManagerWithoutStateUpdater(ProcessingMode.EXACTLY_ONCE_V2, null, false);
        final TaskManager taskManager = setUpTaskManagerWithoutStateUpdater(processingMode, null, false);

        final TopicPartition changelog = new TopicPartition("changelog", 0);
        final Map<TaskId, Set<TopicPartition>> assignment = mkMap(
            mkEntry(taskId00, taskId00Partitions),
            mkEntry(taskId01, taskId01Partitions),
            mkEntry(taskId02, taskId02Partitions),
            mkEntry(taskId03, taskId03Partitions)
        );

// After
        final TaskManager taskManager = setUpTaskManagerWithStateUpdater(ProcessingMode.AT_LEAST_ONCE, tasks, false);
        final TaskManager taskManager = setUpTaskManagerWithStateUpdater(ProcessingMode.EXACTLY_ONCE_V2, null, false);
        final StreamTask task00 = statefulTask(taskId00, taskId00ChangelogPartitions)
            .inState(State.RUNNING)
            .withInputPartitions(taskId00Partitions).build();
        final StreamTask task01 = statefulTask(taskId01, taskId01ChangelogPartitions)
            .inState(State.RUNNING)
            .withInputPartitions(taskId01Partitions).build();
        final StreamTask task02 = statefulTask(taskId02, taskId02ChangelogPartitions)
            .inState(State.RUNNING)
            .withInputPartitions(taskId02Partitions).build();
        final TasksRegistry tasks = mock(TasksRegistry.class);
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: more test replacements [6/n] (#20858).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
