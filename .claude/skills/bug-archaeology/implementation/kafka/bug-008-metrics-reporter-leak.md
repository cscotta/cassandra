**Commit**: 71c0882148 · **Ticket**: KAFKA-20398
**Subsystem**: streams — StreamThread / StreamsThreadMetricsDelegatingReporter
**Category**: Resource leak

**Root cause**
Each time a `StreamThread` was created (including when a crashed thread was replaced via `StreamsUncaughtExceptionHandler`), a new `StreamsThreadMetricsDelegatingReporter` was added to the metrics registry. When the old thread shut down, its reporter was never removed. Over time — especially in applications with frequent thread crashes — this caused unbounded growth in the reporter list and associated metric objects. The reporter was a local variable inside the static factory method and not stored anywhere accessible to the shutdown path.

**Before/After**
```java
// Before — reporter created but not stored, never removed
final StreamsThreadMetricsDelegatingReporter reporter =
    new StreamsThreadMetricsDelegatingReporter(...);
streamsMetrics.metricsRegistry().addReporter(reporter);
// reporter goes out of scope; shutdown path has no reference

// After — stored as field, removed on shutdown
this.metricsReporter = metricsReporter;
...
// In completeShutdown():
streamsMetrics.metricsRegistry().removeReporter(metricsReporter);
```

**How to spot**
Any object registered with a global registry inside a factory method but not retained as a field is a resource leak candidate. The pattern `registry.addX(new X(...))` without a corresponding `registry.removeX(ref)` in the lifecycle `close()` / shutdown path should be flagged in code review.

**Test**
`StreamThreadTest` — updated all constructor calls to pass the `metricsReporter` argument. A dedicated leak test verifies that after thread shutdown the reporter is no longer present in the metrics registry.
