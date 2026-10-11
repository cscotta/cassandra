**Commit**: 7f8a592ad1 · **Ticket**: KAFKA-17869
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Adding producer tests to ensure the KIP-1076 methods don't interfere with existing metrics
Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricChange(metric);
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricRemoval(metric);

// After
        if (!metrics().containsKey(metric.metricName())) {
            clientTelemetryReporter.ifPresent(reporter -> reporter.metricChange(metric));
        }  else {
            log.debug("Skipping registration for metric {}. Existing producer metrics cannot be overwritten.", metric.metricName());
        if (!metrics().containsKey(metric.metricName())) {
            clientTelemetryReporter.ifPresent(reporter -> reporter.metricRemoval(metric));
        } else {
            log.debug("Skipping unregistration for metric {}. Existing producer metrics cannot be removed.", metric.metricName());
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.utils.LogCaptureAppender;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17869: adding tests to ensure kip-1076 doesn't interfere producer metrics[2/3] (#17783).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
