**Commit**: 0035ac06d3 · **Ticket**: KAFKA-17489
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Failed tasks discovered when removed from the state updater during assignment or revocation are added to the task registry. From there they are retrieved and handled as normal tasks. This leads to a couple of IllegalStateExceptions because it breaks some invariants that ensure that only good tasks are assigned and processed. This commit solves this bug by distinguish failed from non-failed tasks in the task registry.

**Before/After**
```java
// Before
            final Set<TopicPartition> partitionsNotToPause = tasks.allTasks()
        for (final Task task : tasks.allTasks()) {
            tasks.addTask(task);
                tasks.addTask(task);
            tasks.addTask(failedTask);

// After
            final Set<TopicPartition> partitionsNotToPause = tasks.allNonFailedTasks()
        for (final Task task : tasks.allNonFailedTasks()) {
            tasks.addFailedTask(task);
                tasks.addFailedTask(task);
            tasks.addFailedTask(failedTask);
    private final Set<TaskId> failedTaskIds = new HashSet<>();
    @Override
    public void addFailedTask(final Task task) {
        failedTaskIds.add(task.id());
        addTask(task);
    }

```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17489: do not handle failed tasks as tasks to assign (#17115).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
