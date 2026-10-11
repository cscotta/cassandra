# Bug 2025: Compaction Stops Permanently When All Executor Slots Are Full at submitBackground Time

**Commit**: 740e344c7f · **Ticket**: CASSANDRA-5554
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

This is a backport of the same fix as CASSANDRA-5554 to a different branch. `submitBackground` used a `while` loop whose guard expression could evaluate to false before the first iteration, causing no compaction task to be submitted. Because compaction for a column family is only re-triggered by a flush, a column family that hit this condition stopped compacting until its next flush event.

Changing the `while` loop to a `do…while` ensures at least one `BackgroundCompactionTask` is always submitted, breaking the stall.

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
do {
    futures.add(executor.submit(new BackgroundCompactionTask(cfs)));
    compactingCF.add(cfs);
} while (executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize());
```

---

## How to Spot

Any `while`-guarded enqueue loop where the loop body is the sole submission path and the caller depends on an external event to retry is a candidate for this bug. A `do…while` is the correct idiom whenever at least one unit of work must always be enqueued.

---

## Test

No test added.
