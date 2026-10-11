**Commit**: 675a0889de · **Ticket**: KAFKA-18764
**Subsystem**: core/src/main/scala/kafka/server/KafkaApis.scala
**Category**: Logic error in condition

**Root cause**: The handlers for `ReadShareGroupState`, `WriteShareGroupState`, `DeleteShareGroupState`, and `ReadShareGroupStateSummary` called `authHelper.authorizeClusterOperation(request, CLUSTER_ACTION)` but did not check its return value. When authorisation failed, the method returned `false` but execution continued into the coordinator dispatch code, where it would either NPE (if the coordinator was absent) or attempt to process an unauthorised request. Additionally, no throttle-aware error response was sent to the client on auth failure, so the client received no reply. The fix checks the return value, sends a properly-throttled per-partition error response (using new `toGlobalErrorResponse` helpers), and returns early.

**Before/After**

```scala
// Before – return value ignored, no early return
authHelper.authorizeClusterOperation(request, CLUSTER_ACTION)
shareCoordinator match { ... }

// After – checked, early return with throttled response
if (!authorizeClusterOperation(request, CLUSTER_ACTION)) {
    requestHelper.sendMaybeThrottle(request, new ReadShareGroupStateResponse(
        ReadShareGroupStateResponse.toGlobalErrorResponse(
            readShareGroupStateRequest.data(), Errors.CLUSTER_AUTHORIZATION_FAILED)))
    return CompletableFuture.completedFuture[Unit](())
}
```

**How to spot**: Any `authorize*` helper that returns a boolean must have its return value checked; discarding it silently bypasses the authorisation gate. Also verify that the error response path applies throttling so that auth-failure clients are rate-limited like other error cases.

**Test**: No dedicated test added in this commit; the change is covered by the share-group authorisation integration test suite.
