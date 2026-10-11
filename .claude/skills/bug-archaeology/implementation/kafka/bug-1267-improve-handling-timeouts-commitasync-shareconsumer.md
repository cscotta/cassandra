**Commit**: 91758cc99d · **Ticket**: KAFKA-18899
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Previously, the `ShareConsumer.commitAsync()` method retried sending
`ShareAcknowledge` requests indefinitely. Now it will instead use the
defaultApiTimeout config to expire the request so that it does not retry forever. PR also fixes a bug in processing `commitSync() `requests, where we
need an additional check if the node is free. Co-authored-by: Andrew Schofield <aschofield@confluent.io>
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            if (acknowledgeRequestState == null || (!acknowledgeRequestState.isCloseRequest() && acknowledgeRequestState.isEmpty())) {
    public void commitAsync(final Map<TopicIdPartition, NodeAcknowledgements> acknowledgementsMap) {
                                        Long.MAX_VALUE,
        String topicName = metadata.topicNames().getOrDefault(topicId,
                topicNamesMap.remove(new IdAndPartition(topicId, partitionIndex)));

// After
                            if (!isNodeFree(nodeId)) {
                                log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                                break;
                            }
            if (acknowledgeRequestState == null ||
                    (!acknowledgeRequestState.isCloseRequest() && acknowledgeRequestState.isEmpty()) ||
                    (acknowledgeRequestState.isCloseRequest() && acknowledgeRequestState.isProcessed)) {
                // Reset timer for any future processing on the same request state.
                acknowledgeRequestState.maybeResetTimerAndRequestState();
     * @param deadlineMs          Time until which the request will be retried if it fails with
     *                            an expected retriable error.
    public void commitAsync(
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18899: improve handling of timeouts for commitasync() in shareconsumer. (#19192).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
