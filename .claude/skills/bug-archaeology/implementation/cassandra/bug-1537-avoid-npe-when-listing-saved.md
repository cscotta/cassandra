# Bug 1537: Avoid NPE When Listing Saved Caches Directory

**Commit**: 1a0aaf0498 · **Ticket**: CASSANDRA-7632
**Subsystem**: src/java/org/apache/cassandra/cache
**Category**: Missing null / bounds check

---

## Root Cause

`AutoSavingCache` called `savedCachesDir.listFiles()` directly in an enhanced for-loop without guarding against a `null` return. `File.listFiles()` returns `null` when the path is not a directory or an I/O error occurs, which causes an immediate `NullPointerException` even though the `assert` above verifies the directory exists — the assert is a no-op in production JVMs running without `-ea`.

### Before / After

```java
// BEFORE
for (File file : savedCachesDir.listFiles())  // NPE if listFiles() returns null
{
    ...
}
```

```java
// AFTER
File[] files = savedCachesDir.listFiles();
if (files != null)
{
    for (File file : files)
    {
        ...
    }
}
else
    logger.warn("Could not list files in {}", savedCachesDir);
```

---

## How to Spot

Any direct iteration over `File.listFiles()` without a null check is a latent NPE. The contract of `File.listFiles()` explicitly states it may return `null`, so it must always be checked before use.

---

## Test

No test added.
