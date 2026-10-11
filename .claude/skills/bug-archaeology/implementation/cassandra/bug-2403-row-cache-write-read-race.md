# Bug 2403: Row cache race between concurrent reads and writes allows stale/partial data to be cached

**Commit**: c9270f4e3a · **Ticket**: CASSANDRA-3862

## Detection Rule

When a cache is populated by a read that first checks for a miss, then queries storage, then inserts, a concurrent write that arrives between the check and the insert will update the cache with stale data — use a sentinel + CAS loop to serialize the population.

## Code Smell

A read path that does `cache.get(key)` → (miss) → read from disk → `cache.put(key, result)` without atomic protection; a concurrent write calls `cache.put(key, newValue)` between the disk read and the final put, and the read then overwrites the newer value with the stale disk result.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: cache-check-then-act-race
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

The row cache population was not atomic: a read would find a cache miss, read from SSTables, and then unconditionally `put` the result. A concurrent write would update the cache in between, but the read's subsequent `put` would overwrite it with the older disk snapshot. The fix introduces a `RowCacheSentinel` marker: the read path uses `putIfAbsent` to claim the slot with a sentinel, does the disk read, then atomically replaces the sentinel with the real data via `replace(key, sentinel, data)`. A concurrent write that finds the sentinel invalidates it instead of updating, forcing the reader to re-try.

## Example

```java
// BEFORE — racy put after disk read
IRowCacheEntry cached = cache.get(key);
if (cached == null) {
    ColumnFamily data = readFromDisk(key);
    cache.put(key, data);   // concurrent write's update silently overwritten
    return data;
}
```

```java
// AFTER — sentinel-based atomic population
IRowCacheEntry cached = cache.get(key);
if (cached == null) {
    RowCacheSentinel sentinel = new RowCacheSentinel();
    boolean reserved = cache.putIfAbsent(key, sentinel);
    if (reserved) {
        ColumnFamily data = readFromDisk(key);
        cache.replace(key, sentinel, data);   // fails if a write already invalidated sentinel
        return data;
    }
    // else another thread populated; loop back to get
}
```
