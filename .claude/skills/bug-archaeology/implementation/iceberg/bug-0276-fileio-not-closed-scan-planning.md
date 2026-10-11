# Bug 0276: FileIO created for remote scan planning is never closed

**Commit**: 39ed7e445361b2ff83887795e7e8d9f92ed45abc · **Ticket**: #15439

## Detection Rule

When a method creates a new resource (e.g., FileIO) and stores it in a field or returns it, verify that every code path — including error and cancellation paths — eventually closes that resource.

## Code Smell

A `loadFileIO(...)` call that assigns to a local field with no corresponding `close()` in a `finally` block, error handler, or resource tracker; the object's lifetime is unbounded even when the owning scan is abandoned or fails.

## Tags

- scope: universal
- category: resource-leak
- code-pattern: close-not-on-all-paths
- severity: performance
- subsystem: core/rest-scan-planning

## Root Cause

`RESTTableScan.fileIOForPlanId()` loaded a fresh `FileIO` instance (with scan-scoped storage credentials) each time remote scan planning ran, but nothing ever closed it. When a plan was cancelled due to a failure or timeout, only `cancelPlan()` was called; the `FileIO` holding open connections and credentials was silently abandoned. The fix registers every created `FileIO` in a `Caffeine` weak-key cache with a removal listener that calls `close()`, and explicitly invalidates the entry in the new `cleanupPlanResources()` path.

## Example

```java
// BEFORE
private FileIO fileIOForPlanId(List<Credential> storageCredentials) {
    return CatalogUtil.loadFileIO(
        catalogProperties.getOrDefault(CatalogProperties.FILE_IO_IMPL, DEFAULT_FILE_IO_IMPL),
        ImmutableMap.<String, String>builder()
            .putAll(catalogProperties)
            .put(RESTCatalogProperties.REST_SCAN_PLAN_ID, planId)
            .buildKeepingLast(),
        hadoopConf,
        storageCredentials.stream()
            .map(c -> StorageCredential.create(c.prefix(), c.config()))
            .collect(Collectors.toList()));
    // returned FileIO is never closed
}
```

```java
// AFTER
private static final Cache<RESTTableScan, FileIO> FILEIO_TRACKER =
    Caffeine.newBuilder()
        .weakKeys()
        .removalListener((scan, io, cause) -> { if (null != io) io.close(); })
        .build();

private FileIO fileIOForPlanId(List<Credential> storageCredentials) {
    FileIO ioForScan = CatalogUtil.loadFileIO(...);
    FILEIO_TRACKER.put(this, ioForScan);
    return ioForScan;
}

private void cleanupPlanResources() {
    cancelPlan();
    if (null != fileIOForPlanId) {
        FILEIO_TRACKER.invalidate(this);
        this.fileIOForPlanId = null;
    }
}
```
