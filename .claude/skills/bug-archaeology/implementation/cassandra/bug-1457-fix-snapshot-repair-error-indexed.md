# Bug 1457: Snapshot Repair Fails on Indexed Tables

**Commit**: ca224b3239 · **Ticket**: CASSANDRA-8020
**Subsystem**: src/java/org/apache/cassandra/repair/RepairMessageVerbHandler.java
**Category**: Missing null / bounds check

---

## Root Cause

When snapshot repair collected SSTables to validate, it applied a token-range intersection predicate to all SSTables in the column family store, including those belonging to secondary index column families. Secondary index SSTables use `LocalPartitioner`, whose tokens are not comparable with ring tokens, so calling `Bounds.intersects` on them causes an error. The fix adds an explicit exclusion for SSTables whose partitioner is `LocalPartitioner`, mirroring the guard that exists elsewhere in repair and streaming code.

### Before / After

```java
// BEFORE
return sstable != null &&
    new Bounds<>(sstable.first.getToken(), sstable.last.getToken())
        .intersects(Collections.singleton(repairingRange));
```

```java
// AFTER
return sstable != null &&
    !(sstable.partitioner instanceof LocalPartitioner) && // exclude SSTables from 2i
    new Bounds<>(sstable.first.getToken(), sstable.last.getToken())
        .intersects(Collections.singleton(repairingRange));
```

---

## How to Spot

Any code that iterates over all SSTables of a `ColumnFamilyStore` and performs token-range arithmetic should check whether the SSTable belongs to a secondary index (i.e. its partitioner is `LocalPartitioner`). Secondary index SSTables live alongside base-table SSTables but use a fundamentally different key space; mixing them in ring-token operations silently produces errors or incorrect results.

---

## Test

No test added.
