# Bug 0774: 2.x Deleted Row Marker Deserialized as Liveness Info

**Commit**: c612cd8d7d · **Ticket**: CASSANDRA-12620
**Subsystem**: Legacy serialization / SSTable read
**Category**: Wrong serialization / deserialization

---

## Root Cause
In Cassandra 2.1, when a row's TTL expired, compaction could convert the row-marker cell into a deleted (tombstone) cell rather than simply removing it. During deserialization of 2.x SSTables in 3.x, the code that handled the zero-length "row marker" cell unconditionally called `addPrimaryKeyLivenessInfo(...)` — treating a tombstone row marker as if it were a live row marker. This caused the partition row to appear alive when it should have been treated as deleted, potentially resurecting data that had been intentionally expired or deleted.

The fix adds a `cell.isTombstone()` guard: liveness info is only set when the row-marker cell is not a tombstone.

### Before / After
```java
// BEFORE: tombstone row marker wrongly promotes row to live
if (cell.name.isEmpty())  // row marker cell
{
    assert !cell.value.hasRemaining();
    builder.addPrimaryKeyLivenessInfo(
        LivenessInfo.create(cell.timestamp, cell.ttl, cell.localDeletionTime));
}

// AFTER: skip if the cell is actually a tombstone (expired TTL compacted away)
if (cell.name.isEmpty())
{
    assert !cell.value.hasRemaining();
    if (!cell.isTombstone())
        builder.addPrimaryKeyLivenessInfo(
            LivenessInfo.create(cell.timestamp, cell.ttl, cell.localDeletionTime));
}
```

---

## How to Spot
When deserializing legacy formats, never assume that a structural element (here, an empty-named cell acting as a row marker) carries the same semantics as its 3.x equivalent. Expired-TTL cells could have been compacted into tombstones in older versions; any code that maps legacy cells to 3.x liveness objects must guard on the cell's deletion state before treating it as live.

---

## Test
No test added.
