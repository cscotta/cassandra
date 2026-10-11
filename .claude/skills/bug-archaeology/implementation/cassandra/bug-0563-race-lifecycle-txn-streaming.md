# Bug 0563: Race Condition on LifecycleTransaction During Streaming

**Commit**: 3539a07884 · **Ticket**: CASSANDRA-14554
**Subsystem**: src/java/org/apache/cassandra/streaming, src/java/org/apache/cassandra/db/lifecycle
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamSession.getTransaction(cfId)` returned the raw `LifecycleTransaction` held by `StreamReceiveTask`. `StreamReader` would then call `txn.trackNew(sstable)` on it from the network I/O thread while `StreamReceiveTask` could be completing (calling `txn.abort()` or `txn.commit()`) from the streaming executor thread. Because `LifecycleTransaction` is not thread-safe, concurrent access to its internal log state produced corruption or assertion failures. The fix introduced `LifecycleNewTracker`, a narrow interface that only exposes `trackNew`/`untrackNew`, and wrapped all calls to it in a `synchronized (StreamReceiveTask.this)` block, so the tracker and the owning task share one intrinsic lock.

### Before / After

```java
// BEFORE - StreamSession: returns raw LifecycleTransaction (unsynchronized)
public LifecycleTransaction getTransaction(UUID cfId) {
    return receivers.get(cfId).getTransaction();
}
// StreamReader uses it unsynchronized from I/O thread
cfs.createSSTableMultiWriter(..., session.getTransaction(cfId));
```

```java
// AFTER - synchronized proxy wrapping the real txn
public synchronized LifecycleNewTracker createLifecycleNewTracker() {
    return new LifecycleNewTracker() {
        public void trackNew(SSTable table) {
            synchronized (StreamReceiveTask.this) { txn.trackNew(table); }
        }
        public void untrackNew(SSTable table) {
            synchronized (StreamReceiveTask.this) { txn.untrackNew(table); }
        }
    };
}
```

---

## How to Spot

When a shared mutable object is retrieved via a getter from one thread and then mutated while another thread may be completing a transaction on the same object, look for the absence of a common lock. A reviewer should ask: is every path that calls mutating methods on this object guarded by the same lock? Returning a raw `LifecycleTransaction` from a `synchronized` method is deceptive — it provides atomicity only for the retrieval, not for the subsequent mutations performed by the caller.

---

## Test

No dedicated test added (existing `ScrubTest` updated to use the new interface signature).
