# Bug 2294: hasIrrelevantData Compares Column Timestamp Against localDeletionTime Instead of markedForDeleteAt

**Commit**: d741037351 · **Ticket**: CASSANDRA-3855

## Detection Rule

When checking whether a column is shadowed by a container tombstone, compare the column's live timestamp against `deletionInfo().maxTimestamp()` (the tombstone timestamp), not against `getLocalDeletionTime()` (a local wall-clock expiry time in seconds), since the two values are in different units and have different semantics.

## Code Smell

`column.mostRecentLiveChangeAt() <= deletedAt` where `deletedAt = deletionInfo().maxTimestamp()` was (before the fix) `getLocalDeletionTime()` — a seconds-since-epoch wall clock value being compared against a microsecond-precision write timestamp, causing false positives or false negatives.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: timestamp-unit-semantic-mismatch
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`hasIrrelevantData` determined whether a column was covered by a container-level tombstone by comparing `column.mostRecentLiveChangeAt()` (a write timestamp in microseconds) against `getLocalDeletionTime()` (a local deletion time in seconds). These two values are not comparable. The fix changes the comparison to use `deletionInfo().isDeleted(column)`, which correctly applies the tombstone timestamp semantics, and also fixes the gcable-deletion check to compare the purged `deletionInfo` against the original rather than comparing against the sentinel `LIVE` value.

## Example

```java
// BEFORE — mixes timestamp units
long deletedAt = deletionInfo().maxTimestamp(); // was: getLocalDeletionTime() — seconds
for (IColumn column : columns)
    if (column.mostRecentLiveChangeAt() <= deletedAt  // microseconds vs seconds
        || column.hasIrrelevantData(gcBefore))
        return true;
```

```java
// AFTER — uses proper semantic check
for (IColumn column : columns)
    if (deletionInfo().isDeleted(column)  // correctly uses tombstone timestamp
        || column.hasIrrelevantData(gcBefore))
        return true;
```
