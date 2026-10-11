**Commit**: 0dfc4017b8 · **Ticket**: KAFKA-18441
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                newStrMap(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000"))) {

// After
                newStrMap(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000",
                    // Default "metadata.recovery.strategy" is rebootstrap. If it meets "retry.backoff.ms" (default is 100L),
                    // following assertion will fail. Set it to none to avoid authentication error cleanup.
                    AdminClientConfig.METADATA_RECOVERY_STRATEGY_CONFIG, "none"))) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18441: fix flaky kafkaadminclienttest#testadminclientapisauthenticationfailure (#18735).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
