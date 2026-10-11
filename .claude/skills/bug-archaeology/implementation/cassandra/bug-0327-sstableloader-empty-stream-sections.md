# Bug 0327: SSTableLoader Streams Empty Sections, Causing Server-Side Error

**Commit**: 193d4ef3a7 · **Ticket**: CASSANDRA-16349
**Subsystem**: io/sstable/SSTableLoader
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableLoader` iterates over the token-range-to-endpoint mapping to build outgoing streams. For each `(endpoint, tokenRanges)` pair it called `sstable.getPositionsForRanges(tokenRanges)` to get the byte-range sections of the SSTable owned by that endpoint, then immediately proceeded to create and enqueue an outgoing stream — without first checking whether `sstableSections` was empty. When an endpoint owned none of the tokens covered by a given SSTable, the sections list was empty, and the resulting zero-byte stream triggered an error on the receiving server.

### Before / After

```java
// BEFORE
List<SSTableReader.PartitionPositionBounds> sstableSections =
    sstable.getPositionsForRanges(tokenRanges);
long estimatedKeys = sstable.estimatedKeysForRanges(tokenRanges);
Ref<SSTableReader> ref = sstable.ref();
OutgoingStream stream = new CassandraOutgoingFile(...);
```

```java
// AFTER
List<SSTableReader.PartitionPositionBounds> sstableSections =
    sstable.getPositionsForRanges(tokenRanges);
// Do not stream to nodes that don't own any part of the SSTable.
if (sstableSections.isEmpty())
    continue;
long estimatedKeys = sstable.estimatedKeysForRanges(tokenRanges);
```

---

## How to Spot

Whenever a loop builds work items from a filtered or range-bounded list, add an emptiness guard before any downstream resource acquisition or I/O. The pattern `getPositionsForRanges` → `ref()` → `new OutgoingFile(...)` allocates a reference-counted resource even for zero-byte payloads; callers should always check whether the filtered result is non-empty before committing to that allocation.

---

## Test

No dedicated test added in this commit; the companion streaming-tasks fix (also CASSANDRA-16349) covers the scenario.
