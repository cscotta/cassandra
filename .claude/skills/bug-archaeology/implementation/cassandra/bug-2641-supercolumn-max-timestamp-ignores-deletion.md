# Bug 2641: SuperColumn maxTimestamp ignores the super-column deletion timestamp

**Commit**: b834dde287 · **Ticket**: CASSANDRA-2753

## Detection Rule
When computing `maxTimestamp()` for a container cell (SuperColumn), include the container's own deletion/tombstone timestamp via `getMarkedForDeleteAt()` in addition to iterating sub-column timestamps.

## Code Smell
`SuperColumn.maxTimestamp()` iterates `getSubColumns()` and takes `Math.max` over sub-column timestamps but returns without considering `getMarkedForDeleteAt()`, so a super-column that is itself a tombstone (with no sub-columns, or with an older sub-column timestamp) returns a timestamp that is too low.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-tombstone-timestamp
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SuperColumn.maxTimestamp()` computed the maximum timestamp across all sub-columns but did not account for the super column's own `markedForDeleteAt` timestamp. If the super column was deleted (e.g. via a `DELETE` of the super column itself) with a newer timestamp than any sub-column, `maxTimestamp()` would return the stale sub-column value. This could affect compaction decisions, repair conflict resolution, and any code that depends on `maxTimestamp()` to determine the most recent change to a super column.

## Example
```java
// BEFORE
long maxTimestamp = Long.MIN_VALUE;
for (IColumn subColumn : getSubColumns())
    maxTimestamp = Math.max(maxTimestamp, subColumn.maxTimestamp());
return maxTimestamp;  // ignores super column's own deletion timestamp

// AFTER
return Math.max(maxTimestamp, getMarkedForDeleteAt());
```
