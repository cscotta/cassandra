# Bug 2313: Cleanup skips index entry deletion when SSTable is outside local token ranges

**Commit**: 8a3bb80e0d · **Ticket**: CASSANDRA-4379

## Detection Rule

When an optimization skips processing of an SSTable based on a range check (e.g., "skip if token range doesn't intersect local ranges"), verify that the skip is only valid when there are no secondary indexes that also need the row data processed.

## Code Smell

A range-intersection early-exit that calls `cfs.replaceCompactedSSTables(...)` and `continue` before checking `indexedColumns`, causing indexed columns to have stale index entries for data that no longer belongs to this node.

## Tags

- scope: cassandra
- category: silent-skip
- code-pattern: early-exit-bypasses-index-cleanup
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause

During cleanup, if an SSTable's token range did not intersect the node's local ranges, the code immediately retired the SSTable without scanning rows. However, if the table had secondary indexes, index entries for those rows were never cleaned up. The fix adds a check: only skip SSTable scanning when there are no indexed columns. Additionally, `indexedColumns` was recomputed inside the per-SSTable loop — an unnecessary repeated call that the fix correctly hoists out of the loop.

## Example

```java
// BEFORE
for (SSTableReader sstable : sstables)
{
    if (!new Bounds<Token>(sstable.first.token, sstable.last.token).intersects(ranges))
    {
        // skip entirely — but index entries are leaked!
        cfs.replaceCompactedSSTables(..., emptyList, CLEANUP);
        continue;
    }
    // ...
    Collection<ByteBuffer> indexedColumns = cfs.indexManager.getIndexedColumns(); // re-computed per sstable
}
```

```java
// AFTER
Collection<ByteBuffer> indexedColumns = cfs.indexManager.getIndexedColumns(); // hoisted

for (SSTableReader sstable : sstables)
{
    if (indexedColumns.isEmpty()
        && !new Bounds<Token>(sstable.first.token, sstable.last.token).intersects(ranges))
    {
        // safe to skip only when no indexes
        cfs.replaceCompactedSSTables(..., emptyList, CLEANUP);
        continue;
    }
    // ...
}
```
