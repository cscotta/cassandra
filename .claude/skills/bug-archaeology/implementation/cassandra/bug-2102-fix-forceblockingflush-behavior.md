# Bug 2102: forceBlockingFlush Returns Immediately Even When a Flush Is In Progress

**Commit**: 86063aa208 · **Ticket**: CASSANDRA-5241
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When `forceBlockingFlush` was called and the memtable appeared clean (no unflushed data), it returned `Futures.immediateCheckedFuture(null)`. However, a flush triggered just before the check could still be in progress on the `postFlushExecutor`. A caller awaiting the returned future would unblock immediately and proceed, even though data written before the call had not yet been durably flushed to disk. The fix submits a no-op task to `postFlushExecutor` instead, ensuring the returned future does not complete until any in-progress flush has finished.

### Before / After

```java
// BEFORE
if (clean) {
    logger.debug("forceFlush requested but everything is clean in {}", columnFamily);
    return Futures.immediateCheckedFuture(null);
}
```

```java
// AFTER
if (clean) {
    return postFlushExecutor.submit(new Runnable() {
        public void run() {
            logger.debug("forceFlush requested but everything is clean in {}", columnFamily);
        }
    });
}
```

---

## How to Spot

Any "nothing to do, return immediately" fast path that bypasses an executor queue should be scrutinised for TOCTOU races: if work submitted to the queue before the check is still running, the fast path breaks the ordering guarantee the caller expects. The safe pattern is to enqueue a fence task rather than return a pre-completed future.

---

## Test

No test added.
