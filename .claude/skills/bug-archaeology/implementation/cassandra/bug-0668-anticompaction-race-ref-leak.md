# Bug 0668: Race Condition and Ref Leak in Anticompaction on Task Submission Failure

**Commit**: e9cc805db1 · **Ticket**: CASSANDRA-13688
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java · AbstractCompactionTask.java · PendingRepairManager.java
**Category**: Resource leak

---

## Root Cause

Two related issues existed in the anticompaction path. First, in `CompactionManager.performAnticompaction`, when the executor rejected the submitted task (returning a cancelled future), the code checked `task.isCancelled()` after submission but did not call `txn.abort()` — leaving the `LifecycleTransaction` open and the associated `Refs<SSTableReader>` unreleased. Second, `PendingRepairManager.CleanupTask.execute()` called `removeSession()` unconditionally in a `finally` block appended by a decorator method — which meant a partial metadata rewrite failure still removed the session before all SSTable metadata had been updated. The fix wraps the metadata rewrite in its own try/finally, sets a `completed` flag, and only calls `removeSession` when all rewrites succeeded.

### Before / After

```java
// BEFORE — txn not aborted when task is cancelled
ListenableFutureTask<?> task = ListenableFutureTask.create(runnable, null);
try {
    executor.submitIfRunning(task, "pending anticompaction");
    return task;
} finally {
    if (task.isCancelled()) sstables.release();  // txn.abort() missing
}
```

```java
// AFTER — both refs and transaction cleaned up
ListenableFuture<?> task = null;
try {
    task = executor.submitIfRunning(runnable, "pending anticompaction");
    return task;
} finally {
    if (task == null || task.isCancelled()) {
        sstables.release();
        txn.abort();           // transaction properly closed
    }
}
```

---

## How to Spot

When a resource pair (reference count + lifecycle transaction) must be released together, releasing only one on error paths creates a ref leak. Review every early-exit and exception path from the point resources are acquired to confirm both halves of the pair are always released in tandem.

---

## Test

A new `missingParentRepairSession` test in `AntiCompactionTest` verifies that when anticompaction fails due to a missing parent session, the transaction is aborted and the refs are released.
