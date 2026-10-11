**Commit**: bd0c628cfc · **Ticket**: KAFKA-19994
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
When a TimeoutException occurs while trying to put multiple active tasks
back   into running, we will add the timed out task back to the state
updater, so that   we retry it. However, if we run into a Task timeout (failing to make progress for a
long   time), we will rethrow a StreamsException wrapping the
TimeoutException we   have drained multiple tasks from the state
updater, they will be lost, and   not added back to the state updater,
and therefore not be closed correctly. The task directories remain
locked, causing issues trying to replace the   stream thread. Reviewers: Matthias J.

**Before/After**
```java
// Before
                                              final java.util.function.Consumer<Set<TopicPartition>> offsetResetter) {
        for (final Task task : stateUpdater.drainRestoredActiveTasks(timeout)) {
            transitRestoredTaskToRunning(task, now, offsetResetter);

// After
import java.util.LinkedHashSet;
    /**
     * @throws StreamsException if fetching committed offsets timed out often enough to exceed task timeout
     */
                                              final java.util.function.Consumer<Set<TopicPartition>> offsetResetter) throws StreamsException {
        // Create a mutable copy to support iterator.remove()
        final Set<StreamTask> restoredTasks = new LinkedHashSet<>(stateUpdater.drainRestoredActiveTasks(timeout));
        final Iterator<StreamTask> iterator = restoredTasks.iterator();

        try {
            while (iterator.hasNext()) {
                final Task task = iterator.next();
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19994: taskmanager may not close all tasks on task timeouts (#21155).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
