**Commit**: 0c116c9629 · **Ticket**: KAFKA-19335
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
There is a sequence of interactions with the membership managers of
KIP-848, KIP-932, KIP-1071 that can put the membership manager into
JOINING state, but where member epoch is set to -1. This can result in
an invalid request being sent, since joining heartbeats should not have
member epoch -1. This may lead to the member failing to join. In the
case of streams, the group coordinator will return INVALID_REQUEST.

**Before/After**
```java
// Before
        if (state == MemberState.UNSUBSCRIBED && maybeCompleteLeaveInProgress()) {
        if (state == MemberState.UNSUBSCRIBED && maybeCompleteLeaveInProgress()) {

// After
        if (state == MemberState.UNSUBSCRIBED && responseData.memberEpoch() < 0 && maybeCompleteLeaveInProgress()) {
        if (responseData.memberEpoch() < 0) {
            log.debug("Ignoring heartbeat response received from broker. Member {} with epoch {} " +
                    "is in {} state and the member epoch is invalid: {}. ", memberId, memberEpoch, state,
                    responseData.memberEpoch());
            maybeCompleteLeaveInProgress();
            return;
        }
        if (state == MemberState.UNSUBSCRIBED && responseData.memberEpoch() < 0 && maybeCompleteLeaveInProgress()) {
        if (responseData.memberEpoch() < 0) {
            log.debug("Ignoring heartbeat response received from broker. Member {} with epoch {} " +
                    "is in {} state and the member epoch is invalid: {}. ", memberId, memberEpoch, state,
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19335: membership managers send negative epoch in joining (#19818).

**Test**
Test modified in ConsumerMembershipManagerTest.java covering the corrected behaviour.
