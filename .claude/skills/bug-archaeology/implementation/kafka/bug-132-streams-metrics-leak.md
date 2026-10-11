**Commit**: 0dd8471174 · **Ticket**: KAFKA-19748
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/metrics/StreamsMetricsImpl.java
**Category**: Resource leak

**Root cause**
`OpenIterators.remove()` called `streamsMetrics.removeMetric(metricName)` when the last open iterator was closed. `removeMetric` removes the metric from the Kafka `Metrics` registry but does not remove the corresponding entry from `StreamsMetricsImpl.storeLevelMetrics`, a separate tracking map used to restore metrics after task migration. The stale entry in `storeLevelMetrics` caused a memory leak as the per-store metric names accumulated over time.

**Before/After**
```java
// BEFORE – only removes from the Kafka Metrics registry
public void removeMetric(final MetricName metricName) {
    metrics.removeMetric(metricName);
    // storeLevelMetrics NOT updated – leak
}

// AFTER – new method also cleans storeLevelMetrics
public void removeStoreLevelMetric(final MetricName metricName) {
    metrics.removeMetric(metricName);
    // ...resolve scope tag...
    Deque<MetricName> metricsForStore = storeLevelMetrics.get(storeSensorPrefix(...));
    if (metricsForStore != null) {
        metricsForStore.remove(metricName);
    }
}
```

**How to spot**
Whenever a component maintains two parallel data structures (here: Kafka `Metrics` registry and `storeLevelMetrics`), every removal path must update both. If removal helpers diverge, the secondary structure silently accumulates stale entries. Search for sites that call only `removeMetric` where a store-level context should also be cleaned.

**Test**
Unit tests in `StreamsMetricsImplTest` were updated to verify that `removeStoreLevelMetric` (not `removeMetric`) is invoked. No logic-level integration test was added.
