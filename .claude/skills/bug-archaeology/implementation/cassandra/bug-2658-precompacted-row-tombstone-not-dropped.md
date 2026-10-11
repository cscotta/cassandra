# Bug 2658: PrecompactedRow.isEmpty returns false for a row containing only tombstones, preventing compaction from dropping it

**Commit**: 3291647a96 · **Ticket**: CASSANDRA-2786

## Detection Rule
When determining if a compacted row is empty and should be skipped in the output SSTable, call `ColumnFamilyStore.removeDeletedCF(compactedCf, gcBefore)` rather than checking `getColumnCount() == 0`; a CF containing only a row tombstone has zero columns but must still be purged via `removeDeletedCF`.

## Code Smell
`PrecompactedRow.isEmpty()` returns `compactedCf.getColumnCount() == 0`, which is `false` for a CF that holds a row-level deletion marker (`markedForDeleteAt > Long.MIN_VALUE`) with no sub-columns; this row is not empty from the compaction GC perspective but is incorrectly output to the new SSTable.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: column-count-misses-row-tombstone
- severity: data-loss
- subsystem: compaction

## Root Cause
`PrecompactedRow.isEmpty()` checked `compactedCf.getColumnCount() == 0` to decide whether to skip writing the row. A CF that is a row tombstone (deleted via a `DELETE FROM` statement) has no sub-columns (`getColumnCount() == 0` is false — actually zero columns, but `markedForDeleteAt` is set) — wait, actually a tombstone CF has 0 columns but a set `markedForDeleteAt`. The correct check is `ColumnFamilyStore.removeDeletedCF(compactedCf, gcBefore) == null`, which returns null when the entire CF (including the row tombstone) is past its GC grace. Using just column count kept tombstoned-but-expired rows in the SSTable indefinitely.

## Example
```java
// BEFORE
public boolean isEmpty() {
    return compactedCf == null || compactedCf.getColumnCount() == 0;
}

// AFTER
public boolean isEmpty() {
    return compactedCf == null
        || ColumnFamilyStore.removeDeletedCF(compactedCf, gcBefore) == null;
}
```
