# Bug 2205: BulkLoader NPE Because SSTableReader.keyCache Initialized Eagerly at Field Level

**Commit**: db3c198ad0 · **Ticket**: CASSANDRA-4846
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableReader.keyCache` was initialized as a `final` field directly referencing `CacheService.instance.keyCache` at class load time. When `SSTableReader` is used by the BulkLoader tool (outside a running Cassandra node), `CacheService` is never initialized, so `CacheService.instance` is `null` and the field assignment threw a `NullPointerException` on construction. The fix makes `keyCache` non-final and defers its assignment to `setTrackedBy`, which is only called when the reader is wired into the server's `DataTracker`. All subsequent cache accesses were guarded with an explicit `keyCache != null` check.

### Before / After

```java
// BEFORE: eagerly assigned at field level, NPE if CacheService not initialized
private final InstrumentingCache<KeyCacheKey, RowIndexEntry> keyCache =
    CacheService.instance.keyCache;
```

```java
// AFTER: lazily assigned in setTrackedBy; guarded at use sites
private InstrumentingCache<KeyCacheKey, RowIndexEntry> keyCache;

public void setTrackedBy(DataTracker tracker) {
    deletingTask.setTracker(tracker);
    keyCache = CacheService.instance.keyCache;
}
```

---

## How to Spot

Eager field initializers that reference singleton services are dangerous when the class is reused in standalone tools or tests where those services may not be started. Prefer lazy initialization via a lifecycle method and add null guards at every use site.

---

## Test

No test added.
