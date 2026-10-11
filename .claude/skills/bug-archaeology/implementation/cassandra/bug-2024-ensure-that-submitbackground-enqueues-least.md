# Bug 2024: Compaction Stops Permanently When All Executor Slots Are Full at submitBackground Time

**Commit**: 3b41d21d9b · **Ticket**: CASSANDRA-5554
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

`submitBackground` used a `while` loop that checked `executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize()` before scheduling even the first task. If the executor was already at capacity at the moment of the call, the loop body never executed and zero tasks were enqueued. Since compaction is only triggered again on a flush, a column family that hit this race could stop compacting indefinitely until its next memtable flush.

The fix changes the `while` to a `do…while`, guaranteeing at least one task is always submitted regardless of current executor occupancy.

### Before / After

```java
// BEFORE
while (executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize())
{
    futures.add(executor.submit(new BackgroundCompactionTask(cfs)));
    compactingCF.add(cfs);
}
```

```java
// AFTER
// we must schedule it at least once, otherwise compaction will stop for a CF until next flush
do {
    futures.add(executor.submit(new BackgroundCompactionTask(cfs)));
    compactingCF.add(cfs);
    // if we have room for more compactions, then fill up executor
} while (executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize());
```

---

## How to Spot

Watch for `while (condition) { enqueue(); }` patterns where the loop body is the only place something gets enqueued and the condition can be false on entry. If the caller never retries independently (e.g., depends on an external event like a flush), a false-on-entry condition starves the work forever. Prefer `do…while` when at least one iteration must always occur.

---

## Test

No test added.
