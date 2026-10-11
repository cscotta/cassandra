**Commit**: 930f165546 · **Ticket**: KAFKA-17248
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR adds a Reporter instance that will add streams thread metrics to the telemetry pipeline. For testing, the PR adds a unit test. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.metrics.KafkaMetric;
    /**
     * @see KafkaConsumer#registerMetricForSubscription(KafkaMetric)
     */
    void registerMetricForSubscription(KafkaMetric metric);

    /**
     * @see KafkaConsumer#unregisterMetricFromSubscription(KafkaMetric)
     */
    void unregisterMetricFromSubscription(KafkaMetric metric);
import org.apache.kafka.common.metrics.KafkaMetric;

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248: add reporter for adding thread metrics to telemetry pipeline and a test [2/n] (#17376).

**Test**
Test modified in StreamsThreadMetricsDelegatingReporterTest.java covering the corrected behaviour.
