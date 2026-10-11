# Bug 2931: Missing localDeletionTime update when merging super-column tombstones

**Commit**: ca4a07e4f0 · **Ticket**: CASSANDRA-689

## Detection Rule
When merging tombstone metadata from one column into another, if `markedForDeleteAt` (the logical tombstone timestamp) is updated but `localDeletionTime` (the physical GC clock) is not, the column appears deleted but is never garbage-collected.

## Code Smell
`FBUtilities.atomicSetMax(markedForDeleteAt, ...)` appears without a preceding `atomicSetMax(localDeletionTime, ...)`, leaving `localDeletionTime` at its default and preventing GC.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: partial-tombstone-metadata-update
- severity: data-loss
- subsystem: storage-engine

## Root Cause
`SuperColumn.putColumn()` updated `markedForDeleteAt` from the incoming column's tombstone marker but did not update `localDeletionTime`. A column with a valid `markedForDeleteAt` but a zero or `MIN_VALUE` `localDeletionTime` would never be garbage-collected, causing deleted data to persist indefinitely. The fix adds the missing `atomicSetMax(localDeletionTime, ...)` call before updating `markedForDeleteAt`.

## Example
```java
// BEFORE (SuperColumn.putColumn)
FBUtilities.atomicSetMax(markedForDeleteAt, column.getMarkedForDeleteAt());
// localDeletionTime never set → GC never triggered
```
```java
// AFTER
FBUtilities.atomicSetMax(localDeletionTime, column.getLocalDeletionTime()); // set first
FBUtilities.atomicSetMax(markedForDeleteAt, column.getMarkedForDeleteAt());
```
