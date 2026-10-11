# Bug 1577: Fix Race in FileCacheService RemovalListener

**Commit**: 900f29c7f7 · **Ticket**: CASSANDRA-7278
**Subsystem**: src/java/org/apache/cassandra/service/FileCacheService.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The `RemovalListener` in `FileCacheService` iterated over a `ConcurrentLinkedQueue` with a for-each loop. A for-each loop takes a snapshot of the iterator at the start but does not drain the queue; concurrent writers can add new elements after iteration begins, and items are never removed from the queue during the loop, creating a race where items could be double-processed or missed. The correct approach is to `poll()` elements one at a time so each removal is atomic.

### Before / After

```java
// BEFORE – iterates without removing; subject to races on concurrent queue
for (RandomAccessReader reader : cachedInstances)
{
    memoryUsage.addAndGet(-1 * reader.getTotalBufferSize());
    reader.deallocate();
}
```

```java
// AFTER – atomically drains the queue
for (RandomAccessReader reader = cachedInstances.poll(); reader != null; reader = cachedInstances.poll())
{
    memoryUsage.addAndGet(-1 * reader.getTotalBufferSize());
    reader.deallocate();
}
```

---

## How to Spot

When a for-each loop iterates over a concurrent collection inside an eviction or removal callback, check whether the loop actually removes elements. Concurrent collections require explicit `poll()` / `remove()` calls to avoid double-processing or missed elements under concurrent access.

---

## Test

No test added.
