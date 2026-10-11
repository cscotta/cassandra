# Bug 1085: ConcurrentModificationException When Rescheduling Failed SSTable Deletion Tasks

**Commit**: 7b083a4e08 · **Ticket**: CASSANDRA-10222
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableDeletingTask.java, src/java/org/apache/cassandra/io/sstable/SnapshotDeletingTask.java
**Category**: Race condition / missing synchronization

---

## Root Cause

Both `SSTableDeletingTask` and `SnapshotDeletingTask` stored failed tasks in a `CopyOnWriteArraySet`. The `rescheduleFailedTasks` method iterated over the set with a for-each loop while calling `failedTasks.remove(task)` inside the loop body. Although `CopyOnWriteArraySet` iterates over a snapshot, the remove-during-iteration pattern is fragile and produces a Coverity warning; more importantly, re-submitting self-referential `SSTableDeletingTask` objects via the set meant any task that failed again would re-add itself to the same set, creating subtle double-scheduling risks. Replacing the set with a `ConcurrentLinkedQueue` and using `poll()` in a drain loop is the correct concurrent idiom: it is lock-free, naturally drains exactly once, and eliminates the possibility of duplicate scheduling.

### Before / After

```java
// BEFORE — iterating and removing from CopyOnWriteArraySet
private static final Set<SSTableDeletingTask> failedTasks = new CopyOnWriteArraySet<>();
...
for (SSTableDeletingTask task : failedTasks)
{
    failedTasks.remove(task);
    task.schedule();
}
```

```java
// AFTER — drain with ConcurrentLinkedQueue.poll()
private static final Queue<Runnable> failedTasks = new ConcurrentLinkedQueue<>();
...
Runnable task;
while (null != (task = failedTasks.poll()))
    ScheduledExecutors.nonPeriodicTasks.submit(task);
```

---

## How to Spot

Any `for-each` loop that calls `collection.remove(element)` on the same collection being iterated is a code-smell regardless of whether the collection is thread-safe; it signals that the author intended a drain loop but used the wrong idiom. The correct pattern for draining a concurrent collection while submitting tasks is `poll()` in a `while` loop. Review all `reschedule`/`retry` methods that iterate a failed-task collection for this anti-pattern.

---

## Test

No test added.
