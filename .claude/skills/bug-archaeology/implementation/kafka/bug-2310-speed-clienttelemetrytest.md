**Commit**: 1b0e16ba65 · **Ticket**: KAFKA-17473
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @ClusterTest(types = Type.KRAFT,
            serverProperties = @ClusterConfigProperty(key = AdminClientConfig.METRIC_REPORTER_CLASSES_CONFIG,
                    value = "kafka.admin.ClientTelemetryTest$GetIdClientTelemetry"))

// After
import static org.apache.kafka.clients.admin.AdminClientConfig.METRIC_REPORTER_CLASSES_CONFIG;
    @ClusterTest(
            types = Type.KRAFT,
            brokers = 3,
            serverProperties = {
                    @ClusterConfigProperty(key = METRIC_REPORTER_CLASSES_CONFIG, value = "kafka.admin.ClientTelemetryTest$GetIdClientTelemetry"),
            })
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17473 speed up clienttelemetrytest (#17263).

**Test**
Test modified in ClientTelemetryTest.java covering the corrected behaviour.
