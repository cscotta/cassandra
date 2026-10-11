**Commit**: da3643c6b4 · **Ticket**: KAFKA-18787
**Subsystem**: core / log / remote / RemoteIndexCache
**Category**: State not cleaned up

**Root cause**
`RemoteIndexCache` is initialized with the broker's `logDir` (the root log directory) so that stale `.deleted` and `.tmp` files left over from a previous run are cleaned up when the broker restarts. The test harness mistakenly passed `tpDir` (a per-topic-partition subdirectory) instead of `logDir`, so the cleanup path during re-initialization never scanned the correct directory. Stale/invalid files therefore survived broker restarts, causing the cache to fail to delete them.

**Before/After**
```scala
// Before — wrong directory passed to RemoteIndexCache
cache = new RemoteIndexCache(defaultRemoteIndexCacheSizeBytes, rsm, tpDir.toString)

// After — correct root log directory
cache = new RemoteIndexCache(defaultRemoteIndexCacheSizeBytes, rsm, logDir.toString)
```

**How to spot**
Any constructor or init path that is expected to clean up residual files from a previous run must be pointed at the correct root directory. When a test uses a subdirectory instead of the root, the cleanup code runs against an empty or irrelevant directory and cleanup failures remain hidden.

**Test**
`RemoteIndexCacheTest` — creates cache entries with `.deleted` and `.tmp` suffix files in `logDir`, restarts the cache, and asserts those files are absent after initialization.
