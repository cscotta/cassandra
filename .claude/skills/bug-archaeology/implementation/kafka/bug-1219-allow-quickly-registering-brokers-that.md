**Commit**: 6e446f0b05 · **Ticket**: KAFKA-19047
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Allow re-registration of brokers with active sessions if the previous broker registration was in controlled shutdown. Reviewers: Colin P. McCabe <cmccabe@apache.org>, Reviewers: José Armando García Sancio <jsancio@apache.org>, David Mao <dmao@confluent.io>

**Before/After**
```java
// Before
            if (heartbeatManager.hasValidSession(brokerId, existing.epoch())) {
                    throw new DuplicateBrokerRegistrationException("Another broker is " +
                        "registered with that broker id.");

// After
            if (heartbeatManager.hasValidSession(brokerId, existing.epoch()) && !existing.inControlledShutdown()) {
                    throw new DuplicateBrokerRegistrationException("Another broker is registered with that broker id. If the broker " +
                            "was recently restarted this should self-resolve once the heartbeat manager expires the broker's session.");
import org.apache.kafka.common.errors.DuplicateBrokerRegistrationException;
import java.util.concurrent.TimeUnit;
    @Test
    public void testDuplicateBrokerRegistrationWithActiveOldBroker() {
        // active here means brokerHeartbeatManager last recorded the broker as unfenced and not in controlled shutdown
        long brokerSessionTimeoutMs = 1000;
        MockTime time = new MockTime(0L, 20L, 1000L);
        FinalizedControllerFeatures finalizedFeatures = new FinalizedControllerFeatures(
            Map.of(MetadataVersion.FEATURE_NAME, MetadataVersion.LATEST_PRODUCTION.featureLevel()), 456L);
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19047: allow quickly re-registering brokers that are in controlled shutdown (#19296).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
