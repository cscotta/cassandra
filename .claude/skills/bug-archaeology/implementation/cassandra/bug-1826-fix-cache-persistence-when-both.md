# Bug 1826: Cache Persistence Deletes Wrong Files When Both Row and Key Cache Are Enabled

**Commit**: 57613dc8c1 · **Ticket**: CASSANDRA-6413
**Subsystem**: src/java/org/apache/cassandra/cache
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`AutoSavingCache.deleteOldCacheFiles()` used two independent `endsWith` checks: one for the cache-type suffix and a separate one for `CURRENT_VERSION + ".db"`. The second condition matched any file ending with that version suffix regardless of cache type. When both row and key cache were enabled, the version-suffix check could delete the freshly saved file belonging to the other cache type, corrupting its persistence. The fix combines both criteria into a single filter that only deletes files whose names contain both the specific cache type prefix and the version suffix.

### Before / After

```java
// BEFORE
if (file.isFile() && file.getName().endsWith(cacheType.toString())) {
    if (!file.delete()) logger.warn(...);
}
if (file.isFile() && file.getName().endsWith(CURRENT_VERSION + ".db")) {
    if (!file.delete()) logger.warn(...);
}
```

```java
// AFTER
if (file.getName().endsWith(cacheType.toString())
    || file.getName().endsWith(String.format("%s-%s.db", cacheType.toString(), CURRENT_VERSION)))
{
    if (!file.delete()) logger.warn(...);
}
```

---

## How to Spot

When a cleanup routine filters files by suffix, each condition should be scoped to the owning subsystem's namespace. An unqualified suffix check (e.g., just a version string) that is shared across multiple instances is a sign the filter is too broad and may hit files it does not own.

---

## Test

No test added.
