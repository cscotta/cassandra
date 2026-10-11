**Commit**: c69a6b0e80 · **Ticket**: KAFKA-17248
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR adds the processId to Kafka Streams client instance metrics

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before

                        .map(rm -> rm.getScopeMetricsList().get(0).getMetrics(0).getName())

// After

import java.util.Optional;
            final String expectedProcessId = streams.metrics().values().stream()
                    .filter(metric -> metric.metricName().tags().containsKey("process-id"))
                    .map(metric -> metric.metricName().tags().get("process-id"))
                    .findFirst().orElseThrow();


            TestUtils.waitForCondition(() -> TelemetryPlugin.processId != null,
                    30_000,
                    "Never received the process id");

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248 - kip 1076 add process-id to get carried to telemetry results (#17630).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
