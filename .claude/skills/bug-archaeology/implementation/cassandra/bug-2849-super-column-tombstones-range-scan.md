# Bug 2849: Supercolumn tombstones not pruned in range scan results

**Commit**: 38ff66f408 · **Ticket**: CASSANDRA-1734

## Detection Rule
When returning rows from a range scan that includes super-column families, apply `removeDeleted` with the current `gcBefore` timestamp to each super-column family before adding it to the result set.

## Code Smell
`rows.add(current)` in the range-scan loop adds super-column rows without pruning deleted subcolumns. For standard column families the merge/collation step handles this, but super-column collation skips subcolumn relevance checks, so expired or tombstoned subcolumns remain in the result.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-tombstone-filter-range-scan
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
During range scans, `RowIteratorFactory` collates super-column data without checking individual subcolumn liveness. The fix adds a post-facto prune step: for super-column families, the range scan wraps each result row with `removeDeleted(cf, gcBefore)` before appending to the result list. `gcBefore` is computed once before the loop from `metadata.gcGraceSeconds`.

## Example
```java
// BEFORE
rows.add(current);

// AFTER
int gcBefore = (int)(System.currentTimeMillis() / 1000) - metadata.gcGraceSeconds;
// ...
rows.add(current.cf != null && current.cf.isSuper()
         ? new Row(current.key, removeDeleted(current.cf, gcBefore))
         : current);
```
