**Commit**: e02db7f23a · **Ticket**: KAFKA-19493
**Subsystem**: clients/src/main/java/org/apache/kafka/common/metrics/stats/Rate.java
**Category**: Wrong constant / default value

**Root cause**
Hourly `Rate` metrics (rebalance-rate-per-hour, failed-rebalance-rate-per-hour) were constructed with only the default `MetricConfig` windowing, which was 2 × 30-second samples = 60 seconds total. When computing a per-hour rate, the code divided an event count observed over 60 seconds by 60 seconds and then scaled to hours, yielding 60× the correct answer. The fix lets callers explicitly set a `timeWindowMs` of 1 hour on the `SampledStat` and use that explicit window in the rate denominator instead of the config-derived one.

**Before/After**
```java
// Before: uses default 60-second config window for an hourly metric
new Rate(TimeUnit.HOURS, new WindowedCount())

// After: explicitly forces a 1-hour window
new Rate(TimeUnit.HOURS, new WindowedCount(), 1)  // 1 hour window
```

**How to spot**
Any time a `Rate` is created with a unit larger than the default `MetricConfig` window (2 × 30 s = 60 s), the denominator will be truncated to the config window and the reported rate will be multiplied by `window / config_window`. Search for `new Rate(TimeUnit.HOURS` and confirm each one passes an explicit `window` argument matching the time unit.

**Test**
Existing unit tests in `AbstractCoordinator` and `ConsumerRebalanceMetricsManager` were updated to use the corrected constructor; the diff also includes parametrized regression tests verifying the correct hourly window is applied.
