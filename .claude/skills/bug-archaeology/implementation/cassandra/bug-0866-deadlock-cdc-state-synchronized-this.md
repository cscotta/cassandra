# Bug 0866: Deadlock in CDC State Tracking via Synchronized-on-this

**Commit**: 90afc58d3d · **Ticket**: CASSANDRA-12198
**Subsystem**: Commit Log / CDC
**Category**: Deadlock

---

## Root Cause
`CommitLogSegment.setCDCState` and `CommitLogSegmentManagerCDC`'s `processNewSegment`/`processDiscardedSegment` all synchronized on the `CommitLogSegment` object instance itself (`synchronized(this)` / `synchronized(segment)`). Because `CommitLogSegment` is also used as a monitor for other purposes in the code base, acquiring the intrinsic lock of the segment object in two different lock-acquisition orders created a potential deadlock. The fix introduces a dedicated `cdcStateLock` object on `CommitLogSegment` so that CDC state transitions are guarded by their own lock, eliminating contention with any other code that synchronizes on the segment itself.

### Before / After
```java
// BEFORE — in CommitLogSegment.setCDCState
synchronized(this)
{
    if (cdcState == CDCState.CONTAINS && newState != CDCState.CONTAINS)
        throw new IllegalArgumentException(...);
    ...
}

// BEFORE — in CommitLogSegmentManagerCDC
synchronized(segment)          // acquires segment's intrinsic lock
{
    segment.setCDCState(...);  // re-acquires same lock → ok, but deadlock with other holders
}

// AFTER — dedicated lock object
Object cdcStateLock = new Object();

synchronized(cdcStateLock)     // both sides use the same dedicated lock
```

---

## How to Spot
Synchronizing on `this` (or on a passed-in object) is dangerous when that same object is used as a monitor elsewhere. Review any method that calls `synchronized(someObjectPassedIn)` and check whether the passed-in object's class also has `synchronized` methods or blocks that could be entered by another thread holding a different lock, creating a lock-ordering inversion. The safest fix is always a private, purpose-specific lock object.

---

## Test
No test added.
