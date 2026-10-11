**Commit**: c48c50d3e8 · **Ticket**: KAFKA-19831
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/DefaultStateUpdater.java
**Category**: Resource leak

**Root cause**
Two code paths in `DefaultStateUpdater` called `task.maybeCheckpoint(true)` without catching `StreamsException`: `removeCheckpointForCorruptedTask` and `pauseTask`. If `maybeCheckpoint` threw, the task was neither moved to the failed-tasks queue nor removed from `updatingTasks`, leaving the `DefaultStateUpdater` thread in an inconsistent state. Additionally, `TaskManager.shutdownStateUpdater` did not drain already-failed tasks before issuing remove futures, allowing `shutdownStateUpdater` to hang indefinitely if the state-updater thread had died with unhandled tasks.

**Before/After**
```java
// Before – no try/catch; exception leaves task stranded
private void pauseTask(final Task task) {
    measureCheckpointLatency(() -> task.maybeCheckpoint(true));
    pausedTasks.put(taskId, task);
    updatingTasks.remove(taskId);
    ...
}

// After – catch StreamsException, route to failed-tasks queue
try {
    measureCheckpointLatency(() -> task.maybeCheckpoint(true));
    pausedTasks.put(taskId, task);
    updatingTasks.remove(taskId);
    ...
} catch (final StreamsException streamsException) {
    handleStreamsExceptionWithTask(streamsException, taskId);
}
```

**How to spot**
Any state-machine operation that can throw must be wrapped so that failed tasks end up in the failed-tasks queue rather than silently remaining in `updatingTasks`. Callers of `shutdownStateUpdater` should drain failed tasks first to avoid deadlock.

**Test**
`StateUpdaterFailureIntegrationTest#correctlyHandleFlushErrorsDuringRebalance`: triggers a `ProcessorStateException` during `flush()` at the exact moment `TaskManager.shutdownStateUpdater` is running, and asserts that `streams.close(Duration.ofSeconds(60))` returns `true` (does not hang).
