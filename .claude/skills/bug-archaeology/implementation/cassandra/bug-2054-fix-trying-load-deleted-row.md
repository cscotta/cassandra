# Bug 2054: Deleted Row Loaded into Row Cache on Startup

**Commit**: feae9efa42 · **Ticket**: CASSANDRA-4463
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Missing null / bounds check

---

## Root Cause

During startup, `ColumnFamilyStore` pre-warms the row cache by reading rows and inserting them into the cache. `getTopLevelColumns` returns `null` when a row has been deleted (a fully tombstoned row yields no live data). Without a null check, `null` was placed directly into the row cache under the row's key. Subsequent reads would retrieve `null` from the cache and crash or return incorrect results instead of treating the row as absent.

### Before / After

```java
// BEFORE
ColumnFamily data = getTopLevelColumns(...);
CacheService.instance.rowCache.put(new RowCacheKey(metadata.cfId, key), data);
```

```java
// AFTER
ColumnFamily data = getTopLevelColumns(...);
if (data != null)
    CacheService.instance.rowCache.put(new RowCacheKey(metadata.cfId, key), data);
```

---

## How to Spot

Whenever a read method can return `null` to signal absence or deletion, every call site that passes the result to a cache or collection without an intermediate null guard is a potential crash site. Review all startup warm-up or prefill paths to confirm they handle the tombstone/deletion case.

---

## Test

No test added.
