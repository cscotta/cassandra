# Bug 1369: StreamTransferTask Race Condition Causes Infinite Loops and Premature SSTable Deletion (2nd Branch)

**Commit**: eeaa3e0123 · **Ticket**: CASSANDRA-7704
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamTransferTask.java
**Category**: Race condition / missing synchronization

---

## Root Cause

This is the 2.0 branch port of the same race condition fixed in bug 1368. `StreamTransferTask` on the 2.0 line also used `ConcurrentHashMap` for its `files` and `timeoutTasks` collections without a unified lock, allowing a timeout callback to race with `complete()` over the same file entry. Additionally, `abort()` lacked any guard against double-invocation, so concurrent calls from the session teardown path could release SSTable references more than once. The per-instance `ScheduledExecutorService` shutdown approach was replaced with per-file future cancellation under a common `synchronized` lock.

### Before / After

```java
// BEFORE
public void abort()
{
    for (OutgoingFileMessage file : files.values())
    {
        file.sstable.releaseReference();  // no guard; can race with complete() or another abort()
    }
    timeoutExecutor.shutdownNow();
}
```

```java
// AFTER
public synchronized void abort()
{
    if (aborted)
        return;        // idempotency guard
    aborted = true;

    for (ScheduledFuture future : timeoutTasks.values())
        future.cancel(false);
    timeoutTasks.clear();

    for (OutgoingFileMessage file : files.values())
        file.sstable.releaseReference();
}
```

---

## How to Spot

Any method that iterates over a shared mutable collection and performs a side-effectful operation (like releasing a reference-counted resource) must hold the same lock used by all other methods that modify or read that collection. The telltale smell is mixing `ConcurrentHashMap` (which gives per-operation atomicity) with multi-step workflows that need transactional atomicity across multiple map operations.

---

## Test

Test updated to verify timeout futures are cancelled when `complete()` races them, and that the session transitions to `WAIT_COMPLETE` after the final file is acknowledged.
