# Bug 2122: Saved Key Cache Silently Skipped When keyCache Field Is Null at Load Time

**Commit**: f930eb79cd · **Ticket**: CASSANDRA-5166
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Missing null / bounds check

---

## Root Cause

During `SSTableReader.load()`, the flag `cacheLoading` was computed as `keyCache != null && !keysToLoadInCache.isEmpty()`. If `keyCache` happened to be null (not yet initialized on the reader instance), the condition short-circuited to `false` and the saved key cache was silently ignored, meaning the on-disk key cache file was never consulted at startup. The fix inverts the guard: determine whether loading is needed first, then lazily assign `keyCache` from the singleton `CacheService.instance.keyCache` if it is null.

### Before / After

```java
// BEFORE
boolean cacheLoading = keyCache != null && !keysToLoadInCache.isEmpty();
```

```java
// AFTER
boolean cacheLoading = !keysToLoadInCache.isEmpty();
if (cacheLoading && keyCache == null)
    keyCache = CacheService.instance.keyCache;
```

---

## How to Spot

When a boolean guard combines an initialization check (`x != null`) with a work check (`!set.isEmpty()`), verify that the null case is not silently treated as "nothing to do." If the null merely means "not yet initialized," the null should trigger lazy initialization rather than early exit.

---

## Test

`SSTableReaderTest.testLoadingSavedKeyCache` writes an SSTable and a corresponding key-cache file, then verifies that the position is actually loaded from cache (asserts `getCachedPosition(...) == 0` rather than a miss).
