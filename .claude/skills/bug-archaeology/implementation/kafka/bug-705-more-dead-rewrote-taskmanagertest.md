**Commit**: 59f51fb3ca · **Ticket**: KAFKA-19683
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Changes made
- Additional `setUpTaskManager()` overloaded method -- Created this
temporarily to pass the CI pipelines so that I can work on the failing
tests incrementally
- Rewrote 3 tests to use stateUpdater thread

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
        taskManager = setUpTaskManager(StreamsConfigUtils.ProcessingMode.AT_LEAST_ONCE, null, false);
    private TaskManager setUpTaskManager(final ProcessingMode processingMode, final TasksRegistry tasks) {
        return setUpTaskManager(processingMode, tasks, false);
    private TaskManager setUpTaskManager(final ProcessingMode processingMode, final boolean stateUpdaterEnabled) {
        return setUpTaskManager(processingMode, null, stateUpdaterEnabled, false);
    }

    private TaskManager setUpTaskManager(final ProcessingMode processingMode, final TasksRegistry tasks, final boolean stateUpdaterEnabled) {
        return setUpTaskManager(processingMode, tasks, stateUpdaterEnabled, false);
    private TaskManager setUpTaskManager(final ProcessingMode processingMode,

// After
        taskManager = setUpTaskManagerWithoutStateUpdater(StreamsConfigUtils.ProcessingMode.AT_LEAST_ONCE, null, false);
    private TaskManager setUpTaskManagerWithStateUpdater(final ProcessingMode processingMode, final TasksRegistry tasks) {
        return setUpTaskManagerWithStateUpdater(processingMode, tasks, false);
    private TaskManager setUpTaskManagerWithStateUpdater(final ProcessingMode processingMode,
                                                         final TasksRegistry tasks,
                                                         final boolean processingThreadsEnabled) {
        topologyMetadata = new TopologyMetadata(topologyBuilder, new DummyStreamsConfig(processingMode));
        final TaskManager taskManager = new TaskManager(
            time,
            changeLogReader,
            ProcessId.randomProcessId(),
            "taskManagerTest",
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19683: remove more dead tests and rewrote 3 tests in taskmanagertest [2/n] (#20544).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
