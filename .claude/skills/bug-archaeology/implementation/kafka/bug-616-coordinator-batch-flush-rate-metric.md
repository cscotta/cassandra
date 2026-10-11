**Commit**: e990941f5a · **Ticket**: KAFKA-19825
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
Add a batch flush rate metric for the group coordinator and share
coordinator. Once we allow the batch linger time to be adaptive, the
batch flush rate becomes less predictable. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * Sensor to measure the flush time.
    private final Sensor flushTimeSensor;
        this.flushTimeSensor = metrics.sensor(this.metricsGroup + "-FlushTime");
        this.flushTimeSensor.add(flushTimeHistogram);
        metrics.removeSensor(flushTimeSensor.name());
        flushTimeSensor.record(durationMs);
            kafkaMetricName(metrics, "batch-flush-time-ms-p999")

// After
import org.apache.kafka.common.metrics.stats.WindowedCount;
     * Sensor to measure the flush time and rate.
    private final Sensor flushSensor;
        this.flushSensor = metrics.sensor(this.metricsGroup + "-Flush");
        this.flushSensor.add(flushTimeHistogram);
        this.flushSensor.add(
            metrics.metricName(
                "batch-flush-rate",
                this.metricsGroup,
                "The flushes per second."),
            new Rate(TimeUnit.SECONDS, new WindowedCount()));
        metrics.removeSensor(flushSensor.name());
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19825 [2/2]: add coordinator batch flush rate metric (#20754).

**Test**
Test modified in CoordinatorRuntimeMetricsImplTest.java covering the corrected behaviour.
