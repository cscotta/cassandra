**Commit**: fba01c42c8 · **Ticket**: KAFKA-17645

**Subsystem**: Tools / ProducerPerformance

**Category**: Wrong constant/default value

**Root cause**
`ProducerPerformance` added a warmup phase where records produced during warmup should not count toward steady-state statistics. The `Stats` constructor accepted a boolean `isSteadyState` flag to distinguish the two phases, but the warmup `Stats` instance was being constructed with the wrong flag value, causing warmup records to be included in the reported throughput and latency figures. The corrected construction properly marks the warmup stats object so its data is excluded from the final summary.

**Before/After**
```java
// Before: warmup stats incorrectly flagged as steady-state
Stats warmupStats = new Stats(reportingInterval, true);

// After: warmup stats correctly flagged as NOT steady-state
Stats warmupStats = new Stats(reportingInterval, false);
```

**How to spot**
If `ProducerPerformance` benchmarks show unexpectedly low latency or high throughput, suspect that warmup records are being mixed into the steady-state window. Look for `Stats` constructor calls where the `isSteadyState` argument does not match the intended phase (warmup vs. measurement).

**Test**
`ProducerPerformanceTest` verifies that warmup-phase records do not appear in the steady-state statistics summary.
