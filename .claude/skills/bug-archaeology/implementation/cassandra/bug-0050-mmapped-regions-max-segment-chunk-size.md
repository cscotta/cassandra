# Bug 0050: MmappedRegions Infinite Loop when MAX_SEGMENT_SIZE < chunkSize

**Commit**: 53ccfd58f2 · **Ticket**: CASSANDRA-20636
**Subsystem**: src/java/org/apache/cassandra/io/util/MmappedRegions.java
**Category**: Off-by-one

---

## Root Cause

`MmappedRegions.updateState` computed the maximum mapping size as `(MAX_SEGMENT_SIZE / chunkSize) * chunkSize`. When `MAX_SEGMENT_SIZE` is smaller than `chunkSize` (e.g., set to a value less than `PageAware.PAGE_SIZE` in tests or unusual configurations), integer division yields 0, making `maxSize = 0`. The subsequent `while (pos < length)` loop then advanced `pos` by 0 each iteration, causing an infinite loop or triggering an assertion failure, and no memory regions were ever mapped.

### Before / After

```java
// BEFORE
long maxSize = (long) (MAX_SEGMENT_SIZE / chunkSize) * chunkSize;
```

```java
// AFTER
long maxSize = Math.max(chunkSize, (long) (MAX_SEGMENT_SIZE / chunkSize) * chunkSize);
```

---

## How to Spot

Whenever a "round down to multiple of N" expression of the form `(value / N) * N` is used as a loop stride or capacity, check the boundary case where `value < N`. The result is zero, which causes infinite loops or zero-sized allocations. During review, look for such expressions and confirm that a `Math.max(N, ...)` guard or equivalent is present.

---

## Test

`SSTableReaderTest.testSpannedIndexPositionsWithMaxSegmentSizeSmallerThanPageSize` sets `MAX_SEGMENT_SIZE = PAGE_SIZE - 1` and verifies that SSTable index positions are computed correctly, directly exercising the previously infinite-looping code path.
