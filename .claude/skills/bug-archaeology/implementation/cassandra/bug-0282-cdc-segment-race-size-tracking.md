# Bug 0282: Race Condition Between CDC Size Tracking and Segment Advance

**Commit**: 84e10d0cad · **Ticket**: CASSANDRA-17792
**Subsystem**: db/commitlog (CommitLogSegmentManagerCDC, CommitLogSegment)
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommitLogSegmentManagerCDC.createSegment()` unconditionally created a hard link in the CDC directory for every new segment, even before checking whether the segment should be permitted. This created a window where a segment flagged `FORBIDDEN` by `CDCSizeTracker.processNewSegment()` still had a hard link, inflating the tracked size and causing subsequent CDC writes to be rejected permanently. Additionally, when the overflow recalculation background task ran, it called `processNewSegment(allocatingFrom())` from within the executor rather than the allocator thread, without holding the necessary locks, producing a race between the size update and the segment state assignment.

### Before / After

```java
// BEFORE — createSegment()
CommitLogSegment segment = CommitLogSegment.createSegment(commitLog, this);
// Hard link created unconditionally before state is known
FileUtils.createHardLink(segment.logFile, segment.getCDCFile());
cdcSizeTracker.processNewSegment(segment);
return segment;
```

```java
// AFTER — createSegment()
CommitLogSegment segment = CommitLogSegment.createSegment(commitLog, this);
cdcSizeTracker.processNewSegment(segment);
// Hard link only when the segment is actually permitted
if (segment.getCDCState() == CDCState.PERMITTED)
    FileUtils.createHardLink(segment.logFile, segment.getCDCFile());
return segment;
```

---

## How to Spot

When a resource (hard link, counter increment) is allocated before the condition that determines whether it is needed, there is a risk of double-allocation or over-counting. Review any pattern where an irreversible side effect precedes the guard that decides whether that side effect should occur. Overflow recalculation tasks that call back into the allocator's state without holding the allocator's lock are also a signal to scrutinise.

---

## Test

No dedicated unit test was added; the fix is covered implicitly by existing `CommitLogSegmentManagerCDC` tests verifying CDC space enforcement behaviour.
