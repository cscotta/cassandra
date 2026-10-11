# Bug 1722: FileCacheService memoryUsage Not Decremented on Cache Hit

**Commit**: 31cbdfd7ba · **Ticket**: CASSANDRA-6838
**Subsystem**: src/java/org/apache/cassandra/service/FileCacheService.java
**Category**: State not cleaned up

---

## Root Cause

`FileCacheService` tracks total buffer memory in `memoryUsage` so that it can enforce a cap and decide when to admit new entries. When a `RandomAccessReader` is handed back to a caller via `checkOut`, the buffer it holds is no longer "sitting idle" in the cache — the caller owns it. The code correctly incremented `memoryUsage` on `put`, but never decremented it on `checkOut`, so the counter grew monotonically and eventually prevented re-use of cached readers even when headroom existed.

### Before / After

```java
// BEFORE
RandomAccessReader result = instances.poll();
if (result != null)
    metrics.hits.mark();
return result;
```

```java
// AFTER
RandomAccessReader result = instances.poll();
if (result != null)
{
    metrics.hits.mark();
    memoryUsage.addAndGet(-result.getTotalBufferSize());
}
return result;
```

---

## How to Spot

For any resource pool that maintains a size/capacity counter, verify that every path removing an item from the pool decrements the counter, not just the path that adds items. Missing the subtraction on the "take" path is a classic accounting asymmetry.

---

## Test

No test added.
