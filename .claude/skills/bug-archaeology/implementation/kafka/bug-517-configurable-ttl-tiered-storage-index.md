**Commit**: 59e4af3f2c · **Ticket**: KAFKA-19970
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Caffeine cache currently only uses size-based eviction with frequency
buckets. Within same frequency bucket,
larger entries are always evicted. This causes old (smaller) index files
to stay in cache indefinitely while
newer indices thrash, resulting in:
- Poor cache utilization for backfill workloads
- Higher fetch errors
- Suboptimal memory usage

This problem is exacerbated when a broker is no longer the leader for a
partition  or when read-from-replica is enabled, as stale indexes can
accumulate without  being cleared. Solution:
---------
Add time-based eviction (expireAfterAccess) via a new internal config
remote.log.index.file.cache.ttl.ms (default: 1 hour).

**Before/After**
```java
// Before
        indexCache = new RemoteIndexCache(rlmConfig.remoteLogIndexFileCacheTotalSizeBytes(), remoteStorageManagerPlugin.get(), logDir);

// After
        case RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TTL_MS_PROP => assertPropertyInvalid(baseProperties, name, "not_a_number", -2)
        indexCache = new RemoteIndexCache(
            rlmConfig.remoteLogIndexFileCacheTotalSizeBytes(),
            rlmConfig.remoteLogIndexFileCacheTtlMs(),
            false,
            remoteStorageManagerPlugin.get(),
            logDir);
    public static final String REMOTE_LOG_INDEX_FILE_CACHE_TTL_MS_PROP = "remote.log.index.file.cache.ttl.ms";
    public static final String REMOTE_LOG_INDEX_FILE_CACHE_TTL_MS_DOC = "The maximum time in milliseconds an index file entry can remain in the cache " +
            "after its last access. After this duration, the entry will be evicted even if there is available space. " +
            "This helps prevent stale index files from remaining in cache indefinitely, particularly when a broker is no longer the leader " +
            "for a partition or when read-from-replica is enabled. Evicted index files are automatically re-fetched from remote storage when needed. " +
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19970: add configurable ttl for tiered storage index cache eviction (#21089).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
