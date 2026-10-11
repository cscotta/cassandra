**Commit**: 55764f837b · **Ticket**: KAFKA-19859
**Subsystem**: core/src/main/java/kafka/server/share/SharePartition.java
**Category**: Logic error in condition

**Root cause**
In `SharePartition.maybeUpdateCachedStateAndOffsets`, when advancing `startOffset` after acknowledgements, two code paths unconditionally assigned `startOffset` from either `cachedState.higherKey(lastOffsetAcknowledged)` or `lastOffsetAcknowledged + 1`. If LSO movement had already advanced `startOffset` to a higher value (e.g., past records that were ARCHIVED by the acquisition-lock timer), these assignments could move `startOffset` backward, re-exposing records that should already be past the window and producing incorrect ARCHIVED/AVAILABLE state after lock expiry.

**Before/After**
```java
// Before – unconditional assignment; can move startOffset backward
startOffset = cachedState.higherKey(lastOffsetAcknowledged);

// After – only advance; never retreat
long lastOffsetAcknowledgedHigherKey = cachedState.higherKey(lastOffsetAcknowledged);
if (lastOffsetAcknowledgedHigherKey > startOffset) {
    startOffset = lastOffsetAcknowledgedHigherKey;
}
```

**How to spot**
Any direct assignment to a monotonically-advancing offset variable inside a callback that may fire out of order (e.g., after LSO movement has already advanced it) must use `max(current, new)` rather than plain assignment.

**Test**
`SharePartitionTest#testLsoMovementWithPendingAcknowledgements` and `testLsoMovementWithPendingAcknowledgementsForBatches`: move LSO past some in-flight offsets, expire acquisition-lock timers, then verify that offset states reflect ARCHIVED (not incorrectly AVAILABLE) for offsets before the final startOffset.
