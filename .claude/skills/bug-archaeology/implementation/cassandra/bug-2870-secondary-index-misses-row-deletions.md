# Bug 2870: Secondary index not updated for row deletions

**Commit**: e7591fc3c0 · **Ticket**: CASSANDRA-1546 (CASSANDRA-1571)

## Detection Rule
When an index-update path checks for the presence of specific column names in a mutation, verify it also handles the case where the entire row is marked for deletion, which carries no column names but still invalidates index entries.

## Code Smell
The loop `for (byte[] column : cfs.getIndexedColumns()) { if (cf.getColumnNames().contains(column)) ... }` only detects index-relevant changes when individual indexed columns are present; a row-level tombstone (`cf.isMarkedForDelete()`) sets no individual column names, so index entries for deleted rows were never cleaned up.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-tombstone-case-in-index-update
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
The condition that decided whether an indexed column was mutated did not account for row-level deletes. Adding `|| cf.isMarkedForDelete()` to the trigger condition caused the secondary index update path to be entered for deletes. Inside `applyIndexUpdates`, an additional guard `if (column == null || column.isMarkedForDelete()) continue;` prevents creating new index entries for tombstoned columns while still allowing old stale entries to be removed.

## Example
```java
// BEFORE — row deletions never trigger index cleanup
if (cf.getColumnNames().contains(column))
{
    mutatedIndexedColumns.add(column);
}
```
```java
// AFTER — row deletions also trigger index cleanup
if (cf.getColumnNames().contains(column) || cf.isMarkedForDelete())
{
    mutatedIndexedColumns.add(column);
}
```
