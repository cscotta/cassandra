# Bug 2482: Race between column family flush and secondary index flush — double-freeze causes missed renew

**Commit**: a795eb936d · **Ticket**: CASSANDRA-3547

## Detection Rule

When a memtable is frozen before iterating all CFS stores (including indexes), a concurrent secondary-index flush can freeze the same memtable again, causing the primary store to skip both flushing it and renewing it.

## Code Smell

`oldMemtable.freeze()` called on the primary CFS memtable before the loop that collects dirty memtables from all indexes, while each index CFS can also independently invoke flush (and freeze) on its own memtable. The subsequent `if (!cfs.getMemtableThreadSafe().isClean()) icc.add(cfs)` check without `!mt.isFrozen()` allows adding an already-frozen memtable.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: early-freeze-before-index-collection
- severity: data-loss
- subsystem: storage-engine

## Root Cause

`ColumnFamilyStore.switchMemtable()` froze the primary memtable before looping over all CFS stores (main + indexes). If a secondary index's memtable was concurrently being flushed (and therefore already frozen), `!mt.isClean()` was still true (dirty data exists), but the memtable was already in the process of being flushed by another thread. The fix delays freeze until inside the loop, and adds `!mt.isFrozen()` as a guard before adding to the flush list.

## Example

```java
// BEFORE
oldMemtable.freeze();  // freeze primary first
for (ColumnFamilyStore cfs : concatWithIndexes())
{
    if (!cfs.getMemtableThreadSafe().isClean())
        icc.add(cfs);  // may add already-frozen index memtable
}
```

```java
// AFTER
for (ColumnFamilyStore cfs : concatWithIndexes())
{
    Memtable mt = cfs.getMemtableThreadSafe();
    if (!mt.isClean() && !mt.isFrozen())
    {
        mt.freeze();  // freeze lazily, only if not already frozen
        icc.add(cfs);
    }
}
```
