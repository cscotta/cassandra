# Bug 1109: Fix Dealing with In-Row RTs from Thrift

**Commit**: d96d175881 · **Ticket**: CASSANDRA-10046
**Subsystem**: src/java/org/apache/cassandra/db/LegacyLayout.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When Thrift writes a mutation containing range tombstones, `CassandraServer` constructed a `MutableDeletionInfo` directly and added all ranges to it using the standard `RangeTombstone` path. However, the Thrift/legacy model distinguishes between "partition-level" range tombstones (which belong in `DeletionInfo`) and "in-row" range tombstones (collection tombstones and row-deletion markers encoded as ranges), which must be emitted as separate cells in the 3.0 storage format. By bypassing `LegacyDeletionInfo`, the in-row tombstones were collapsed into the partition deletion info and lost their identity, resulting in incorrect row/collection deletions being stored. The fix routes all range tombstone construction through `LegacyDeletionInfo.add(CFMetaData, LegacyRangeTombstone)`, which correctly classifies each tombstone and places it in the right bucket.

### Before / After

```java
// BEFORE (CassandraServer)
MutableDeletionInfo delInfo = MutableDeletionInfo.live();
// all ranges go straight into partition deletion info
delInfo.add(new RangeTombstone(...), cfm.comparator);
```

```java
// AFTER (CassandraServer)
LegacyLayout.LegacyDeletionInfo delInfo = LegacyLayout.LegacyDeletionInfo.live();
// in-row tombstones are separated from partition tombstones
delInfo.add(cfm, new LegacyLayout.LegacyRangeTombstone(start, end, deletionTime));
```

---

## How to Spot

Any code path that bridges the Thrift/legacy wire format to the 3.0 storage model must use the `LegacyLayout` abstraction layer rather than constructing `MutableDeletionInfo` or `RangeTombstone` directly. When reviewing Thrift-to-CQL conversion code, verify that range tombstone handling calls through `LegacyDeletionInfo` so that the classifier (`isCollectionTombstone`, `isRowDeletion`) can route each tombstone correctly.

---

## Test

No new test added. The fix is covered by the broader Thrift compatibility test suite.
