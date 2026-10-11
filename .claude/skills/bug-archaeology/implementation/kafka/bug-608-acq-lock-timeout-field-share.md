**Commit**: 169e211997 · **Ticket**: KAFKA-19892
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
* Add field `AcquisitionLockTImeoutMs` to `ShareAcknowledgeResponse` to
communicate the timeout to the share consumer. * This becomes useful with addition of RENEW ack type in KIP-1222. * Tests have been updated. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                                              List<Node> nodeEndpoints) {
        return new ShareAcknowledgeResponse(toMessage(error, throttleTimeMs, responseData.entrySet().iterator(), nodeEndpoints));
                                                         List<Node> nodeEndpoints) {
        return ShareAcknowledgeResponse.of(Errors.NONE, 0, new LinkedHashMap<>(partitions), Collections.emptyList());
        return ShareAcknowledgeResponse.of(error, 0, new LinkedHashMap<>(partitions), Collections.emptyList());
        return ShareAcknowledgeResponse.of(Errors.NONE, 0, new LinkedHashMap<>(partitions), Collections.emptyList());

// After
                                              List<Node> nodeEndpoints, int acquisitionLockTimeout) {
        return new ShareAcknowledgeResponse(toMessage(error, throttleTimeMs, responseData.entrySet().iterator(), nodeEndpoints, acquisitionLockTimeout));
                                                         List<Node> nodeEndpoints, int acquisitionLockTimeout) {
                .setAcquisitionLockTimeoutMs(acquisitionLockTimeout)
    { "name": "AcquisitionLockTimeoutMs", "type": "int32", "versions": "2+",
      "about": "The time in milliseconds for which the acquired records are locked." },
        return ShareAcknowledgeResponse.of(Errors.NONE, 0, new LinkedHashMap<>(partitions), Collections.emptyList(), 0);
        return ShareAcknowledgeResponse.of(error, 0, new LinkedHashMap<>(partitions), Collections.emptyList(), 0);
        return ShareAcknowledgeResponse.of(Errors.NONE, 0, new LinkedHashMap<>(partitions), Collections.emptyList(), 0);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19892: add acq lock timeout ms field to share ack response. (#20901).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
