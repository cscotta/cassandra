**Commit**: e1f45218c9 · **Ticket**: KAFKA-19485
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
*What*
Currently when we received a top level error response in ShareFetch, we
would log the error, update the share session epoch and proceed to the
next request. But these acknowledgements(if any) are not completed and the callback
would not have been processed. PR aims to address this by completing these acknowledgements with the
error code from the response in this case. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
                // Complete any inFlight acknowledgements with the error code from the response.
                Map<TopicIdPartition, Acknowledgements> nodeAcknowledgementsInFlight = fetchAcknowledgementsInFlight.get(fetchTarget.id());
                if (nodeAcknowledgementsInFlight != null) {
                    nodeAcknowledgementsInFlight.forEach((tip, acks) -> {
                        acks.complete(Errors.forCode(response.error().code()).exception());
                        metricsManager.recordFailedAcknowledgements(acks.size());
                    });
                    maybeSendShareAcknowledgeCommitCallbackEvent(nodeAcknowledgementsInFlight);
                    nodeAcknowledgementsInFlight.clear();
                }
    @Test
    public void testPiggybackAcknowledgementsOnInitialShareSession_ShareSessionNotFound() {
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19485 (ii) : complete any pending acknowledgements in sharefetch on an error response. (#20247).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
