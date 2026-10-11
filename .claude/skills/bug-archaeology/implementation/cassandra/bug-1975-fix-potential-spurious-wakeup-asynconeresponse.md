# Bug 1975: Spurious Wakeup Not Handled in AsyncOneResponse

**Commit**: 453b6f56fb · **Ticket**: CASSANDRA-5690
**Subsystem**: src/java/org/apache/cassandra/net/AsyncOneResponse.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`AsyncOneResponse.get()` used a `ReentrantLock` with a `Condition` and called `condition.await()` exactly once. The Java Memory Model permits spurious wakeups from `await()` — a thread may return from the wait even when `signal()` was never called. Without a loop re-checking the `done` predicate, a spurious wakeup would cause `get()` to return `null` (since `result` was never set) instead of waiting for the actual response. The fix replaces the lock/condition pattern with `synchronized` and a `while (!done)` loop around `timedWait`, the canonical pattern for correct waiting.

### Before / After

```java
// BEFORE - single await() call vulnerable to spurious wakeup
if (!done.get())
{
    timeout = tu.toNanos(timeout);
    long overall_timeout = timeout - (System.nanoTime() - start);
    bVal = overall_timeout > 0 && condition.await(overall_timeout, TimeUnit.NANOSECONDS);
}
// bVal check done after lock release - window for missed signal
```

```java
// AFTER - while loop re-checks done predicate after every wakeup
synchronized (this)
{
    while (!done)
    {
        long overallTimeout = timeout - (System.nanoTime() - start);
        if (overallTimeout <= 0)
            throw new TimeoutException("Operation timed out.");
        TimeUnit.NANOSECONDS.timedWait(this, overallTimeout);
    }
}
```

---

## How to Spot

Any `condition.await()` or `Object.wait()` call not inside a `while (predicate)` loop is susceptible to spurious wakeups. During review, every wait call should be wrapped in a loop that re-evaluates the guard condition, per the Java documentation for `Object.wait()`.

---

## Test

No test added.
