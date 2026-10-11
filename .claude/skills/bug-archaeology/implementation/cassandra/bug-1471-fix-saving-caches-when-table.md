# Bug 1471: Fix Saving Caches When a Table Is Dropped

**Commit**: e25d94e6e9 · **Ticket**: CASSANDRA-7784
**Subsystem**: src/java/org/apache/cassandra/cache/
**Category**: Missing null / bounds check

---

## Root Cause

`AutoSavingCache.Writer` grouped cache keys for serialization using `CacheKey.PathInfo` (a struct containing keyspace name, table name, and CF UUID). When a table was dropped, `Schema.instance.getCF(cfId)` returned null inside `getPathInfo()` on `CounterCacheKey` and `RowCacheKey`, and the return value was immediately destructured (`cf.left`, `cf.right`), throwing a NullPointerException. The fix replaced the `PathInfo` grouping key with the raw CF UUID and added an explicit `Schema.instance.hasCF()` check to skip cache entries whose table no longer exists before attempting to write them.

### Before / After

```java
// BEFORE — NPE when table is dropped: getCF returns null, .left/.right thrown on null
public PathInfo getPathInfo() {
    Pair<String, String> cf = Schema.instance.getCF(cfId);
    return new PathInfo(cf.left, cf.right, cfId);   // NullPointerException
}
```

```java
// AFTER — return the UUID directly; existence check in the writer
public UUID getCFId() {
    return cfId;
}
// In AutoSavingCache.Writer:
UUID cfId = key.getCFId();
if (!Schema.instance.hasCF(key.getCFId()))
    continue; // the table has been dropped.
```

---

## How to Spot

Any code path that calls `Schema.instance.getCF()` and immediately dereferences the result without a null check is vulnerable to a race with concurrent `DROP TABLE`. Cache save/load paths are particularly susceptible because they run asynchronously and iterate over long-lived cache contents that may reference tables dropped since the cache was populated.

---

## Test

No test added.
