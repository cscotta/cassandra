**Commit**: 680b87174f · **Ticket**: KAFKA-19773
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Andrew Schofield <aschofield@apache.org>

**Before/After**
```java
// Before
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetry" />
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryPayload" />
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryReceiver" />
import org.apache.kafka.server.telemetry.ClientTelemetry;
import org.apache.kafka.server.telemetry.ClientTelemetryReceiver;
            types = Type.KRAFT,
                @ClusterConfigProperty(key = METRIC_REPORTER_CLASSES_CONFIG, value = "org.apache.kafka.clients.admin.ClientTelemetryTest$GetIdClientTelemetry"),
    /**
     * We should add a ClientTelemetry into plugins to test the clientInstanceId method Otherwise the
     * {@link org.apache.kafka.common.protocol.ApiKeys#GET_TELEMETRY_SUBSCRIPTIONS} command will not be supported
     * by the server
     **/

// After
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryExporter" />
      <allow class="org.apache.kafka.server.telemetry.ClientTelemetryExporterProvider" />
import org.apache.kafka.server.telemetry.ClientTelemetryExporter;
import org.apache.kafka.server.telemetry.ClientTelemetryExporterProvider;
            types = Type.KRAFT,
                @ClusterConfigProperty(key = METRIC_REPORTER_CLASSES_CONFIG, value = "org.apache.kafka.clients.admin.ClientTelemetryTest$TelemetryExporter"),
    public static class TelemetryExporter implements ClientTelemetryExporterProvider, MetricsReporter {
        public ClientTelemetryExporter clientTelemetryExporter() {
 *
 * @deprecated Since 4.2.0, use {@link ClientTelemetryExporterProvider} instead. This interface will be
 *             removed in Kafka 5.0.0. The new interface provides a {@link ClientTelemetryExporter}
 *             which includes additional context such as the push interval.
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19773: include push interval in clienttelemetryreceiver context (kip-1217) (#20672).

**Test**
Test modified in ClientTelemetryTest.java covering the corrected behaviour.
