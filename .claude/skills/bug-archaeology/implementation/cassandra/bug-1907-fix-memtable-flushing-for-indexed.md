# Bug 1907: Memtable flush silently drops tombstones for secondary-indexed tables

**Commit**: 898ce5f046 · **Ticket**: CASSANDRA-6112
**Subsystem**: src/java/org/apache/cassandra/db/Memtable.java, src/java/org/apache/cassandra/db/index/SecondaryIndexManager.java
**Category**: State not cleaned up

---

## Root Cause

During a memtable flush, `ColumnFamilyStore.removeDeletedColumnsOnly` was called unconditionally to strip columns that had been superseded by partition or range tombstones before writing the SSTable. For tables with secondary indexes this is incorrect: the secondary index contains entries pointing to those deleted columns, and those index entries can only be cleaned up during compaction by inspecting the actual tombstone in the base SSTable. When the tombstone was stripped from the flush output before it reached disk, the compaction logic never saw it, leaving stale index entries that could only be repaired by an expensive read-repair path during a 2i query.

The fix gates `removeDeletedColumnsOnly` behind a check for the absence of secondary indexes, preserving tombstones in flushed SSTables whenever a table has any indexes.

### Before / After

```java
// BEFORE — always removes deleted columns, breaking index cleanup
ColumnFamilyStore.removeDeletedColumnsOnly(cf, Integer.MIN_VALUE);
```

```java
// AFTER — skip removal when secondary indexes exist
if (!cfs.indexManager.hasIndexes())
    ColumnFamilyStore.removeDeletedColumnsOnly(cf, Integer.MIN_VALUE);
```

---

## How to Spot

Any optimization that discards data (tombstones, deletions, version metadata) before persistence must verify that no secondary structure (index, counter, view) depends on seeing that data in the persisted form. The interaction between the flush path and the compaction-based index cleanup path is a common blind spot.

---

## Test

No test added.
