**Commit**: 19d8a414ef · **Ticket**: KAFKA-15900
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.KafkaThread;
import java.util.concurrent.atomic.AtomicReference;
    private HeartbeatThread heartbeatThread = null;
        this(rebalanceConfig, logContext, client, metrics, metricGrpPrefix, time, Optional.empty());
                               Optional<ClientTelemetryReporter> clientTelemetryReporter) {
            if (heartbeatThread.hasFailed()) {
        if (heartbeatThread != null && heartbeatThread.hasFailed()) {
            heartbeatThread = new HeartbeatThread();
        HeartbeatThread thread;

// After
import java.util.function.Supplier;
    private final Optional<Supplier<BaseHeartbeatThread>> heartbeatThreadSupplier;
    private BaseHeartbeatThread heartbeatThread = null;
        this(rebalanceConfig, logContext, client, metrics, metricGrpPrefix, time, Optional.empty(), Optional.empty());
                               Optional<ClientTelemetryReporter> clientTelemetryReporter,
                               Optional<Supplier<BaseHeartbeatThread>> heartbeatThreadSupplier) {
        this.heartbeatThreadSupplier = heartbeatThreadSupplier;
            if (heartbeatThread.isFailed()) {
        if (heartbeatThread != null && heartbeatThread.isFailed()) {
            heartbeatThread = heartbeatThreadSupplier.orElse(HeartbeatThread::new).get();
        BaseHeartbeatThread thread;
    /**
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15900, kafka-18310: fix flaky test testoutdatedcoordinatorassignment and abstractcoordinatortest (#18945).

**Test**
Test modified in AbstractCoordinatorTest.java covering the corrected behaviour.
