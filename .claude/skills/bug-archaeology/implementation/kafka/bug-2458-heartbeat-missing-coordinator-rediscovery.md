**Commit**: 6a2789cf70 · **Ticket**: KAFKA-17293
**Subsystem**: clients/consumer/internals/AbstractHeartbeatRequestManager
**Category**: Silent skip on non-match

**Root cause**
When a heartbeat request failed with a `RetriableException` (which includes `DisconnectException`), the `HeartbeatRequestManager` correctly called `membershipManager.onHeartbeatFailure(true)` and scheduled a retry, but it never notified `CoordinatorRequestManager` to re-discover the coordinator. This meant that a transient coordinator disconnect would leave the consumer retrying heartbeats to a dead coordinator indefinitely. The `CommitRequestManager` already had a private `handleCoordinatorDisconnect` helper, but the heartbeat path lacked it entirely.

**Before/After**
```java
// Before
membershipManager.onHeartbeatFailure(exception instanceof RetriableException);
if (exception instanceof RetriableException) {
    // missing: coordinatorRequestManager.handleCoordinatorDisconnect(...)
    log.debug("Will retry in {} ms", heartbeatRequestState.remainingBackoffMs(responseTimeMs));

// After
membershipManager.onHeartbeatFailure(exception instanceof RetriableException);
if (exception instanceof RetriableException) {
    coordinatorRequestManager.handleCoordinatorDisconnect(exception, responseTimeMs);
    log.debug("Will retry in {} ms", heartbeatRequestState.remainingBackoffMs(responseTimeMs));
```

**How to spot**
When reviewing failure handlers for request managers, verify that each retriable error path calls the coordinator manager's disconnect handler — not just the membership manager's failure hook. A `DisconnectException` with no subsequent coordinator re-discovery triggers an infinite retry cycle.

**Test**
`testDisconnect` added to `ConsumerHeartbeatRequestManagerTest`.
