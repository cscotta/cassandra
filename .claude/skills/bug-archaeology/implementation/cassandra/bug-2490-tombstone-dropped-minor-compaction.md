# Bug 2490: Compaction drops tombstones for rows not fully covered by compaction set

**Commit**: dc342de371 · **Ticket**: CASSANDRA-2786

## Detection Rule

When `isEmpty()` on a compacted row purges tombstones based on `gcBefore` without first verifying that all versions of that row are included in the compaction, shadowed data in un-compacted SSTables can reappear after tombstones are purged.

## Code Smell

`ColumnFamilyStore.removeDeletedCF(emptyColumnFamily, gcBefore) == null` called in `isEmpty()` on a row type (`EchoedRow`, `LazilyCompactedRow`) without checking `shouldPurge()`, allowing tombstone deletion even in a non-major compaction where older SSTables remain uncompacted.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: tombstone-purge-without-coverage-check
- severity: data-loss
- subsystem: compaction

## Root Cause

`EchoedRow.isEmpty()` used `removeDeletedCF(..., gcBefore)` which would purge the row if its tombstone was old enough — without checking whether the compaction included all SSTables that might have data for that key. `LazilyCompactedRow.isEmpty()` had the same issue. For non-major (minor) compactions where older SSTables remain, purging a tombstone can cause deleted data to "resurrect" from those older files. The fix makes `EchoedRow.isEmpty()` always return `false` (never purge; echoing implies no deserialize), and makes `LazilyCompactedRow.isEmpty()` only apply `removeDeletedCF` when `shouldPurge` is true.

## Example

```java
// BEFORE (EchoedRow)
public boolean isEmpty() {
    return !row.hasNext() && ColumnFamilyStore.removeDeletedCF(row.getColumnFamily(), gcBefore) == null;
}
```

```java
// AFTER (EchoedRow)
public boolean isEmpty() {
    // never okay to purge an EchoedRow -- if it were, we'd need to deserialize instead of echoing
    return false;
}

// AFTER (LazilyCompactedRow)
boolean cfIrrelevant = shouldPurge
    ? ColumnFamilyStore.removeDeletedCF(emptyColumnFamily, controller.gcBefore) == null
    : !emptyColumnFamily.isMarkedForDelete(); // tombstones are relevant
```
