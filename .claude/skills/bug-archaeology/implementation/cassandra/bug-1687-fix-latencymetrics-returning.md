# Bug 1687: Fix LatencyMetrics Returning Infinity

**Commit**: 3ca740119e · **Ticket**: CASSANDRA-6920
**Subsystem**: src/java/org/apache/cassandra/metrics
**Category**: Race condition / missing synchronization

---

## Root Cause

This commit applies the same fix as CASSANDRA-6920 to an older branch variant of `LatencyMetrics` that used `addMicro()` instead of `addNano()`. The root cause is identical: `addMicro()` updated `latency` and `totalLatency` in two separate unsynchronized writes, while `getRecentLatency()` read them in two separate unsynchronized reads. The interleaving of a write pair with a read pair could produce a zero denominator in the recent-latency calculation, yielding infinity.

### Before / After

```java
// BEFORE
public void addMicro(long micros)
{
    latency.update(micros, TimeUnit.MICROSECONDS);
    totalLatency.inc(micros);
    ...
}
```

```java
// AFTER
public void addMicro(long micros)
{
    synchronized (this)
    {
        latency.update(micros, TimeUnit.MICROSECONDS);
        totalLatency.inc(micros);
    }
    ...
}
```

`getRecentLatency()` received the same synchronized read block as in the `addNano` variant (bug-1686).

---

## How to Spot

Same heuristic as bug-1686: two logically paired counter updates or reads in adjacent unsynchronized statements, with a division that uses the delta of those counters as the denominator. Applying a fix to one code path but not a parallel path on another branch is a common source of recurrence.

---

## Test

No test added.
