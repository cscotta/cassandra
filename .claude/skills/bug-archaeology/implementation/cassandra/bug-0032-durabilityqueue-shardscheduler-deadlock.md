# Bug 0032: DurabilityQueue/ShardScheduler Deadlock and Null Boolean Dereference

**Commit**: 13a3ae9297 · **Ticket**: CASSANDRA-21053
**Subsystem**: service/accord, utils/memory
**Category**: Deadlock

---

## Root Cause

Two independent bugs are addressed. First, a deadlock between `DurabilityQueue` and `ShardScheduler` in the Accord subsystem, caused by lock ordering: the two components acquired each other's locks in inconsistent order under concurrent execution. Second, `MemtableCleanerThread.Cleanup` unboxed a `Boolean` result from a `Future<Boolean>` callback directly in an `if` expression without a null guard; if an exception had been thrown during the task, the `Boolean` could be `null`, producing an NPE when the JVM auto-unboxed it.

Additionally, `AccordCache.tryApplyShrink` was adding a cache entry to the eviction queue unconditionally before checking whether it was still referenced or already enqueued — leading to a "shrink without lock" regression that could corrupt cache state.

### Before / After

```java
// BEFORE (MemtableCleanerThread)
if ((res || err != null) && pool.needsCleaning())
    wait.signal();
```

```java
// AFTER
if (((res != null && res) || err != null) && pool.needsCleaning())
    wait.signal();
```

```java
// BEFORE (AccordCacheEntry.tryApplyShrink)
void tryApplyShrink(Object cur, Object upd) {
    if (isLoaded() && unwrap() == cur && upd != cur && upd != null)
        applyShrink(owner.parent(), cur, upd);
    // caller unconditionally did: queue.addLast(node)
}
```

```java
// AFTER
void tryApplyShrink(Object cur, Object upd, IntrusiveLinkedList<AccordCacheEntry<?,?>> queue) {
    if (references() > 0 || !isUnqueued())
        return;
    if (isLoaded() && unwrap() == cur && upd != cur && upd != null)
        applyShrink(owner.parent(), cur, upd);
    queue.addLast(this);
}
```

---

## How to Spot

Whenever a method receives a `Boolean` (boxed) result from an asynchronous callback that may deliver `null` on exceptional paths, always guard the unbox with an explicit null check before using the value in a boolean expression. Similarly, when handing off an object to a data structure (like an eviction queue), check preconditions such as reference count and "not already enqueued" inside the same locked section where the hand-off occurs, rather than letting the caller perform an unconditional add after the call returns.

---

## Test

No new test added for the deadlock itself; `AccordDebugKeyspaceTest` was extended to cover ordering corrections in virtual tables.
