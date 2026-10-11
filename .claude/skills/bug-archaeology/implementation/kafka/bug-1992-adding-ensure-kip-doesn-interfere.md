**Commit**: 1c998f8ef3 · **Ticket**: KAFKA-17869
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Adding tests to ensure the KIP-1076 methods don't interfere with existing metrics in clients

Reviewers: Apoorv Mittal <amittal@confluent.io>, Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricChange(metric);
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricRemoval(metric);
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricChange(metric);
        if (clientTelemetryReporter.isPresent()) {
            ClientTelemetryReporter reporter = clientTelemetryReporter.get();
            reporter.metricRemoval(metric);

// After
    <allow class="org.apache.log4j.Level" />
        if (!metrics().containsKey(metric.metricName())) {
            clientTelemetryReporter.ifPresent(reporter -> reporter.metricChange(metric));
        } else {
            log.debug("Skipping registration for metric {}. Existing consumer metrics cannot be overwritten.", metric.metricName());
        if (!metrics().containsKey(metric.metricName())) {
            clientTelemetryReporter.ifPresent(reporter -> reporter.metricRemoval(metric));
        } else {
            log.debug("Skipping unregistration for metric {}. Existing consumer metrics cannot be removed.", metric.metricName());
        if (!metrics().containsKey(metric.metricName())) {
            clientTelemetryReporter.ifPresent(reporter -> reporter.metricChange(metric));
        } else {
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17869: adding tests to ensure kip-1076 doesn't interfere with consumer metrics[1/3] (#17781).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
