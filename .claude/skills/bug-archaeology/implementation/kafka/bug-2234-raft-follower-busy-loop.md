**Commit**: 34ebccc79f · **Ticket**: KAFKA-17655
**Subsystem**: raft (KafkaRaftClient)
**Category**: Logic error in condition

**Root cause**
In `pollFollowerAsVoter`, when `hasUpdateVoterPeriodExpired()` returned `true` but no voter update was actually needed, `resetUpdateVoterPeriod()` was not called. The period expiry flag therefore remained set, causing the same check to fire on every subsequent poll iteration in a tight busy-loop without performing any useful work and without resetting the timer.

**Before/After**
```java
// Before – timer not reset when no update needed
if (hasUpdateVoterPeriodExpired() && needsVoterUpdate()) {
    sendUpdateVoter();
    resetUpdateVoterPeriod();
}
// if expired but !needsVoterUpdate: timer stays fired → busy-loop

// After – always reset, fall through to fetch
if (hasUpdateVoterPeriodExpired()) {
    resetUpdateVoterPeriod();
    if (needsVoterUpdate()) sendUpdateVoter();
}
maybeSendFetchOrFetchSnapshot();
```

**How to spot**
Whenever a period/expiry flag controls a conditional action, the flag must be reset regardless of whether the action was taken; "reset only on action" creates a busy-loop when the condition is true but the action predicate is false.

**Test**
Unit tests added in `KafkaRaftClientTest` for the no-update-needed path.
