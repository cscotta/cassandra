**Commit**: 5dd0153864 · **Ticket**: KAFKA-19927
**Subsystem**: server-common/src/main/java/org/apache/kafka/server/share/persister/PersisterStateManager.java
**Category**: Logic error in condition

**Root cause**
`PersisterStateManager` RPC handlers received `ClientResponse` objects and checked for error conditions via a `checkResponseError(response, errorConsumer)` helper that accepted a `BiConsumer` callback for error handling. When the response indicated `wasDisconnected()` or `wasTimedOut()` (both retriable network-level conditions), the handler invoked the error consumer and then continued to parse the response body, which could itself fail. The fix removes the `BiConsumer` pattern: retriable network errors now fall through to a retry path while fatal errors are handled inline, preventing unnecessary body parsing after a transport-level failure.

**Before/After**
```java
// BEFORE – error consumer called, but parsing still continued
Optional<Errors> err = checkResponseError(response, this::findCoordinatorErrorResponse);
if (err.isEmpty()) {
    handleFindCoordinatorResponse(response);
}

// AFTER – simplified; body parsed only when no transport error
Errors clientResponseError = checkResponseError(response).orElse(Errors.NONE);
switch (clientResponseError) {
    case NONE:
        // parse body here
    case NETWORK_EXCEPTION:
    case REQUEST_TIMED_OUT:
        // retry
}
```

**How to spot**
Handler methods that call an error-consumer callback and then fall through to body parsing are risky: the callback side effect does not prevent the subsequent parse. Look for patterns where error handling writes to a future or enqueues a retry but does not `return` early, allowing the caller to continue with an invalid response.

**Test**
Updated and new tests in `PersisterStateManagerTest` covering `NETWORK_EXCEPTION` and `REQUEST_TIMED_OUT` response error codes to verify that retries are scheduled rather than propagated as fatal errors.
