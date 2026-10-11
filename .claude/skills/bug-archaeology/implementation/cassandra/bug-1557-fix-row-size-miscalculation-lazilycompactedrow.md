# Bug 1557: Fix Row Size Miscalculation in LazilyCompactedRow

**Commit**: 192596ad75 · **Ticket**: CASSANDRA-7543
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LazilyCompactedRow.java, src/java/org/apache/cassandra/db/ColumnIndex.java
**Category**: Off-by-one

---

## Root Cause

`LazilyCompactedRow.write()` calculated the serialized row size as `clockSize + columnSerializedSize`, but omitted the bytes written for reopened range tombstone markers. When a `RangeTombstone` spans multiple index blocks, `ColumnIndex.Builder` writes a tombstone marker at the start of each new block via `tombstoneTracker.writeOpenedMarker()`. Those marker bytes were added to the output stream but not accounted for in the size calculation, causing the reported row size to be smaller than the actual written bytes. The fix tracks the cumulative size of all opened-marker writes in `ColumnIndex.Builder` and adds it to the final size computation.

### Before / After

```java
// BEFORE (LazilyCompactedRow.java)
long dataSize = clockOut.getLength() + columnSerializedSize;
```

```java
// AFTER (LazilyCompactedRow.java)
long dataSize = clockOut.getLength() + columnSerializedSize + this.indexBuilder.getOpenedMarkerSize();

// New tracking in ColumnIndex.Builder
private long openedMarkerSize = 0;
// ...
long tombstoneSize = tombstoneTracker.writeOpenedMarker(firstColumn, output, atomSerializer);
endPosition += tombstoneSize;
openedMarkerSize += tombstoneSize;
```

---

## How to Spot

Any time bytes are written to an output stream in a loop (e.g., for repeated tombstone markers across index blocks), verify that every write path's byte count is also reflected in the final size accumulator. Size fields that are populated from a partial subset of the write calls will silently undercount.

---

## Test

`testOneRowWithRangeTombstone` in `LazilyCompactedRowTest`: writes a row with a range tombstone spanning multiple columns, compacts it, and asserts that the size returned by `row.write()` matches the actual bytes written to the output buffer.
