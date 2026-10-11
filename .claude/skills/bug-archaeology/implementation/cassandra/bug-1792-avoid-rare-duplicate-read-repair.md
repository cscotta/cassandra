# Bug 1792: Avoid Rare Duplicate Read Repair Triggering

**Commit**: 09ab439b49 · **Ticket**: CASSANDRA-6606
**Subsystem**: src/java/org/apache/cassandra/service/ReadCallback.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`maybeResolveForRepair()` compared `received.get()` against `endpoints.size()` to decide whether to schedule a background read repair. Because `received` is an `AtomicInteger` incremented by multiple threads, a second thread could observe the same value after the first had already triggered repair, causing the repair to be scheduled twice. The fix passes the locally-incremented count `n` (already held by the calling thread) instead of re-reading the shared atomic, eliminating the window for a duplicate trigger.

### Before / After

```java
// BEFORE
protected void maybeResolveForRepair()
{
    if (blockfor < endpoints.size() && received.get() == endpoints.size())
```

```java
// AFTER
protected void maybeResolveForRepair(int n)
{
    if (blockfor < endpoints.size() && n == endpoints.size())
```

---

## How to Spot

When a method checks a shared counter condition by re-reading the atomic instead of using the value that was just incremented and returned by `incrementAndGet()`, a second concurrent caller can observe the same terminal value and re-enter the same branch. Always pass the return value of the atomic operation through to any subsequent guard rather than reading it again.

---

## Test

No test added.
