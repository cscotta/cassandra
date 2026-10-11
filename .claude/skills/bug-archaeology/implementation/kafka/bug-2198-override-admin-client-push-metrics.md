**Commit**: 493e4c1cc9 · **Ticket**: KAFKA-17248
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR disables metrics push for the AdminClient as the default. KafkaStreams enables metrics push for its internal AdminClient. Tests are included that assert an error if a user disables either the main consumer or admin client metrics push but Kafka Streams metrics push config is enabled. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
                                        true,
        try (AdminClientUnitTestEnv env = mockClientEnv()) {

// After
                                        false,
        try (AdminClientUnitTestEnv env = mockClientEnv(AdminClientConfig.ENABLE_METRICS_PUSH_CONFIG, "true")) {
        configs.put(AdminClientConfig.ENABLE_METRICS_PUSH_CONFIG, true);
    private static final Map<String, Object> ADMIN_CLIENT_OVERRIDES;
    static {
        final Map<String, Object> tempAdminClientDefaultOverrides = new HashMap<>();
        tempAdminClientDefaultOverrides.put(AdminClientConfig.ENABLE_METRICS_PUSH_CONFIG, "true");
        ADMIN_CLIENT_OVERRIDES = Collections.unmodifiableMap(tempAdminClientDefaultOverrides);
    }

        verifyClientTelemetryConfigs();
    private void verifyClientTelemetryConfigs() {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248: override admin client to push metrics true, test for case where streams metrics [4/n] (#17422).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
