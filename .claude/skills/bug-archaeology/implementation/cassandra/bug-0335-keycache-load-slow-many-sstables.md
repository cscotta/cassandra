# Bug 0335: Slow Key Cache Load Blocking Startup on Tables with Many SSTables

**Commit**: 1911a887e8 · **Ticket**: CASSANDRA-14898
**Subsystem**: src/java/org/apache/cassandra/service/CacheService.java
**Category**: Wrong constant / default value

---

## Root Cause

`CacheService.KeyCacheSerializer.deserialize` resolved each key-cache entry to its SSTable by calling `cfs.getSSTables(SSTableSet.CANONICAL)` and performing a linear scan for the matching generation number. For a column family store with N SSTables and M key-cache entries this produced O(N×M) SSTable iterator calls on every startup, each call rebuilding the full SSTable set. On deployments with thousands of SSTables this made key-cache loading take hours and completely blocked the startup sequence. Additionally, there was no timeout guarding cache loading: the `AutoSavingCache` load loop ran until every serialized entry was processed, with no way to bound startup time. The default timeout of 30 s was correct but was never applied because the loop condition only checked `in.available() > 0`.

### Before / After

```java
// BEFORE – O(N) scan per key-cache entry, unbounded loop
while (in.available() > 0) {
    // ...
    reader = findDesc(generation, cfs.getSSTables(SSTableSet.CANONICAL)); // linear scan every time
}
```

```java
// AFTER – build HashMap<generation, SSTableReader> once per CFS, respect timeout
long loadByNanos = start + TimeUnit.SECONDS.toNanos(DatabaseDescriptor.getCacheLoadTimeout());
while (System.nanoTime() < loadByNanos && in.available() > 0) {
    // ... look up in cachedSSTableReaders HashMap – O(1) per entry
}
// cleanupAfterDeserialize() clears the per-load HashMap
```

---

## How to Spot

Any deserialization or bulk-load loop that calls a method returning a live collection on every iteration is a candidate for O(N²) behaviour. Look for patterns like `for entry in saved_entries: find(entry, live_collection.get())` where `live_collection.get()` rebuilds or iterates the full set. Also flag any long-running startup operation that has no deadline or timeout: startup sequences must be bounded so that a degraded but recoverable state does not look identical to a hung process.

---

## Test

`KeyCacheTest.testKeyCacheLoadCacheLoadTimeExceedingLimit` uses a Mockito spy to inject a 1-second delay per deserialization call and asserts that fewer than the maximum expected entries are loaded when `cache_load_timeout_seconds` is set to 2.
