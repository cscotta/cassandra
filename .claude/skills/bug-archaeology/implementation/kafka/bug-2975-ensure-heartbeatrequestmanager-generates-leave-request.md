**Commit**: ca9f4aeda7 · **Ticket**: KAFKA-16639
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Fix the bug where the heartbeat is not sent when a newly created consumer is immediately closed. When there is a heartbeat request in flight and the consumer is then closed. In the current code, the HeartbeatRequestManager does not correctly send the closing heartbeat because a previous heartbeat request is still in flight. However, the closing heartbeat is only sent once, so in this situation, the broker will not know that the consumer has left the consumer group until the consumer's heartbeat times out.

**Before/After**
```java
// Before
        boolean heartbeatNow = membershipManager.shouldHeartbeatNow() && !heartbeatRequestState.requestInFlight();


// After

        // Case 1: The member is leaving
        boolean heartbeatNow = membershipManager.state() == MemberState.LEAVING ||
                // Case 2: The member state indicates it should send a heartbeat without waiting for the interval, and there is no heartbeat request currently in-flight
                (membershipManager.shouldHeartbeatNow() && !heartbeatRequestState.requestInFlight());


    @ParameterizedTest
    @ApiKeyVersionsSource(apiKey = ApiKeys.CONSUMER_GROUP_HEARTBEAT)
    public void testSendingLeaveGroupHeartbeatWhenPreviousOneInFlight(final short version) {
        mockStableMember();
        time.sleep(DEFAULT_HEARTBEAT_INTERVAL_MS);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16639 ensure heartbeatrequestmanager generates leave request regardless of in-flight heartbeats. (#16017).

**Test**
Test modified in HeartbeatRequestManagerTest.java covering the corrected behaviour.
