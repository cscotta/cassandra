# Bug 0352: DigestResolver Signaled Before Responses Are Visible to All Threads

**Commit**: f9d41ff836 · **Ticket**: CASSANDRA-16883
**Subsystem**: src/java/org/apache/cassandra/service (ReadCallback, DigestResolver)
**Category**: Race condition / missing synchronization

---

## Root Cause

`ReadCallback.response()` increments an atomic counter for received responses but signals the waiting thread as soon as `n >= blockfor && resolver.isDataPresent()`, without verifying that the backing `responses` array inside `DigestResolver` has been fully published to all threads.  Because the `ConcurrentLinkedQueue` in `DigestResolver` and the atomic counter in `ReadCallback` are two independent memory locations, a thread that wakes up on the condition signal can call `resolver.resolve()` before all the required responses have been made visible via the queue, causing the resolver to see fewer responses than expected or to assert-fail.

### Before / After

```java
// BEFORE
if (n >= blockfor && resolver.isDataPresent())
{
    condition.signalAll();
    ...
}
```

```java
// AFTER
if (n >= blockfor && resolver.responses.size() >= blockfor && resolver.isDataPresent())
{
    condition.signalAll();
    ...
}
```

---

## How to Spot

Whenever a liveness or completion check is split across two independent data structures — an atomic counter and a collection — beware that the counter can advance ahead of the collection due to CPU reordering or JIT scheduling.  Any code that signals a waiting thread must ensure every data structure that the woken thread will read has been fully published, not just the counter used to decide whether to signal.

---

## Test

Concurrent two-thread test that races both `response()` calls against each other, then calls `awaitResults()` followed by `resolver.resolve()`, asserting the result has at least one row; run repeatedly for 2 minutes to expose the window.
