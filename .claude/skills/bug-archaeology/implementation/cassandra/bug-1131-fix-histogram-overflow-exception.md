# Bug 1131: Histogram Overflow Exception in EstimatedHistogramReservoir

**Commit**: 23826f7a26 · **Ticket**: CASSANDRA-9973
**Subsystem**: src/java/org/apache/cassandra/metrics/EstimatedHistogramReservoir.java
**Category**: Wrong constant / default value

---

## Root Cause

`EstimatedHistogramReservoir` was constructed with a default bucket count of 128. The underlying `EstimatedHistogram` allocates buckets that cover an exponential range of values; with 128 buckets the maximum representable value in nanoseconds was less than 4 hours of latency. Any observation exceeding the highest bucket caused an overflow exception. Raising the default to 164 buckets extends the coverage to over 4 hours of nanoseconds, which is sufficient for any realistic operation latency tracked by these metrics.

### Before / After

```java
// BEFORE: 128 buckets, max ~hours < 4 -- overflows on large latencies
public EstimatedHistogramReservoir()
{
    this(128);
}
```

```java
// AFTER: 164 buckets, > 4 hours in nanoseconds
// Default to >4 hours of in nanoseconds of buckets
public EstimatedHistogramReservoir()
{
    this(164);
}
```

---

## How to Spot

When a histogram or reservoir class exposes a no-argument constructor that hard-codes a bucket or capacity constant, verify that the constant is sized for the worst-case input the system can produce, not just the common case. For time-based histograms, compute the maximum representable nanosecond value at the chosen bucket count and compare it against realistic upper-bound latencies (minutes to hours for operations subject to GC, network, or disk stalls). An off-by-construction default that seems large enough in normal operation will throw or silently saturate under load.

---

## Test

No dedicated test added; the fix is a constant change validated by the comment "Default to >4 hours of in nanoseconds of buckets."
