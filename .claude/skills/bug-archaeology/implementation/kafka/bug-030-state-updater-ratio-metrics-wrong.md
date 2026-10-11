**Commit**: 3f5b877b83 · **Ticket**: KAFKA-18369
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/DefaultStateUpdater.java
**Category**: Wrong constant / default value

**Root cause**
The four `*-ratio` metrics for the Kafka Streams `DefaultStateUpdater` (`idle-ratio`, `active-restore-ratio`, `standby-update-ratio`, `checkpoint-ratio`) were computed as the ratio of sub-task time to total time _within a single iteration_ and then averaged with `Avg()` across iterations. This produced an average-of-ratios rather than a ratio-of-windowed-totals, making the metric sensitive to iteration length and not comparable to the analogous thread-level ratio metrics. The fix replaces `Avg` with `Value` (last-recorded value) backed by `WindowedSum` accumulators, so the ratio is computed over a rolling measurement window.

**Before/After**
```java
// Before (per-iteration ratio, averaged)
updaterMetrics.idleRatioSensor.record((double) totalWaitLatency / totalLatency, now);
// sensor backed by: new Avg()

// After (windowed sum ratio)
idleTimeWindowedSum.record(metricsConfig, idleTime, now);
runOnceLatencyWindowedSum.record(metricsConfig, totalLatency, now);
// then: idleTimeWindowedSum.measure(...) / runOnceLatencyWindowedSum.measure(...)
// sensor backed by: new Value()
```

**How to spot**
Metric sensors that use `Avg` for ratio metrics are suspicious when the iteration duration varies. An average of ratios is not the same as the ratio of sums; the correct pattern for "fraction of time spent" over a window is `WindowedSum(numerator) / WindowedSum(denominator)`.

**Test**
The commit updates existing unit and integration tests to use the new windowed semantics and adds upgrade-guide documentation.
