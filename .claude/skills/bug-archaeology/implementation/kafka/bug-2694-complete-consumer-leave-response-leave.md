**Commit**: 931bb62a23 · **Ticket**: KAFKA-16984
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Improvement to ensure that the consumer unsubscribe operation waits for a response to the leave group request before moving on to close the consumer. This makes it consistent with the behaviour of the legacy consumer. This will avoid undesired interactions on close, that triggers a leave group, and shuts down the network thread when it completes (which before this PR could led to responses to disconnected clients). Note that this PR does not change the transitions of the state machine on leave group, only the completion of the leave group future.

**Before/After**
```java
// Before
        membershipManager.onHeartbeatRequestSent();
        membershipManager.onHeartbeatFailure();
    void onHeartbeatFailure();
     * Update state when a heartbeat is sent out. This will transition out of the states that end
    void onHeartbeatRequestSent();
    public void onHeartbeatFailure() {
        metricsManager.maybeRecordRebalanceFailed();

// After
        membershipManager.onHeartbeatRequestGenerated();
        membershipManager.onHeartbeatFailure(exception instanceof RetriableException);
        membershipManager.onHeartbeatFailure(false);
     *
     * @param retriable True if the request failed with a retriable error.
    void onHeartbeatFailure(boolean retriable);
     * Update state when a heartbeat is generated. This will transition out of the states that end
    void onHeartbeatRequestGenerated();
        if (state == MemberState.UNSUBSCRIBED && maybeCompleteLeaveInProgress()) {
            log.debug("Member {} with epoch {} received a successful response to the heartbeat " +
                "to leave the group and completed the leave operation. ", memberId, memberEpoch);
            return;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16984: complete consumer leave on response to leave request (#16569).

**Test**
Test modified in HeartbeatRequestManagerTest.java covering the corrected behaviour.
