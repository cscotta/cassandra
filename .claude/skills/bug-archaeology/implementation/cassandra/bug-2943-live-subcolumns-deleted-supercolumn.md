# Bug 2943: Live sub-columns of deleted super-columns not returned in query results

**Commit**: 83cce240bf · **Ticket**: CASSANDRA-583

## Detection Rule
When a super-column is marked for deletion but contains sub-columns written after the tombstone timestamp, a filter that checks only `container.isMarkedForDelete()` without comparing sub-column timestamps against the tombstone will incorrectly suppress live sub-columns.

## Code Smell
`mostRecentChangeAt()` on a `SuperColumn` iterates all sub-columns including deleted ones, returning the max timestamp across both live and deleted sub-columns; comparing this against the container's delete marker does not correctly identify live sub-columns that post-date the tombstone.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: tombstone-vs-live-subcolumn-comparison
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SuperColumn.mostRecentChangeAt()` returned the maximum timestamp across all sub-columns, including deleted ones. This was used to determine whether a sub-column was "newer than" its container's tombstone. If a sub-column was itself deleted at a high timestamp, it would cause `mostRecentChangeAt()` to return a timestamp that eclipsed the super-column tombstone, making the entire super-column appear live even though no live sub-columns existed. The fix renames the method to `mostRecentLiveChangeAt()` and changes it to consider only non-deleted sub-columns' timestamps, ensuring the comparison correctly identifies whether live data exists after the tombstone.

## Example
```java
// BEFORE: considers deleted sub-columns' timestamps
public long mostRecentChangeAt() {
    long max = Long.MIN_VALUE;
    for (IColumn column : columns_.values()) {
        if (column.mostRecentChangeAt() > max)
            max = column.mostRecentChangeAt();  // includes deleted cols
    }
    return max;
}
```
```java
// AFTER: considers only live sub-columns
public long mostRecentLiveChangeAt() {
    long max = Long.MIN_VALUE;
    for (IColumn column : columns_.values()) {
        if (!column.isMarkedForDelete() && column.timestamp() > max)
            max = column.timestamp();  // live cols only
    }
    return max;
}
```
