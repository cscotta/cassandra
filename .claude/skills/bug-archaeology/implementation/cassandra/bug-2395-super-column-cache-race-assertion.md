# Bug 2395: Super column cache update races with PeriodicCommitLog, causing assertion failure

**Commit**: f239907446 · **Ticket**: CASSANDRA-3957

## Detection Rule

When a cached mutable object is updated in-place by sharing a reference with a data structure that can be concurrently written by another thread, guard the update by cloning the shared object before merging it into the cache.

## Code Smell

`cachedRow.addAll(columnFamily, allocator)` passes the live `columnFamily` (which the commit-log writer still holds a reference to and may mutate its super columns concurrently) directly into the cache row without copying super column structures first.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: shared-mutable-cache-update
- severity: crash
- subsystem: storage-engine

## Root Cause

When Cassandra updates a cached row, it calls `addAll` with the `ColumnFamily` object that was just written to the commit log. With `PeriodicCommitLog`, the commit-log writer continues to reference that object and can concurrently modify its `SuperColumn` children. Because `addAll` shares the `SuperColumn` reference rather than cloning it, the cache and the commit-log path end up mutating the same `SuperColumn` concurrently, tripping an internal assertion. The fix introduces `addAllWithSCCopy` which clones each `SuperColumn` (shallow clone of the super column, preserving sub-column references) before merging into the cache.

## Example

```java
// BEFORE — SuperColumn shared between cache and commit-log writer
((ColumnFamily) cachedRow).addAll(columnFamily, HeapAllocator.instance);
```

```java
// AFTER — SuperColumns cloned to break the shared reference
((ColumnFamily) cachedRow).addAllWithSCCopy(columnFamily, HeapAllocator.instance);

// addAllWithSCCopy:
if (cf.isSuper()) {
    for (IColumn c : cf)
        columns.addColumn(((SuperColumn)c).cloneMeShallow(), allocator);
    delete(cf);
} else {
    addAll(cf, allocator);
}
```
