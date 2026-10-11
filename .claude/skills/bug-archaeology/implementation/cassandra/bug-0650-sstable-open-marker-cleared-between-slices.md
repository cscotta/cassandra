# Bug 0650: SSTable Iterator Clears Open Marker Between Slices, Losing Range Tombstone

**Commit**: 975c3d81b6 · **Ticket**: CASSANDRA-13787
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/SSTableIterator.java
**Category**: State not cleaned up

---

## Root Cause

When an SSTable reader iterates multiple slices over a partition that contains a range tombstone spanning slice boundaries, it tracks the currently active range tombstone in `openMarker`. The old code called `getAndClearOpenMarker()`, which both read and nulled out `openMarker` while emitting the close bound for the current slice. Clearing `openMarker` meant the next slice iteration started with no knowledge of the ongoing range deletion, causing the tombstone to be silently dropped for subsequent slices. The fix retains `openMarker` (reads it without clearing) so the state survives into the next slice.

### Before / After

```java
// BEFORE – clears openMarker, losing it for the next slice
if (openMarker != null)
{
    next = new RangeTombstoneBoundMarker(end, getAndClearOpenMarker());
    return true;
}
sliceDone = true;
```

```java
// AFTER – retains openMarker for subsequent slices
sliceDone = true;
if (openMarker != null)
{
    next = new RangeTombstoneBoundMarker(end, openMarker);
    return true;
}
```

---

## How to Spot

Any iterator that drives a state machine with a "currently open" marker (`openMarker`, `openBound`, etc.) and resets that state as a side-effect of reading it will silently drop information when the consumer re-enters after a boundary. Review every `getAndClear*` style method and confirm the cleared state is truly finished rather than spanning the next iteration segment.

---

## Test

`SinglePartitionSliceCommandTest.testMultiSlicesCommandWithFlush/WithoutFlush` verifies that range tombstone open/close marker pairs are correctly emitted for every sub-slice when a multi-slice read command covers a partition with an existing range deletion.
