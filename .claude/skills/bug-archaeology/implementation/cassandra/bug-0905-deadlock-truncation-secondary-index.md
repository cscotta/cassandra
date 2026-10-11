# Bug 0905: Deadlock on Truncation with Secondary Index

**Commit**: 2837ec65b9 · **Ticket**: n/a
**Subsystem**: ColumnFamilyStore / Truncation
**Category**: Deadlock

---

## Root Cause
The `truncateBlocking` method in `ColumnFamilyStore` held the `synchronized (data)` lock across the entire truncation sequence, including a call to `forceBlockingFlush()` inside the lock. `concatWithIndexes()` (called to compute `truncatedAt`) iterates over secondary index CFS instances. Those index CFS instances can also attempt to acquire the same monitor during their own flush or write operations, creating a cycle: the primary table holds `data` and waits on a flush that the index table needs the monitor to proceed. The fix moves only the non-durable-writes flush path inside `synchronized (data)` (required to atomically enqueue the flush), extracts `FBUtilities.waitOnFuture(...)` to outside the lock, and moves the `truncatedAt` computation and the SSTable max-age scan entirely out of the synchronized block.

### Before / After
```java
// BEFORE
synchronized (data) {
    if (keyspace.getMetadata().durableWrites || takeSnapshot) {
        replayAfter = forceBlockingFlush();
    } else {
        final Flush flush = new Flush(true);
        flushExecutor.execute(flush);
        replayAfter = FBUtilities.waitOnFuture(postFlushExecutor.submit(flush.postFlush));
    }
    // truncatedAt computation (iterates indexes) still inside lock
    ...
}

// AFTER
if (keyspace.getMetadata().durableWrites || takeSnapshot) {
    replayAfter = forceBlockingFlush();           // no lock held
} else {
    Future<ReplayPosition> replayAfterFuture;
    synchronized (data) {                         // lock only for enqueue
        final Flush flush = new Flush(true);
        flushExecutor.execute(flush);
        replayAfterFuture = postFlushExecutor.submit(flush.postFlush);
    }
    replayAfter = FBUtilities.waitOnFuture(replayAfterFuture);  // wait outside lock
}
// truncatedAt computation outside lock
```

---

## How to Spot
Be suspicious whenever a `synchronized` block contains both a blocking wait (flush, future.get, latch.await) and a call that iterates over associated objects that share the same lock hierarchy. If secondary indexes are involved, check whether the index CFS can also try to acquire the primary CFS monitor during the blocked operation. Prefer narrowing the synchronized region to only the state mutation that needs atomicity, and doing any blocking work outside the lock.

---

## Test
No test added.
