# Bug 2968: Counter CF compaction assertion error due to removeOldShards called without shouldPurge check

**Commit**: caea7ffd6b · **Ticket**: CASSANDRA-2968

## Detection Rule
When calling `CounterColumn.removeOldShards()` after `ColumnFamilyStore.removeDeleted()`, gate the call on `shouldPurge` being true, because shard removal is only safe when tombstone purging is active.

## Code Smell
`if (compactedCf != null && compactedCf.metadata().getDefaultValidator().isCommutative()) { CounterColumn.removeOldShards(...); }` without a `shouldPurge &&` guard will trigger assertion failures in `removeOldShards` when called during non-purging compaction rounds.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-precondition-guard-for-purge
- severity: crash
- subsystem: compaction

## Root Cause
Both `LazilyCompactedRow` and `PrecompactedRow` called `CounterColumn.removeOldShards()` whenever the compacted CF was a counter type and non-null, regardless of whether `shouldPurge` was true. `removeOldShards` internally asserts conditions that only hold when tombstone purging is in effect. When compaction ran without purging the assertion fired. The fix adds `shouldPurge &&` to both callsites.

## Example
```java
// BEFORE (LazilyCompactedRow)
if (purged != null && purged.metadata().getDefaultValidator().isCommutative())
    CounterColumn.removeOldShards(purged, controller.gcBefore);
```
```java
// AFTER
if (shouldPurge && purged != null && purged.metadata().getDefaultValidator().isCommutative())
    CounterColumn.removeOldShards(purged, controller.gcBefore);
```
