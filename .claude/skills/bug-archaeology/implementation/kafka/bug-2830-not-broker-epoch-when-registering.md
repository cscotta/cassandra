**Commit**: 2fd00ce536 · **Ticket**: KAFKA-16952
**Subsystem**: metadata/src
**Category**: State not cleaned up

**Root cause**
* KAFKA-16952: Do not bump broker epoch when re-registering the same incarnation

As part of KIP-858 (Handle JBOD broker disk failure in KRaft), we added some code that caused the
broker to re-register itself when transitioning from a MetadataVersion that did not support broker
directory IDs, to one that did. This code was necessary because otherwise the controller would not
be aware of what directories the broker held. However, prior to this PR, the re-registration process acted exactly like a full registration. That
is, it bumped the broker epoch (which is meant to only be bumped on broker restart).

**Before/After**
```java
// Before
            BrokerRegistrationRequestData request,
            long brokerEpoch,
            FinalizedControllerFeatures finalizedFeatures,
            short version) {
        if (version < 2 || existing == null || request.previousBrokerEpoch() != existing.epoch()) {
            log.debug("Received an unclean shutdown request");
            brokerUncleanShutdownHandler.addRecordsForShutdown(request.brokerId(), records);
        }
                if (!existing.incarnationId().equals(request.incarnationId())) {
            } else {
                if (!existing.incarnationId().equals(request.incarnationId())) {
                    // Remove any existing session for the old broker incarnation.

// After
    String clusterId() { // Visible for testing
        return clusterId;
    }

        BrokerRegistrationRequestData request,
        long newBrokerEpoch,
        FinalizedControllerFeatures finalizedFeatures
    ) {
        Uuid prevIncarnationId = null;
            prevIncarnationId = existing.incarnationId();
                if (!request.incarnationId().equals(prevIncarnationId)) {
        if (!request.incarnationId().equals(prevIncarnationId)) {
```

**How to spot**
When reviewing metadata/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16952: do not bump broker epoch when re-registering the same incarnation (#16333).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
