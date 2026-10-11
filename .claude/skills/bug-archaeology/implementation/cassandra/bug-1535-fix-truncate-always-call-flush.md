# Bug 1535: Fix Truncate to Always Call Flush on Table

**Commit**: 60eab4e45e · **Ticket**: CASSANDRA-7511
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java, DataTracker.java
**Category**: State not cleaned up

---

## Root Cause

`ColumnFamilyStore.truncateBlocking()` only called `forceBlockingFlush()` when `auto_snapshot` was enabled. When `auto_snapshot` was disabled, in-memory memtable data was discarded by swapping in a fresh `Memtable` via `renewMemtable()` without writing to disk. This left the commit log still referencing the old (now-discarded) data: if a node crashed and replayed the commit log it would re-apply mutations that had been truncated, silently corrupting the table. The fix removes the conditional and always flushes before the truncate, ensuring commit log segments containing pre-truncation data are fully persisted and can be discarded.

### Before / After

```java
// BEFORE
if (DatabaseDescriptor.isAutoSnapshot())
{
    forceBlockingFlush();
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
}
// else: silently drop memtable in-place, leaving commit log dirty
Keyspace.switchLock.writeLock().lock();
try {
    for (ColumnFamilyStore cfs : concatWithIndexes()) {
        Memtable mt = cfs.getMemtableThreadSafe();
        if (!mt.isClean())
            mt.cfs.data.renewMemtable();
    }
} finally { Keyspace.switchLock.writeLock().unlock(); }
```

```java
// AFTER
forceBlockingFlush();
Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
// renewMemtable() code path removed entirely
```

---

## How to Spot

Any code that discards in-memory write state (memtables, write buffers) without ensuring the corresponding durable log is also cleaned up or flushed is a candidate for this pattern. Truncation and compaction paths should always flush before mutating durable state.

---

## Test

`CommitLogTest.testTruncateWithoutSnapshot` — disables `auto_snapshot`, writes a mutation, truncates, fills a new commit log segment, then asserts that old segments are discarded (active segment count drops from 2 to 1).
