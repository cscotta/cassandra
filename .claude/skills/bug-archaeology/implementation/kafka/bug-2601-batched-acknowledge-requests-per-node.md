**Commit**: 0cbc5e083a · **Ticket**: KAFKA-17217
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
In ShareConsumeRequestManager, currently every time we perform a commitSync/commitAsync/acknowledgeOnClose we create one AcknowledgeRequestState for each call. But this can be optimised further as we can batch up the acknowledgements to be sent to the same node before the next poll() is invoked. This will ensure that between 2 polls, the acknowledgements are accumulated in one request per node and then sent during poll, resulting in lesser RPC calls. To achieve this, we are storing a pair of acknowledge request states for every node, the first value denotes the requestState for commitAsync() and the second value denotes the requestState for commitSync() and acknowledgeOnClose().

**Before/After**
```java
// Before
import java.util.LinkedList;
import java.util.Queue;
    private final Queue<AcknowledgeRequestState> acknowledgeRequestStates;
        this.acknowledgeRequestStates = new LinkedList<>();
        Iterator<AcknowledgeRequestState> iterator = acknowledgeRequestStates.iterator();
        while (iterator.hasNext()) {
            AcknowledgeRequestState acknowledgeRequestState = iterator.next();
            if (acknowledgeRequestState.isProcessed()) {
                iterator.remove();
            } else if (!acknowledgeRequestState.maybeExpire()) {
                if (nodesWithPendingRequests.contains(acknowledgeRequestState.nodeId)) {
                    log.trace("Skipping acknowledge request because previous request to {} has not been processed", acknowledgeRequestState.nodeId);

// After
import java.util.concurrent.atomic.AtomicBoolean;
    private final Map<Integer, Pair<AcknowledgeRequestState>> acknowledgeRequestStates;
        this.acknowledgeRequestStates = new HashMap<>();
        AtomicBoolean isAsyncDone = new AtomicBoolean();
        for (Map.Entry<Integer, Pair<AcknowledgeRequestState>> requestStates : acknowledgeRequestStates.entrySet()) {
            int nodeId = requestStates.getKey();

            if (!isNodeFree(nodeId)) {
                log.trace("Skipping acknowledge request because previous request to {} has not been processed, so acks are not sent", nodeId);
                isAsyncDone.set(false);
                // For commitAsync
                maybeBuildRequest(requestStates.getValue().getAsyncRequest(), currentTimeMs, true, isAsyncDone).ifPresent(unsentRequests::add);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17217: batched acknowledge requests per node in shareconsumerequestmanager (#16727).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
