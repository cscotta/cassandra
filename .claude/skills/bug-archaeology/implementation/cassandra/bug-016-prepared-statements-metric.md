# Bug 016: Avoid iterating all prepared statements when getting PreparedStatementsCacheSize metric

**Commit**: 2acc420e1c · **Ticket**: CASSANDRA-21038
**Subsystem**: CQL / Query Processing
**Category**: Wrong constant / default value

---

## Root Cause

The `preparedStatementsCacheMemoryUsedBytes()` method computed the cache size by iterating over every entry in the prepared statements cache and summing individual weights via `getSizeOfPreparedStatementForCache`. This made an O(n) linear scan on a hot path that is called whenever the `PreparedStatementsCacheSize` metric is sampled, causing unnecessary CPU overhead proportional to the number of prepared statements. The Caffeine cache already tracks the total weighted size internally via its eviction policy; the fix simply reads that pre-computed value directly.

### Before / After

```java
// BEFORE — O(n) manual iteration to sum weights
public static long preparedStatementsCacheMemoryUsedBytes()
{
    long preparedStatementsCacheMemoryUsedBytes = 0;
    for (Map.Entry<MD5Digest, Prepared> entry : preparedStatements.asMap().entrySet())
    {
        preparedStatementsCacheMemoryUsedBytes += getSizeOfPreparedStatementForCache(entry.getKey(), entry.getValue());
    }
    return preparedStatementsCacheMemoryUsedBytes;
}

// AFTER — read the already-maintained weighted size from the cache policy
public static long preparedStatementsCacheMemoryUsedBytes()
{
    return preparedStatements.policy().eviction()
                             .map(p -> p.weightedSize().orElse(0L))
                             .orElse(0L);
}
```

---

## How to Spot

When a metric-collection method re-derives a value that the underlying data structure already tracks internally, look for a dedicated API on that structure first. Caffeine (and Guava) caches expose `policy().eviction().get().weightedSize()` precisely for this purpose. Any loop that walks all cache entries just to compute an aggregate (sum, count, max) during a read-only metric sample is a candidate for replacement with a built-in O(1) accessor, especially if the metric is polled frequently.

---

## Test

No test added.
