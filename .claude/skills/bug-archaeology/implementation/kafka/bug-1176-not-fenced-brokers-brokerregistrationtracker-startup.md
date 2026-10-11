**Commit**: c465abc458 · **Ticket**: KAFKA-19130
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
When the controller starts up (or becomes active after being inactive), we add all of the registered brokers to BrokerRegistrationTracker so that they will not be accidentally fenced the next time we are looking for a broker to fence. We do this because the state in BrokerRegistrationTracker is "soft state" (it doesn't appear in the metadata log), and the newly active controller starts off with no soft state. (Its soft state will be populated by the brokers sending heartbeat requests to it over time.)

In the case of fenced brokers, we are not worried about accidentally fencing the broker due to it being missing from
BrokerRegistrationTracker for a while (it's already fenced). Therefore, it ...

**Before/After**
```java
// Before
            heartbeatManager.tracker().updateContactTime(
                new BrokerIdAndEpoch(registration.id(), registration.epoch()), nowNs);
            if (heartbeatManager.hasValidSession(brokerId, existing.epoch()) && !existing.inControlledShutdown()) {
                    throw new DuplicateBrokerRegistrationException("Another broker is registered with that broker id. If the broker " +
        assertEquals(OptionalLong.of(1000L), clusterControl.heartbeatManager().tracker().
        assertEquals(OptionalLong.of(1000L), clusterControl.heartbeatManager().tracker().
        assertThrows(DuplicateBrokerRegistrationException.class, () ->
            clusterControl.registerBroker(new BrokerRegistrationRequestData().
                    setClusterId("pjvUwj3ZTEeSVQmUiH3IJw").
                    setBrokerId(0).
                    setLogDirs(List.of(Uuid.fromString("yJGxmjfbQZSVFAlNM3uXZg"))).
                    setFeatures(new BrokerRegistrationRequest

// After
            if (!registration.fenced()) {
                heartbeatManager.tracker().updateContactTime(
                    new BrokerIdAndEpoch(registration.id(), registration.epoch()), nowNs);
            }
            if (heartbeatManager.hasValidSession(brokerId, existing.epoch())) {
                    throw new DuplicateBrokerRegistrationException("Another broker is registered with that broker id. If the broker " +
            setFenced(false).
        assertEquals(OptionalLong.empty(), clusterControl.heartbeatManager().tracker().
        assertEquals(OptionalLong.empty(), clusterControl.heartbeatManager().tracker().
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19130: do not add fenced brokers to brokerregistrationtracker on startup (#19454).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
