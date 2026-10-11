# Bug 2668: Columns suppressed by tombstone not removed during replica resolution

**Commit**: 53d28807c1 → 4428bb276d · **Ticket**: CASSANDRA-2590

## Detection Rule
When merging replica responses for read repair, if the resolution logic clones the first non-null column family and then calls `resolve()` directly rather than going through the tombstone-aware `collectCollatedColumns` + `removeDeleted` pipeline, live columns suppressed by row or supercolumn tombstones are silently retained.

## Code Smell
`resolved = cf.cloneMe()` followed by `resolved.resolve(cf)` for each version, bypassing the tombstone suppression path that `getColumnFamily` normally applies.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: tombstone-suppression-bypass
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
`RowRepairResolver.resolveSuperset()` cloned the first non-null replica response and merged subsequent responses with `resolve()`, which only does last-write-wins column merging. It never invoked `collectCollatedColumns` + `removeDeleted`, so columns that should have been suppressed by a row-level or supercolumn tombstone survived replica resolution and were served to the client. The fix mimics the same `collectCollatedColumns` + `removeDeleted` path used by `ColumnFamilyStore.getColumnFamily`.

## Example
```java
// BEFORE
for (ColumnFamily cf : versions)
    if (cf != null) { resolved = cf.cloneMe(); break; }
for (ColumnFamily cf : versions)
    resolved.resolve(cf);
return resolved;
```
```java
// AFTER
// build a shallow clone accumulating deletions, then run collating iterator
for (ColumnFamily cf : versions)
    if (cf != null) { if (resolved == null) resolved = cf.cloneMeShallow(); else resolved.delete(cf); }
CollatingIterator iter = new CollatingIterator(...);
for (ColumnFamily version : versions) { if (version != null) iter.addIterator(...); }
filter.collectCollatedColumns(resolved, iter, Integer.MIN_VALUE);
return ColumnFamilyStore.removeDeleted(resolved, Integer.MIN_VALUE);
```
