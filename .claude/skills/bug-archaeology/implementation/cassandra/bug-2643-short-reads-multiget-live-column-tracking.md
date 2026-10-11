# Bug 2643: Short reads in multi-get due to missing live-column replenishment after read-repair

**Commit**: 388c690c22 · **Ticket**: CASSANDRA-2643

## Detection Rule
When a slice query is backed by read-repair that may return a merged result with fewer live columns than requested, track the maximum live-column count seen across all replicas and retry with an expanded request if the merged result falls short.

## Code Smell
A multi-get that sends out fixed `ReadCommand` objects and returns the first response without comparing the merged live-column count against the maximum seen across replicas will silently return fewer columns than requested when some replicas have deleted tombstones that others haven't yet received.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: short-read-without-live-column-tracking
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
When read-repair merged responses from multiple replicas, tombstones on some replicas could reduce the effective live-column count below the requested limit even though other replicas had the full set of live columns. The read path returned the merged (potentially tombstone-heavy) result without checking whether it satisfied the original column count request. The fix introduces `maxLiveColumns` tracking in `RowRepairResolver`, exposes it via `getMaxLiveColumns()`, and adds a retry loop in `StorageProxy` that re-issues expanded read commands when the live column count is insufficient.

## Example
```java
// BEFORE — resolved row returned unconditionally
return new Row(key, resolved);
```
```java
// AFTER — track max live columns and expose for retry
for (ColumnFamily cf : versions) {
    int liveColumns = cf.getLiveColumnCount();
    if (liveColumns > maxLiveColumns)
        maxLiveColumns = liveColumns;
}
resolved = resolveSuperset(versions);
return new Row(key, resolved);
// StorageProxy then retries if repairCallback.getMaxLiveColumns() < requested
```
