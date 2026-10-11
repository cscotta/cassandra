**Commit**: 4ed0a958e5 · **Ticket**: KAFKA-17248
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Implementation of KIP-1076 to allow for adding client application metrics to the KIP-714 framework

Reviewers: Apoorv Mittal <amittal@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
    api project(':clients')
import org.apache.kafka.common.message.GetTelemetrySubscriptionsRequestData;
import org.apache.kafka.common.requests.GetTelemetrySubscriptionsRequest;
import org.apache.kafka.common.requests.GetTelemetrySubscriptionsResponse;

// After
    api project(path: ':clients', configuration: 'shadow')
    testImplementation project(':group-coordinator')
      <allow class="org.apache.kafka.server.authorizer.AuthorizableRequestContext" />
      <allow class="org.apache.kafka.shaded.io.opentelemetry.proto.metrics.v1.MetricsData"/>
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetry" />
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryPayload" />
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryReceiver" />
import org.apache.kafka.common.telemetry.internals.ClientTelemetryReporter;
import org.apache.kafka.common.telemetry.internals.ClientTelemetryUtils;
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;
        Optional<ClientTelemetryReporter> clientTelemetryReporter;
            clientTelemetryReporter = CommonClientConfigs.telemetryReporter(clientId, config);
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248 - kip 1076 implementation (#17021).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
