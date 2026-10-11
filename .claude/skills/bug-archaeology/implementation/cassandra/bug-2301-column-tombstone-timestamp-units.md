# Bug 2301: Column Tombstone Check Uses localDeletionTime (Seconds) Instead of markedForDeleteAt (Microseconds)

**Commit**: 84a1d6059b · **Ticket**: CASSANDRA-4396

## Detection Rule

When comparing a column's write timestamp against a container tombstone to determine if the column is shadowed, verify the comparison uses the tombstone's `markedForDeleteAt` (a user-space timestamp in the same unit as write timestamps) and not `getLocalDeletionTime()` (a wall-clock time in seconds).

## Code Smell

`column.mostRecentLiveChangeAt() < getLocalDeletionTime()` mixes a microsecond-precision write timestamp with a seconds-since-epoch wall-clock value; columns written before 2001 will always appear shadowed, and columns written after ~year 2554 will never appear shadowed.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: timestamp-unit-semantic-mismatch
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`AbstractColumnContainer.hasIrrelevantData` checked whether a column was covered by a row tombstone by comparing `column.mostRecentLiveChangeAt()` (write timestamp in microseconds) against `getLocalDeletionTime()` (local expiry time in seconds). Because the units are incompatible, columns with write timestamps below ~year 2001 always appeared shadowed and were incorrectly dropped during compaction. The fix stores `getMarkedForDeleteAt()` (same-unit tombstone timestamp) in `deletedAt` and uses that for the comparison.

## Example

```java
// BEFORE — compares microsecond write timestamp against seconds wall clock
for (IColumn column : columns)
    if (column.mostRecentLiveChangeAt() < getLocalDeletionTime()  // wrong units
        || column.hasIrrelevantData(gcBefore))
        return true;
```

```java
// AFTER — compares write timestamp against tombstone timestamp (same units)
long deletedAt = getMarkedForDeleteAt();  // microsecond tombstone timestamp
for (IColumn column : columns)
    if (column.mostRecentLiveChangeAt() <= deletedAt
        || column.hasIrrelevantData(gcBefore))
        return true;
```
