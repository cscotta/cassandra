**Commit**: 743e185c8b · **Ticket**: KAFKA-17450
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Currently there are 4 handler functions present for handling ShareAcknowledge responses. ShareConsumeRequestManager had an interface and the respective handlers would implement it. Instead of having 4 different handlers for this, now using AcknowledgeRequestType, we could merge the code and have only 2 handler functions, one for ShareAcknowledge success and one for ShareAcknowledge failure, eliminating the need for the interface. This PR also fixes a bug - We were not using the time at which the response was received while handling the ShareAcknowledge response, we were using an outdated time.

**Before/After**
```java
// Before
            UnsentRequest request = acknowledgeRequestState.buildRequest(currentTimeMs);
                            this::handleShareAcknowledgeSuccess,
                            this::handleShareAcknowledgeFailure,
                                    this::handleShareAcknowledgeSuccess,
                                    this::handleShareAcknowledgeFailure,
                            this::handleShareAcknowledgeCloseSuccess,
                            this::handleShareAcknowledgeCloseFailure,
                                               long currentTimeMs) {
            final ShareAcknowledgeResponse response = (ShareAcknowledgeResponse) resp.responseBody();
            final ShareSessionHandler handler = acknowledgeRequestState.sessionHandler();
            final short requestVersion = resp.requestHeader().apiVersion();
            if (!handler.handleResponse(response, requestVersion)) {

// After
            UnsentRequest request = acknowledgeRequestState.buildRequest();
                                               long responseCompletionTimeMs) {
            ShareAcknowledgeResponse response = (ShareAcknowledgeResponse) resp.responseBody();

            if (acknowledgeRequestState.onClose()) {
                response.data().responses().forEach(topic -> topic.partitions().forEach(partition -> {
                    TopicIdPartition tip = new TopicIdPartition(topic.topicId(),
                            partition.partitionIndex(),
                            metadata.topicNames().get(topic.topicId()));
                    if (partition.errorCode() != Errors.NONE.code()) {
                        metricsManager.recordFailedAcknowledgements(acknowledgeRequestState.getInFlightAcknowledgementsCount(tip));
                    }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17450 reduced the handlers for handling shareacknowledgeresponse (#17061).

**Test**
No test added.
