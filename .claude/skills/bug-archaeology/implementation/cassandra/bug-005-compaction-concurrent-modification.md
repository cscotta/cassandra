# Bug 005: Fix ConcurrentModificationException in compaction garbagecollect

**Commit**: 2b1c8395eb · **Ticket**: CASSANDRA-21065
**Subsystem**: Compaction / CompactionManager
**Category**: Race condition / missing synchronization

---

## Root Cause

In `CompactionManager.performGarbageCollection()`, when `only_purge_repaired_tombstones` is enabled, the code iterated directly over the live view returned by `transaction.originals()`. Inside that loop it called `transaction.cancel(sstable)` for every unrepaired SSTable, which modifies the very collection being iterated, throwing a `ConcurrentModificationException`. Wrapping the collection in a defensive `new ArrayList<>(...)` copy before the loop ensures the iteration is over a stable snapshot while cancellations safely mutate the underlying transaction set.

### Before / After

```java
// BEFORE — iterates live collection while cancel() mutates it
for (SSTableReader sstable : transaction.originals())
{
    if (!sstable.isRepaired())
    {
        // cancel() modifies transaction.originals() → ConcurrentModificationException
        transaction.cancel(sstable);
        ...
    }
}

// AFTER — iterate a snapshot copy; cancel() can freely mutate the original
for (SSTableReader sstable : new ArrayList<>(transaction.originals()))
{
    if (!sstable.isRepaired())
    {
        transaction.cancel(sstable);
        ...
    }
}
```

---

## How to Spot

Any for-each loop over a collection that is also passed (directly or indirectly) to a mutating method inside the same loop body is a candidate for `ConcurrentModificationException`. Pay particular attention to transaction or lifecycle objects whose `cancel`/`remove` methods update an internal set — callers often forget that the collection returned by accessor methods like `originals()` is backed by the mutable internal state rather than a defensive copy.

---

## Test

`CompactionGarbageCollectOnlyPurgeRepairedTest.testOnlyPurgeRepaired` creates a table with `only_purge_repaired_tombstones=true`, flushes five SSTables with an alternating repaired/unrepaired pattern, calls `performGarbageCollection`, and asserts no `ConcurrentModificationException` is thrown.
