**Commit**: 8fde6dedea · **Ticket**: KAFKA-18155
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
In the response handler for ShareAcknowledge, we are passing the clientResponse.receivedTimeMs() to the handler methods. But when there is a disconnect or when the response received is null, we should be passing the current time instead. This bug was causing consumer to hang as it did not call the handler methods on disconnect, and further requests were blocked waiting for its completion. Reviewers: Andrew Schofield <aschofield@confluent.io>,  Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
            BiConsumer<ClientResponse, Throwable> responseHandler = (clientResponse, error) -> {
                if (error != null) {
                    handleShareAcknowledgeFailure(nodeToSend, requestBuilder.data(), this, error, clientResponse.receivedTimeMs());
                } else {
                    handleShareAcknowledgeSuccess(nodeToSend, requestBuilder.data(), this, clientResponse, clientResponse.receivedTimeMs());
                }
            };

                return new UnsentRequest(requestBuilder, Optional.of(nodeToSend)).whenComplete(responseHandler);

// After

                UnsentRequest unsentRequest = new UnsentRequest(requestBuilder, Optional.of(nodeToSend));
                BiConsumer<ClientResponse, Throwable> responseHandler = (clientResponse, error) -> {
                    if (error != null) {
                        handleShareAcknowledgeFailure(nodeToSend, requestBuilder.data(), this, error, unsentRequest.handler().completionTimeMs());
                    } else {
                        handleShareAcknowledgeSuccess(nodeToSend, requestBuilder.data(), this, clientResponse, unsentRequest.handler().completionTimeMs());
                    }
                };
                return unsentRequest.whenComplete(responseHandler);
    @Test
    public void testServerDisconnectedOnShareAcknowledge() {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18155 : fix bug in response handler for shareacknowledge (#18029).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
