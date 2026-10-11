**Commit**: 44c6e956ed · **Ticket**: KAFKA-19529
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
All state updater threads use the same metrics instance, but do not use
unique names for their sensors. This can have the following symptoms:

1) Data inserted into one sensor by one thread can affect the metrics of
all state updater threads. 2) If one state updater thread is shutdown, the metrics associated to
all state updater threads are removed. 3) If one state updater thread is started, while another one is removed,
it can happen that a metric is registered with the `Metrics` instance,
but not associated to any `Sensor` (because it is concurrently removed),
which means that the metric will not be removed upon shutdown.

**Before/After**
```java
// Before
                                  final Metrics metrics,
    private final Metrics metrics;
                               final Metrics metrics,
        private final Deque<String> allSensorNames = new LinkedList<>();
        private StateUpdaterMetrics(final Metrics metrics, final String threadId) {
            MetricName metricName = metrics.metricName("active-restoring-tasks",
            metrics.addMetric(metricName, (config, now) -> stateUpdaterThread != null ?
            metricName = metrics.metricName("standby-updating-tasks",
            metrics.addMetric(metricName, (config, now) -> stateUpdaterThread != null ?
            metricName = metrics.metricName("active-paused-tasks",
            metrics.addMetric(metricName, (config, now) -> stateUpdaterThr

// After
import org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl;
                                  final StreamsMetricsImpl metrics,
    private final StreamsMetricsImpl metrics;
                               final StreamsMetricsImpl metrics,
        private final Deque<Sensor> allSensors = new LinkedList<>();
        private StateUpdaterMetrics(final StreamsMetricsImpl metrics, final String threadId) {
            final Metrics metricsRegistry = metrics.metricsRegistry();
            MetricName metricName = metricsRegistry.metricName("active-restoring-tasks",
            metricsRegistry.addMetric(metricName, (config, now) -> stateUpdaterThread != null ?
            metricName = metricsRegistry.metricName("standby-updating-tasks",
            metricsRegistry.addMetric(metricName, (config, now) -> stateUpdaterThread != null ?
            metricName = metricsRegistry.metricName("active-paused-tasks",
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19529: state updater sensor names should be unique (#20262).

**Test**
Test modified in DefaultStateUpdaterTest.java covering the corrected behaviour.
