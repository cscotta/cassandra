**Commit**: 266532f562 · **Ticket**: KAFKA-18736
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/StreamsGroupHeartbeatRequestManager.java
**Category**: Missing null / bounds check

**Root cause**: `StreamsGroupHeartbeatRequestManager` completed heartbeat requests via a callback that checked `response != null` to call `onResponse(...)` but had no `else` branch for the error case (`response == null, exception != null`). As a result, send-level failures (network errors, unsupported version) were silently swallowed: the heartbeat state was never reset, no back-off was applied, and the membership manager was never notified. Similarly, `onResponse` handled the success path (`NONE`) but had no `else` for non-`NONE` error codes, so server-side errors were ignored entirely. The fix adds `onFailure()` for the send-error path and `onErrorResponse()` for server error codes, with explicit handling per error type.

**Before/After**

```java
// Before – no else branch for failures
if (response != null) {
    onResponse((StreamsGroupHeartbeatResponse) response.responseBody(), completionTimeMs);
}
// error silently dropped

// After
if (response != null) {
    onResponse(...);
} else {
    onFailure(exception, completionTimeMs);
}

// Before – only success handled
if (Errors.forCode(response.data().errorCode()) == Errors.NONE) {
    onSuccessResponse(response, currentTimeMs);
}
// non-NONE errors silently dropped

// After
} else {
    onErrorResponse(response, currentTimeMs);
}
```

**How to spot**: Any async request callback that handles only the success branch leaves the client in an undefined state on failure. Always pair a success handler with an error handler that resets relevant state and applies back-off.

**Test**: No new test in this commit; error handling is verified via the existing heartbeat manager test suite which is extended separately.
