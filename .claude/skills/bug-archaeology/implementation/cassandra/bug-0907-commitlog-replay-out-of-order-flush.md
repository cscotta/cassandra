# Bug 0907: Commit Log Replay Incorrect After Out-of-Order Flush Completion

**Commit**: 849a438690 · **Ticket**: CASSANDRA-9669
**Subsystem**: CommitLog / ColumnFamilyStore flush lifecycle
**Category**: Race condition / missing synchronization

---

## Root Cause
When a memtable flush completes, Cassandra records the highest commit-log replay position covered by that flush so the commit log can safely discard segments up to that point. The original code returned a `null` future from `switchMemtableIfCurrent` when the current memtable had already been swapped out (meaning another flush was in progress), causing callers like `forceFlush` to return a completed future with no position information. If flushes completed out of order — e.g., a later-enqueued flush finished before an earlier one — the commit log could believe an earlier position was covered when it was not, leading to data loss on replay after a crash. The fix changes `switchMemtableIfCurrent` to return a `waitForFlushes()` future when it decides not to switch, which waits in the `postFlushExecutor` queue and therefore completes only after all previously submitted flushes (and their position updates) have finished. Additionally, `PostFlush` is changed from a `Runnable` to a `Callable<ReplayPosition>` so the actual covered position propagates back to callers.

### Before / After
```java
// BEFORE — returns null future when no switch needed
public Future<?> switchMemtableIfCurrent(Memtable memtable) {
    synchronized (data) {
        if (data.getView().getCurrentMemtable() == memtable)
            return switchMemtable();
    }
    return Futures.immediateFuture(null);  // no ordering guarantee
}

// AFTER — waits for all pending flushes to complete
public ListenableFuture<ReplayPosition> switchMemtableIfCurrent(Memtable memtable) {
    synchronized (data) {
        if (data.getView().getCurrentMemtable() == memtable)
            return switchMemtable();
    }
    return waitForFlushes();  // queued on postFlushExecutor, inherits ordering
}
```

---

## How to Spot
Any flush or "wait for flush" helper that returns an immediately-completed future as its "nothing to do" path can break ordering guarantees if the caller depends on preceding flushes having recorded their commit-log positions. During review, check that the "no-op" return from a flush method is not a bare `Futures.immediateFuture(null)` — it should instead be something that still gets serialized through the post-flush executor so it observes the happens-before relationship with prior flushes.

---

## Test
`MetadataSerializerTest` updated to cover both `commitLogLowerBound` and `commitLogUpperBound` in metadata serialization. A `testLaReadsLb` test verifies that stats serialized in the `lb` format are readable as `la` format with identical results.
