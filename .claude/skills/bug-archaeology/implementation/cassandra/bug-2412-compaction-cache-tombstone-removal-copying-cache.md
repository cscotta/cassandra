# Bug 2412: Compaction unnecessarily deserializes cached rows even when cache uses copy-on-put

**Commit**: 94860c6c37 · **Ticket**: CASSANDRA-3921

## Detection Rule

When a cache supports a "copy-on-put" mode where updates must re-serialize the entire value, in-place mutation of cached rows during compaction is both racy and pointless — guard with a mode check and skip the mutation.

## Code Smell

`CompactionController.removeDeletedInCache` calls `ColumnFamilyStore.removeDeleted(cachedRow, gcBefore)` unconditionally, which mutates the cached row in place; for a copying cache this is a racy operation against other writers that will serialize the same row.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: in-place-cache-mutation-with-copying-cache
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

During compaction, `removeDeletedInCache` attempted to purge tombstones from cached rows by mutating them directly. When the row cache was configured in "put-copying" mode (where each put serializes a snapshot of the row), this in-place mutation was racy against concurrent writers and could corrupt the cache. Because any write would immediately invalidate or replace the cached row anyway, the mutation was also unproductive. The fix adds an early return when `CacheService.instance.rowCache.isPutCopying()`.

## Example

```java
// BEFORE — unconditional in-place mutation, racy for copy-caches
public void removeDeletedInCache(DecoratedKey key) {
    ColumnFamily cachedRow = cfs.getRawCachedRow(key);
    if (cachedRow != null)
        ColumnFamilyStore.removeDeleted(cachedRow, gcBefore);
}
```

```java
// AFTER — skip for copying caches
public void removeDeletedInCache(DecoratedKey key) {
    if (CacheService.instance.rowCache.isPutCopying())
        return;   // racy and pointless; next write will invalidate anyway
    ColumnFamily cachedRow = cfs.getRawCachedRow(key);
    if (cachedRow != null)
        ColumnFamilyStore.removeDeleted(cachedRow, gcBefore);
}
```
