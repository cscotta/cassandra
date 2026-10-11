# Bug 0605: Batch Commit Log Sync Does Not Set syncRequested Flag

**Commit**: 00c90c16e9 · **Ticket**: CASSANDRA-14292
**Subsystem**: `src/java/org/apache/cassandra/db/commitlog/BatchCommitLogService.java`, `src/java/org/apache/cassandra/db/commitlog/AbstractCommitLogService.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`BatchCommitLogService.maybeWaitForSync` called `haveWork.release()` directly to wake the sync thread but did not set the `syncRequested` flag. The sync loop in `AbstractCommitLogService` only performs a full fsync when `syncRequested` is true; without the flag, the woken thread could execute a no-op iteration and the mutation's `awaitDiskSync` could block for up to the full `commitlog_sync_batch_window_in_ms` timeout (default 2 seconds) rather than returning immediately. The fix extracts the two operations — `syncRequested = true` and `haveWork.release(1)` — into a new `requestSync()` method and uses it in both `BatchCommitLogService.maybeWaitForSync` and `AbstractCommitLogService.requestExtraSync`, ensuring that waking the sync thread always sets the flag atomically within the same call.

### Before / After

```java
// BEFORE — BatchCommitLogService only releases semaphore, not the flag
pending.incrementAndGet();
haveWork.release();  // syncRequested never set
alloc.awaitDiskSync(commitLog.metrics.waitingOnCommit);
```

```java
// AFTER — requestSync() sets flag and releases in one call
protected void requestSync()
{
    syncRequested = true;
    haveWork.release(1);
}

// BatchCommitLogService:
pending.incrementAndGet();
requestSync();
alloc.awaitDiskSync(commitLog.metrics.waitingOnCommit);
```

---

## How to Spot

When a semaphore release is used to signal a worker loop that checks a separate boolean flag before doing real work, both operations must be performed together. Any caller that releases the semaphore without setting the flag creates a silent no-op wakeup that breaks the expected latency guarantee. Search for direct `haveWork.release()` calls and verify each one also sets `syncRequested`.

---

## Test

`BatchCommitLogTest.testBatchCLSyncImmediately` writes a mutation and asserts that `CommitLog.add` returns in under 1 second (well below the 1-second batch window), confirming that the sync happens promptly rather than waiting for the full window.
