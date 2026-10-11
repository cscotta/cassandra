**Commit**: c284a92472 · **Ticket**: KAFKA-19990
**Subsystem**: transaction-coordinator/src/main/java/org/apache/kafka/coordinator/transaction/RPCProducerIdManager.java
**Category**: Missing null / bounds check

**Root cause**
The `onComplete` handler in `RPCProducerIdManager` only processed the response if the body was an `AllocateProducerIdsResponse`, silently ignoring authentication exceptions and version-mismatch exceptions carried in the `ClientResponse`. When such errors occurred, `requestInFlight` was never reset, and `backoffDeadlineMs` was not set, so the manager became permanently stuck — unable to issue new requests and not retrying after a backoff.

**Before/After**
```java
// Before – silently drops auth/version errors
public void onComplete(ClientResponse response) {
    if (response.responseBody() instanceof AllocateProducerIdsResponse) {
        handleAllocateProducerIdsResponse((AllocateProducerIdsResponse) response.responseBody());
    }
}

// After – explicit checks before casting; handleUnsuccessfulResponse() on any error
protected void handleAllocateProducerIdsResponse(ClientResponse clientResponse) {
    if (clientResponse.authenticationException() != null) { handleUnsuccessfulResponse(); return; }
    if (clientResponse.versionMismatch() != null)         { handleUnsuccessfulResponse(); return; }
    if (!clientResponse.hasResponse())                    { handleUnsuccessfulResponse(); return; }
    AllocateProducerIdsResponse response = (AllocateProducerIdsResponse) clientResponse.responseBody();
    ...
}
```

**How to spot**
Any `ClientResponse` handler that casts to a specific response type without first checking `authenticationException()`, `versionMismatch()`, and `hasResponse()` may silently swallow errors, leaving the component in a broken state.

**Test**
`ProducerIdManagerTest#testRetryBackoffOnAuthException`, `testRetryBackoffOnVersionMismatch`, `testRetryBackoffOnNoResponse`: each injects the corresponding error, asserts that `CoordinatorLoadInProgressException` is thrown during backoff, advances time, then verifies that a subsequent call succeeds.
