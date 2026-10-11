# Bug 0626: Race Causes compactingCF Counter to Grow Without Bound When Executor Is Full

**Commit**: 35e6d61361 · **Ticket**: CASSANDRA-13801
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/CompactionManager.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`submitBackground` tracked how many compaction tasks were in flight for each `ColumnFamilyStore` by incrementing `compactingCF` before submitting the task, then checking `fut.isCancelled()` afterward to decide whether to keep the entry. The problem is a race window: the count is added before the future is created, so when the executor is at capacity and cancels the task immediately, the increment had already happened and the subsequent `remove` was not executed because the code only removed on non-cancellation. Over time (or under a Byteman-induced delay), the count for a CFS grew higher than the true number of running tasks, causing the throttle check `count > 0 && executor.getActiveCount() >= executor.getMaximumPoolSize()` to permanently block new submissions for that table even after all tasks finished.

### Before / After

```java
// BEFORE – increment before submit; no remove on cancellation
Future<?> fut = executor.submitIfRunning(new BackgroundCompactionCandidate(cfs), "background task");
if (!fut.isCancelled()) {
    compactingCF.add(cfs);   // never removed if fut was cancelled
    futures.add(fut);
}
```

```java
// AFTER – increment in constructor (before work starts); remove if cancelled
Future<?> fut = executor.submitIfRunning(new BackgroundCompactionCandidate(cfs), "background task");
if (!fut.isCancelled())
    futures.add(fut);
else
    compactingCF.remove(cfs);  // undo the add done in constructor

// In BackgroundCompactionCandidate constructor:
BackgroundCompactionCandidate(ColumnFamilyStore cfs) {
    compactingCF.add(cfs);     // now coupled to object lifecycle
    this.cfs = cfs;
}
```

---

## How to Spot

Whenever a counter is incremented before an operation that may fail or cancel, and the roll-back path does not unconditionally decrement on every failure branch, the counter can leak upward. Reviewers should trace every exit path from the critical section and confirm that every increment has a matching decrement on the failure/cancellation path.

---

## Test

`CompactionsBytemanTest.testCompactingCFCounting` uses Byteman to inject a 1-second sleep between task submission and the `isCancelled()` check, then asserts the `compactingCF` count returns to zero after the futures complete.
