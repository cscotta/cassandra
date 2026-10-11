# Bug 1069: Incorrect Mmap Segment Boundary Tracking Corrupts File Access

**Commit**: c37562e345 · **Ticket**: CASSANDRA-10357
**Subsystem**: src/java/org/apache/cassandra/io/util/MmappedSegmentedFile.java
**Category**: Off-by-one

---

## Root Cause

`MmappedSegmentedFile.Builder` tracked segment boundaries using a mutable `List<Long>` with a "current segment size" accumulator. The boundary-selection algorithm had two related defects: (1) it could emit a boundary that left the following segment larger than the 2 GB mmap limit, because it advanced `currentStart` by `currentSize` before accounting for the actual boundary position; (2) when serializing and deserializing boundary lists, the length of the file was never appended as a final sentinel, so the last segment's end was unknown. These errors meant that mmapped segments could span more than 2 GB, causing `MappedByteBuffer` operations to fail or silently access wrong data. The fix rewrites the boundary-tracking structure (`Boundaries`) with cleaner fixed/unfixed candidate logic, appends a magic number to the summary on write so that correctly-built summaries are not needlessly re-validated on startup, and adds an on-the-fly repair path for existing SSTables whose summaries were produced by the old code.

### Before / After

```java
// BEFORE — boundary algorithm could allow a segment to exceed MAX_SEGMENT_SIZE
public void addPotentialBoundary(long boundary)
{
    if (boundary - currentStart <= MAX_SEGMENT_SIZE)
    {
        currentSize = boundary - currentStart;
        return;
    }
    if (currentSize > 0)
    {
        currentStart += currentSize;
        boundaries.add(currentStart);
    }
    currentSize = boundary - currentStart;
    if (currentSize > MAX_SEGMENT_SIZE)
    {
        currentStart = boundary;
        boundaries.add(currentStart);
        currentSize = 0;
    }
}
```

```java
// AFTER — Boundaries class maintains a fixed/unfixed candidate model guaranteeing segment sizes
public void addCandidate(long candidate)
{
    boundaries = ensureCapacity(boundaries, fixedCount);
    fixedCount = addCandidate(boundaries, fixedCount, candidate);
}

private static int addCandidate(long[] boundaries, int fixedCount, long candidate)
{
    long delta = candidate - boundaries[fixedCount - 1];
    if (delta != 0)
    {
        if (delta <= MAX_SEGMENT_SIZE)
            boundaries[fixedCount] = candidate;          // overwrite unfixed candidate
        else if (boundaries[fixedCount] == 0)
            boundaries[fixedCount++] = candidate;        // first candidate in oversized span → fix it
        else
            boundaries[++fixedCount] = candidate;        // prior candidate fixed, start new span
    }
    return fixedCount;
}
```

---

## How to Spot

In any segment-building algorithm that accumulates "potential" boundaries, look for a place where both the current-segment size and the current-start are updated together. If the update to `currentStart` lags behind the true boundary by even one step, the invariant "no segment exceeds MAX_SEGMENT_SIZE" can be violated. The safest design separates "candidate" positions (which may be overwritten) from "fixed" positions (which cannot), and derives all segment sizes from the fixed array at finalization time rather than maintaining a running accumulator.

---

## Test

`LongSegmentedFileBoundaryTest` added: generates large synthetic SSTable files with boundaries that cross the 2 GB mmap segment limit and verifies that the resulting `MmappedSegmentedFile` segments are all within the allowed size.
