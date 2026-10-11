# Bug 2052: RangeTombstone Tracker Incorrectly Applied During SSTable Streaming at Column Index Boundary

**Commit**: 0f1fb4340c · **Ticket**: CASSANDRA-5418
**Subsystem**: src/java/org/apache/cassandra/db/ColumnIndex.java, src/java/org/apache/cassandra/io/sstable/SSTableWriter.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When streaming SSTable data, `SSTableWriter` uses `ColumnIndex.Builder` to re-index already-serialized atoms that include pre-written `RangeTombstone` markers. The `Builder` always created a `RangeTombstone.Tracker`, which would attempt to write additional range tombstone open-markers at each column index block boundary. Because the tombstone data had already been serialized verbatim from the stream, these extra marker writes produced corrupt, double-encoded tombstone entries in the resulting SSTable. The fix introduces a `fromStream` flag that, when `true`, leaves `tombstoneTracker` null and skips all tracker-driven writes.

### Before / After

```java
// BEFORE: tracker always created, writes duplicate markers during streaming
this.tombstoneTracker = new RangeTombstone.Tracker(cf.getComparator());
```

```java
// AFTER: tracker is null when building index from an existing stream
this.tombstoneTracker = fromStream ? null : new RangeTombstone.Tracker(cf.getComparator());
// All tracker usages guarded:
if (tombstoneTracker != null)
    endPosition += tombstoneTracker.writeOpenedMarker(firstColumn, output, atomSerializer);
```

---

## How to Spot

Whenever a builder or serializer class is reused in both write-from-scratch and copy-from-stream contexts, check whether any side-effecting logic (e.g., writing additional markers or headers) is guarded by the appropriate mode flag. Column index boundary logic that writes data is a particularly risky site.

---

## Test

A new `testTransferRangeTombstones` test was added to `StreamingTransferTest` that creates a row with large columns and `RangeTombstone` entries spanning an index block boundary, streams the SSTable, then verifies the transferred data is intact.
