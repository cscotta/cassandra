# Bug 2426: Truncate leaves behind non-CFS-backed secondary index data

**Commit**: 5452883577 · **Ticket**: CASSANDRA-3844

## Detection Rule

When iterating a data structure's "children" to apply a destructive operation, verify the iteration API actually covers all variants of those children, not just one subtype.

## Code Smell

`main.concatWithIndexes()` iterates only CFS-backed secondary indexes; non-CFS indexes (e.g., custom per-row indexes sharing a single instance) are silently skipped, leaving stale data after truncate.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: incomplete-collection-iteration
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause

The truncate path called `main.concatWithIndexes()` which yields only `ColumnFamilyStore`-backed index CFS instances. Non-CFS indexes (per-row indexes) are stored only in `SecondaryIndexManager.indexesByColumn` and were never reached. The fix adds a dedicated `getIndexes()` method on `SecondaryIndexManager` that deduplicates via an `IdentityHashMap` (handling shared instances) and calls `truncate(truncatedAt)` on every index regardless of type.

## Example

```java
// BEFORE – iterates only CFS-backed indexes
for (ColumnFamilyStore cfs : main.concatWithIndexes()) {
    List<SSTableReader> truncated = new ArrayList<>();
    for (SSTableReader sstable : cfs.getSSTables())
        if (!sstable.newSince(truncatedAt)) truncated.add(sstable);
    if (!truncated.isEmpty()) cfs.markCompacted(truncated);
}

// AFTER – also truncates non-CFS indexes
main.discardSSTables(truncatedAt);
for (SecondaryIndex index : main.indexManager.getIndexes())
    index.truncate(truncatedAt);
```
