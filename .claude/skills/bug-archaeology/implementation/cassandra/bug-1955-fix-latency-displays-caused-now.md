# Bug 1955: Stress tool reported latencies 1000x too large after metrics-core upgrade

**Commit**: 5dabd1cc0c · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/StressAction.java, tools/stress/src/org/apache/cassandra/stress/StressStatistics.java
**Category**: Wrong constant / default value

---

## Root Cause

The metrics-core library changed its `Snapshot` percentile methods (e.g. `getMedian()`, `get95thPercentile()`) to return values in nanoseconds rather than milliseconds. The stress tool was displaying these values directly as milliseconds, causing every latency figure to appear 1,000,000 times larger than reality. The fix adds a `nanosToMillis` conversion helper and applies it at every call site before using the snapshot value for display or accumulation.

### Before / After

```java
// BEFORE
latency.getMedian(), latency.get95thPercentile(), latency.get999thPercentile()
// ...
tallyLatencySum += latency.getMedian();
tally95thLatencySum += latency.get95thPercentile();
```

```java
// AFTER
StressStatistics.nanosToMillis(latency.getMedian()),
StressStatistics.nanosToMillis(latency.get95thPercentile()),
StressStatistics.nanosToMillis(latency.get999thPercentile())
// ...
tallyLatencySum += nanosToMillis(latency.getMedian());
tally95thLatencySum += nanosToMillis(latency.get95thPercentile());
```

---

## How to Spot

After upgrading a metrics/monitoring library, audit every call site that reads timing values and confirm whether the new version's unit contract matches the old one. A unit mismatch is silent at compile time and only reveals itself as wildly incorrect output at runtime.

---

## Test

No test added.
