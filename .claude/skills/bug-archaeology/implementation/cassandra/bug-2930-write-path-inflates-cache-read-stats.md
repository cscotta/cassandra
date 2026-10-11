# Bug 2930: Write path incorrectly increments cache read statistics

**Commit**: db1bebf118 · **Ticket**: CASSANDRA-770

## Detection Rule
When the same cache `get()` method is used to check for an existing entry in both read and write paths, write operations inflate read-hit and read-miss counters, skewing cache statistics.

## Code Smell
`getRawCachedRow()` calls `rowCache.get(key)`, which records a cache access in metrics; callers in the write path use this method to check for a cached row to update, but should not affect read-oriented stats.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: instrumented-get-in-write-path
- severity: performance
- subsystem: storage-engine

## Root Cause
`ColumnFamilyStore.getRawCachedRow()` used `rowCache.get(key)`, the same method used for actual reads, which also increments hit/miss counters. The write path calls `getRawCachedRow()` to check whether it needs to invalidate a cached row. By adding a `getInternal()` bypass that accesses the underlying map without touching statistics, writes no longer pollute the read cache metrics.

## Example
```java
// BEFORE
public ColumnFamily getRawCachedRow(String key) {
    return rowCache.getCapacity() == 0 ? null : rowCache.get(key);
    // rowCache.get() records a cache hit/miss
}
```
```java
// AFTER
public ColumnFamily getRawCachedRow(String key) {
    return rowCache.getCapacity() == 0 ? null : rowCache.getInternal(key);
    // getInternal() skips instrumentation
}
// In InstrumentedCache:
public V getInternal(K key) { return map.get(key); }
```
