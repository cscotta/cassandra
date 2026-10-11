**Commit**: 9e5503b04f · **Ticket**: KAFKA-18745
**Subsystem**: server-common / share / PersisterStateManager
**Category**: Missing null / bounds check

**Root cause**
`PersisterStateManager.RequestHandler.onComplete()` previously only processed a `ClientResponse` when `response != null && response.hasResponse()` was true. A disconnection or timeout produces a non-null response object whose `hasResponse()` returns false, so the callback was silently dropped and the `CompletableFuture` that backs the RPC was never completed. Additionally, the concrete handler classes each duplicated their own `writeStateErrorResponse`/`readStateErrorResponse` private methods instead of sharing a common hook, so the base class could not invoke the right error path.

**Before/After**
```java
// Before
if (response != null && response.hasResponse()) {
    if (isFindCoordinatorResponse(response)) {
        handleFindCoordinatorResponse(response);
    } else if (isResponseForRequest(response)) {
        handleRequestResponse(response);
    }
}

// After
if (response == null) {
    requestErrorResponse(Errors.UNKNOWN_SERVER_ERROR, new NetworkException("..."));
    sender.wakeup();
    return;
}
if (isFindCoordinatorResponse(response)) {
    Optional<Errors> err = checkNetworkError(response, this::findCoordinatorErrorResponse);
    if (err.isEmpty()) { handleFindCoordinatorResponse(response); }
} else if (isResponseForRequest(response)) {
    Optional<Errors> err = checkNetworkError(response, this::requestErrorResponse);
    if (err.isEmpty()) { handleRequestResponse(response); }
}
```

**How to spot**
Look for `onComplete` or callback methods that take a `ClientResponse` but only enter their main branch when `hasResponse()` is true. A disconnection or timeout yields `wasDisconnected()==true` / `wasTimedOut()==true` and `hasResponse()==false`; callers waiting on a future will hang forever.

**Test**
`PersisterStateManagerTest` — verifies that `checkNetworkError` correctly classifies disconnected, timed-out, and unknown responses, and that each concrete handler completes its future with the appropriate error code rather than leaving it pending.
