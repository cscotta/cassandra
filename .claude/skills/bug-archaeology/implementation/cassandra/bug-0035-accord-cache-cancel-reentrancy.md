# Bug 0035: Cache Modification Reentrancy When Cancelling Accordion Loads

**Commit**: 7b12404a4e · **Ticket**: CASSANDRA-20821
**Subsystem**: service/accord
**Category**: Race condition / missing synchronization

---

## Root Cause

When an `AccordTask` was cancelled from the owning executor thread (via `executeDirectlyWithLock`), it directly manipulated the cache and task queues while already holding the executor lock. This reentrant modification of the cache — which itself acquires and releases its own internal state — could corrupt cache state and trigger `ConcurrentModificationException`. The fix routes cancellations through the same asynchronous `submit` pathway used for all other operations, so cancellation is processed as a serialised `CancelAsync` message rather than an inline reentrant call. A separate sub-bug: if `Task.cleanupExclusive()` threw an exception, the surrounding cleanup code was abandoned (running/owner state left dirty), corrupting the `SequentialExecutor`. Wrapping the call in a `try/finally` ensures cleanup always completes.

### Before / After

```java
// BEFORE: AccordTask.cancel() — inline reentrant cache access
public void cancel() {
    executeDirectlyWithLock(() -> {
        // directly touches queue while lock already held
        if (queue.contains(this)) { queue.remove(this); ... }
    });
}
```

```java
// AFTER: routed through async submit
public void cancel() {
    submit((e, c) -> c.cancelExclusive(e), CancelAsync::new, this);
}
```

```java
// BEFORE: cleanupTask — exception skips state reset
task.cleanupExclusive();
running = false; owner = null; ...

// AFTER
try { task.cleanupExclusive(); }
finally { owner = null; running = false; ... }
```

---

## How to Spot

When a method that holds a lock calls back into code that also modifies shared state normally protected by that same lock, look for reentrancy hazards. Cancellation paths in particular are often written as "fast path" shortcuts that bypass the normal serialisation contract. Any time a cancel/abort callback manipulates a queue or cache directly without going through the same submission pipeline, check whether it can be reachable from the holding thread.

---

## Test

`AccordLoadTest` was adjusted to guard flush calls during early startup; no dedicated concurrency regression test was added.
