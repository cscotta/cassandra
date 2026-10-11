**Commit**: f9e30289d9 · **Ticket**: KAFKA-17403
**Subsystem**: clients/consumer/internals/HeartbeatRequestManager
**Category**: Logic error in condition

**Root cause**
`HeartbeatRequestManager.pollOnClose()` sent a leave-group heartbeat only if the consumer was not yet in the LEAVING state at the time `close()` was called. If the consumer had already transitioned to LEAVING (e.g., because a prior `poll()` started the leave), `pollOnClose()` returned an empty list and the leave heartbeat was never actually delivered. The fix adds a `pollOnClose(long currentTimeMs)` override that checks for the LEAVING state explicitly and generates the heartbeat regardless of how the transition was initiated.

**Before/After**
```java
// Before: leave HB skipped if state was already LEAVING
NetworkClientDelegate.PollResult pollOnClose() {
    if (state != MemberState.LEAVING) return PollResult.EMPTY;
    return pollHeartbeat(time.milliseconds());
}

// After: dedicated override that always polls when LEAVING
NetworkClientDelegate.PollResult pollOnClose(long currentTimeMs) {
    if (state == MemberState.LEAVING || state == MemberState.PREPARE_LEAVING)
        return pollHeartbeat(currentTimeMs);
    return PollResult.EMPTY;
}
```

**How to spot**
Methods that guard on "not yet in state X" before triggering a transition to X will silently no-op if the transition was already started by another code path. Methods that need to act *while in* state X should instead guard on "currently in state X".

**Test**
`HeartbeatRequestManagerTest` updated to verify leave-group heartbeat is sent when `pollOnClose` is called with LEAVING state already active.
