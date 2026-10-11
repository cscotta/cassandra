# Bug 1237: Fix Counting Cache Serialization in Request Metrics

**Commit**: 04287d440a · **Ticket**: CASSANDRA-9466
**Subsystem**: src/java/org/apache/cassandra/service/CacheService.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

During key-cache serialization (saving the cache to disk), `CacheService.KeyCacheSerializer.serialize` fetched each entry using the public `keyCache.get(key)` method. The public `get` method in `InstrumentingCache` records a metrics hit or miss for every access. As a result, the act of iterating and serializing the cache contents was polluting the request-level hit/miss counters with artificial accesses, inflating hit counts and distorting cache effectiveness metrics. The fix uses `getInternal`, which retrieves the value directly without touching the metrics.

### Before / After

```java
// BEFORE: serialization path increments request hit/miss metrics
RowIndexEntry entry = CacheService.instance.keyCache.get(key);
```

```java
// AFTER: serialization path bypasses metrics recording
RowIndexEntry entry = CacheService.instance.keyCache.getInternal(key);
```

---

## How to Spot

Any internal, non-request-driven access to a metrics-instrumented cache (serialization, eviction scanning, health checks, warm-up) should use a bypass accessor that does not update request counters. When reviewing cache access patterns, distinguish between user-request reads that should be metered and administrative reads that should not. The presence of `cache.get()` in a serialization or background-maintenance method is a red flag.

---

## Test

No test added.
