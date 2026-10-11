**Commit**: dbc0e9af70 · **Ticket**: KAFKA-17248
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR adds a Reporter instance that will add streams client metrics to the telemetry pipeline. For testing, the PR adds a unit test. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.metrics.KafkaMetric;

    /**
     * Add the provided application metric for subscription.
     * This metric will be added to this client's metrics
     * that are available for subscription and sent as
     * telemetry data to the broker.
     * The provided metric must map to an OTLP metric data point
     * type in the OpenTelemetry v1 metrics protobuf message types.
     * Specifically, the metric should be one of the following:
     * <ul>
     *  <li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248: add reporter for getting client metrics into telemetry pipeline and test [3/n] (#17377).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
