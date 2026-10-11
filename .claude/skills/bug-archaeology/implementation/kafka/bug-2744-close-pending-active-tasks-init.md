**Commit**: 25230b5388 · **Ticket**: KAFKA-10199
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
With enabled state updater tasks that are created but not initialized
are stored in a set. In each poll iteration the stream thread drains
that set, intializes the tasks, and adds them to the state updater. On partition lost, all active tasks are closed. This commit ensures that active tasks pending initialization in
the set mentioned above are closed cleanly on partition lost.

**Before/After**
```java
// Before
        removeLostActiveTasksFromStateUpdater();
    private void removeLostActiveTasksFromStateUpdater() {
            final Set<Task> tasksToCloseClean = new HashSet<>();

// After
        removeLostActiveTasksFromStateUpdaterAndPendingTasksToInit();
    private void removeLostActiveTasksFromStateUpdaterAndPendingTasksToInit() {
            final Set<Task> tasksToCloseClean = new HashSet<>(tasks.drainPendingActiveTasksToInit());
import java.util.Iterator;
    @Override
    public Set<Task> drainPendingActiveTasksToInit() {
        final Set<Task> result = new HashSet<>();
        final Iterator<Task> iterator = pendingTasksToInit.iterator();
        while (iterator.hasNext()) {
            final Task task = iterator.next();
            if (task.isActive()) {
                result.add(task);
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10199: close pending active tasks to init on partitions lost (#16545) (#16550).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
