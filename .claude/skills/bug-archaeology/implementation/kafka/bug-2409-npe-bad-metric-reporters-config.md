**Commit**: cd7670d884 · **Ticket**: KAFKA-17478
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;

// After
    // Init value is needed to avoid NPE in case of exception raised in the constructor
    private Optional<ClientTelemetryReporter> clientTelemetryReporter = Optional.empty();
    // Init value is needed to avoid NPE in case of exception raised in the constructor
    private Optional<ClientTelemetryReporter> clientTelemetryReporter = Optional.empty();
    // Init value is needed to avoid NPE in case of exception raised in the constructor
    private Optional<ClientTelemetryReporter> clientTelemetryReporter = Optional.empty();
    // Init value is needed to avoid NPE in case of exception raised in the constructor
    private Optional<ClientTelemetryReporter> clientTelemetryReporter = Optional.empty();
    @ParameterizedTest
    @EnumSource(GroupProtocol.class)
    public void testConstructorInvalidMetricReporters(GroupProtocol groupProtocol) {
        Properties props = new Properties();
```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17478 fix npe with bad metric.reporters config (#17086).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
