**Commit**: 53a8b6189a · **Ticket**: KAFKA-17539
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
     * See {@link KafkaShareConsumer#clientInstanceId(Duration)}}

// After
import org.apache.kafka.common.metrics.KafkaMetric;
    /**
     * Add the provided application metric for subscription. This metric will be added to this client's metrics
     * that are available for subscription and sent as telemetry data to the broker.
     * The provided metric must map to an OTLP metric data point type in the OpenTelemetry v1 metrics protobuf message types.
     * Specifically, the metric should be one of the following:
     * <ul>
     *  <li>
     *     Sum: Monotonic total count meter (Counter). Suitable for metrics like total number of X, e.g., total bytes sent.
     *  </li>
     *  <li>
     *     Gauge: Non-monotonic current value meter (UpDownCounter). Suitable for metrics like current value of Y, e.g., current queue count.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17539: application metrics extension for share consumer (#18377).

**Test**
Test modified in KafkaShareConsumerMetricsTest.java covering the corrected behaviour.
