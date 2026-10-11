**Commit**: 3255a73229 · **Ticket**: KAFKA-19831
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The usage of the futureless remove task was removed in PR #15896  This
PR cleans up the old code that's no longer used. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                            if (taskAndAction.futureForRemove() == null) {
                                removeTask(taskAndAction.taskId());
                            } else {
                                removeTask(taskAndAction.taskId(), taskAndAction.futureForRemove());
                            }
        private void removeTask(final TaskId taskId) {
            final Task task;
            if (updatingTasks.containsKey(taskId)) {
                task = updatingTasks.get(taskId);
                measureCheckpointLatency(() -> task.maybeCheckpoint(true));
                final Collection<TopicPartition> changelogPartitions = task.changelogPartitions();
                changelogReader.unregister(changelogPartitions);

// After
                            removeTask(taskAndAction.taskId(), taskAndAction.futureForRemove());
}
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19831: removed the futureless remove task as it's no longer used. (#20843).

**Test**
No test added.
