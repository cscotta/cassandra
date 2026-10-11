**Commit**: dfa28c463a · **Ticket**: KAFKA-19953
**Subsystem**: core/src/main/java/kafka/server/share/SharePartition.java
**Category**: Logic error in condition

**Root cause**
In `SharePartition.releaseAcquiredRecords`, when the per-offset state map is iterated to release records belonging to a given `memberId`, a non-matching offset (owned by a different member) caused an early `return Optional.empty()`. This aborted the entire release loop for the batch, leaving all subsequent offsets owned by `memberId` unreleased — even though those offsets should have been freed. The fix changes `return Optional.empty()` to `continue`, so only the non-matching offset is skipped and the loop proceeds to the remaining offsets.

**Before/After**
```java
// Before: early return stops processing all remaining offsets in the batch
if (!offsetState.getValue().memberId().equals(memberId) && ...) {
    log.debug("Member {} is not the owner ... Skipping offset.", ...);
    return Optional.empty();  // ← exits the entire lambda/loop
}

// After: continue skips only the current offset
if (!offsetState.getValue().memberId().equals(memberId) && ...) {
    log.debug("Member {} is not the owner ... Skipping offset.", ...);
    continue;  // ← moves to the next offset in the batch
}
```

**How to spot**
In loops over per-offset state within a batch, any `return` statement in the middle of iteration should be scrutinised: it may be correct to abort the batch (e.g., on an error), but when the intent is to "skip this offset but keep processing others", `continue` is correct and `return` is wrong. The bug is especially subtle when the loop is embedded in a lambda, where `return` returns from the lambda (short-circuiting the outer `forEach`) rather than the enclosing method.

**Test**
`testReleaseAcquiredRecordsPerOffsetWithDifferentMemberId`: acquires offsets 5–14 with member-1, partially acknowledges them, then acquires 10–12 with member-2, then releases for member-1. Asserts that offsets 7–9 and 13–14 (owned by member-1) are released to `AVAILABLE`, while offsets 10–12 (owned by member-2) remain `ACQUIRED`.
