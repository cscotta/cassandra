# Bug 1049: PartitionUpdate.operationCount() Ignores Static Row

**Commit**: 3615da5831 · **Ticket**: CASSANDRA-10606
**Subsystem**: Storage Engine / PartitionUpdate
**Category**: Logic error in condition

---

## Root Cause
`PartitionUpdate.operationCount()` was designed to count the number of discrete mutation operations in an update (used for rate-limiting and accounting). It summed `rowCount()`, the number of range tombstones, and a flag for the partition-level deletion, but it never counted the static row. A `PartitionUpdate` that contained only (or also) a static column write would therefore undercount its operations by 1, causing callers that relied on this count (e.g. mutation size accounting) to under-report writes.

The fix adds a single conditional term: `+ (staticRow().isEmpty() ? 0 : 1)`, mirroring the existing idiom used for the partition deletion check.

### Before / After
```java
// BEFORE – static row not included in operation count
public int operationCount()
{
    return rowCount()
         + deletionInfo.rangeCount()
         + (deletionInfo.getPartitionDeletion().isLive() ? 0 : 1);
}

// AFTER – static row counted as one operation when present
public int operationCount()
{
    return rowCount()
         + (staticRow().isEmpty() ? 0 : 1)
         + deletionInfo.rangeCount()
         + (deletionInfo.getPartitionDeletion().isLive() ? 0 : 1);
}
```

---

## How to Spot
Whenever a method counts "operations" or "cells" in a partition update, check explicitly that each of the four distinct parts of a `PartitionUpdate` — regular rows, static row, range tombstones, and partition deletion — are accounted for. The static row is a structural parallel to the partition-deletion flag and is easy to overlook because `rowCount()` returns only non-static rows.

---

## Test
A new `PartitionUpdateTest` was added that creates a `PartitionUpdate` containing a static column and asserts that `operationCount()` returns 1 for that case.
