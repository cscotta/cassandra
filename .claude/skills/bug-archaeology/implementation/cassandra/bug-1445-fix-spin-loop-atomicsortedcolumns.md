# Bug 1445: Fix Spin Loop in AtomicSortedColumns

**Commit**: dee15a85c8 · **Ticket**: CASSANDRA-7546
**Subsystem**: src/java/org/apache/cassandra/db/AtomicBTreeColumns.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`AtomicBTreeColumns.addAllWithSizeDelta()` used a pure compare-and-swap retry loop to merge incoming column data into the memtable's B-tree. Under high write contention to the same partition, concurrent threads continuously invalidate each other's CAS attempts, producing a livelock (spin loop) that wastes CPU without making progress. The fix introduces a waste-tracking heuristic: if cumulative discarded allocation across retries exceeds a threshold, the thread escalates to pessimistic locking via `sun.misc.Unsafe.monitorEnter`, serializing writes to that partition until contention subsides.

### Before / After

```java
// BEFORE
while (true)
{
    Holder current = ref;
    // ... build tree ...
    if (tree != null && refUpdater.compareAndSet(this, current, new Holder(tree, deletionInfo)))
    {
        updater.finish();
        return updater.dataSize;
    }
    // retry unconditionally — can spin forever under contention
}
```

```java
// AFTER
boolean monitorOwned = false;
try {
    if (usePessimisticLocking()) {
        Locks.monitorEnterUnsafe(this);
        monitorOwned = true;
    }
    while (true) {
        // ... build tree ...
        if (tree != null && refUpdater.compareAndSet(...)) { ... return; }
        else if (!monitorOwned) {
            if (updateWastedAllocationTracker(updater.heapSize))
            { Locks.monitorEnterUnsafe(this); monitorOwned = true; }
        }
    }
} finally {
    if (monitorOwned) Locks.monitorExitUnsafe(this);
}
```

---

## How to Spot

An unbounded CAS retry loop is safe only when contention is brief and bounded. When the contested object can receive concurrent mutations from arbitrarily many threads writing to the same partition, a pure spin is unsafe. Any CAS loop that retries without any backoff, yield, or locking fallback should be audited for livelock risk under high concurrency, especially in hot-partition scenarios.

---

## Test

No test added.
