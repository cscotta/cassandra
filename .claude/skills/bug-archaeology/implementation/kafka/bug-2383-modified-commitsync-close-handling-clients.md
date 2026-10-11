**Commit**: 3a79fabacf · **Ticket**: KAFKA-17502
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Currently the code in ShareConsumeRequestManager works on the basis that there can only be one commitSync()/close() at a time. But there is a chance these calls timeout on the application thread, but are still sent later on the background thread. This will mean the incoming commitSync()/close() will not be processed, resulting in possible loss of acknowledgements. To cover this case, we will now have a list of AcknowledgeRequestStates to store the commitSyncs() and a separate requestState to store the close().

**Before/After**
```java
// Before
    private final Map<Integer, Pair<AcknowledgeRequestState>> acknowledgeRequestStates;
        if (!fetchMoreRecords || closing) {
        for (Map.Entry<Integer, Pair<AcknowledgeRequestState>> requestStates : acknowledgeRequestStates.entrySet()) {
                if (!isNodeFree(nodeId)) {
                    log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                } else if (isAsyncDone.get()) {
                    maybeBuildRequest(requestStates.getValue().getSyncRequest(), currentTimeMs, false, isAsyncDone).ifPresent(unsentRequests::add);
                log.trace("Completing acknowledgement on close");


// After
import java.util.LinkedList;
import java.util.Queue;
    private final Map<Integer, Tuple<AcknowledgeRequestState>> acknowledgeRequestStates;
        if (!fetchMoreRecords) {
        for (Map.Entry<Integer, Tuple<AcknowledgeRequestState>> requestStates : acknowledgeRequestStates.entrySet()) {
                if (isAsyncDone.get()) {
                    // We try to process the close request only if we have processed the async and the sync requests for the node.
                    if (requestStates.getValue().getSyncRequestQueue() == null) {
                        if (!isNodeFree(nodeId)) {
                            log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                        } else {
                            AcknowledgeRequestState closeRequestState = requestStates.getValue().getCloseRequest();
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17502: modified commitsync() and close() handling in clients (#17136).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
