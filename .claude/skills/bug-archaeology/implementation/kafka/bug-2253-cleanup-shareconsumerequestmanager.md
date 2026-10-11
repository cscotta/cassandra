**Commit**: 8637b6a0ff · **Ticket**: KAFKA-17711
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
What
Minor cleanup and javadoc changes in ShareConsumeRequestManager. Reviewers:  Andrew Schofield <aschofield@confluent.io>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        AtomicBoolean isAsyncDone = new AtomicBoolean();
                isAsyncDone.set(false);
                // For commitAsync
                maybeBuildRequest(requestStates.getValue().getAsyncRequest(), currentTimeMs, true, isAsyncDone).ifPresent(unsentRequests::add);
                if (isAsyncDone.get()) {
                        if (!isNodeFree(nodeId)) {
                            log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                        } else {
                            AcknowledgeRequestState closeRequestState = requestStates.getValue().getCloseRequest();
                            maybeBuildRequest(closeRequestState, currentTimeMs, false, isAsyncDone).ifPresent(unsentRequests::add);
                        }
                            if (!isNodeFree(nodeId)) {

// After
        // The acknowledgements sent via ShareFetch are stored in this map.
        AtomicBoolean isAsyncSent = new AtomicBoolean();
                isAsyncSent.set(false);
                // First, the acknowledgements from commitAsync is sent.
                maybeBuildRequest(requestStates.getValue().getAsyncRequest(), currentTimeMs, true, isAsyncSent).ifPresent(unsentRequests::add);

                if (isAsyncSent.get()) {
                    if (!isNodeFree(nodeId)) {
                        log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                        continue;
                    }

```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17711: minor cleanup changes in shareconsumerequestmanager (#17392).

**Test**
No test added.
