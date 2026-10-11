# Bug 1686: Fix LatencyMetrics Returning Infinity

**Commit**: 5e51aa4e6d · **Ticket**: CASSANDRA-6920
**Subsystem**: src/java/org/apache/cassandra/metrics
**Category**: Race condition / missing synchronization

---

## Root Cause

`LatencyMetrics.addNano()` updated `latency` (a Meter) and `totalLatency` (a Counter) in two separate, unsynchronized steps. `getRecentLatency()` then read both counters without synchronization. Because the two reads in `getRecentLatency()` and the two writes in `addNano()` were not atomic with respect to each other, it was possible to read a stale `ops` count (not yet incremented) against an already-incremented `totalLatency`, or vice versa, yielding a denominator of zero and returning positive or negative infinity.

### Before / After

```java
// BEFORE
public void addNano(long nanos)
{
    latency.update(nanos, TimeUnit.NANOSECONDS);
    totalLatency.inc(nanos / 1000);
    ...
}

public double getRecentLatency()
{
    long ops = latency.count();
    long n   = totalLatency.count();
    ...
    return ((double) n - lastLatency) / (ops - lastOpCount);
}
```

```java
// AFTER
public void addNano(long nanos)
{
    synchronized (this)
    {
        latency.update(nanos, TimeUnit.NANOSECONDS);
        totalLatency.inc(nanos / 1000);
    }
    ...
}

public double getRecentLatency()
{
    long ops, n;
    synchronized (this)
    {
        ops = latency.count();
        n   = totalLatency.count();
    }
    ...
    return ((double) n - lastLatency) / (ops - lastOpCount);
}
```

---

## How to Spot

Any time two logically paired counters are updated or read in separate statements without a shared lock, a reader can see them in an inconsistent intermediate state. Division by a delta of two counters that must stay in sync is especially dangerous; if the denominator can momentarily be zero due to a torn read, the result is infinity or NaN. Paired counter updates should always be placed inside a single synchronized block.

---

## Test

No test added.
