**Commit**: 77ac31b36a · **Ticket**: KAFKA-18164
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            Node nodeToSend = metadata.fetch().nodeById(nodeId);
            log.trace("Building acknowledgements to send : {}", finalAcknowledgementsToSend);
            nodesWithPendingRequests.add(nodeId);
            inFlightAcknowledgements.forEach((tip, acks) -> {
    public void testServerDisconnectedOnShareAcknowledge() {
        acknowledgements.add(3L, AcknowledgeType.REJECT);
        assertTrue(shareConsumeRequestManager.hasCompletedFetches());

// After
                Node nodeToSend = metadata.fetch().nodeById(nodeId);
                nodesWithPendingRequests.add(nodeId);

                log.trace("Building acknowledgements to send : {}", finalAcknowledgementsToSend);

            Map<TopicIdPartition, Acknowledgements> acknowledgementsMapToClear =
                    incompleteAcknowledgements.isEmpty() ? acknowledgementsToSend : incompleteAcknowledgements;

            acknowledgementsMapToClear.forEach((tip, acks) -> {
            acknowledgementsMapToClear.clear();
            nextPartitions.clear();
            nextAcknowledgements.clear();
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18164: clear existing acknowledgements on share session epoch reset. (#18063).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
