# Bug 0687: ColumnFilter Serialization Missing fetched Columns — Coordinator and Replica See Different Columns After Schema Change

**Commit**: 1f54aa424f · **Ticket**: CASSANDRA-13004
**Subsystem**: `src/java/org/apache/cassandra/db/filter/ColumnFilter.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnFilter` distinguished "fetched" columns (everything the replica should read from disk) from "queried" columns (the subset the coordinator requested). When `fetchAllRegulars` was true, the coordinator evaluated `fetched` lazily from `metadata.partitionColumns()` at call time; the serialized form sent to replicas contained only the `queried` subset. A replica that had already received an `ALTER TABLE ADD/DROP COLUMN` schema update would reconstruct `fetched` from its own (updated) metadata, making its view of which columns to fetch differ from the coordinator's. This produced digest mismatches and could silently drop or duplicate cells. The fix serialises the concrete `fetched` column set explicitly at messaging version 3014 and bumps the messaging protocol to that version so that all nodes agree on the column set used by a given read.

### Before / After

```java
// BEFORE – serialize: only queried columns sent on wire
if (selection.selection != null) {
    Columns.serializer.serialize(selection.selection.statics, out);
    Columns.serializer.serialize(selection.selection.regulars, out);
}
```

```java
// AFTER – also send fetched columns for fetchAll filters
if (version >= MessagingService.VERSION_3014 && selection.isFetchAll) {
    Columns.serializer.serialize(selection.fetched.statics, out);
    Columns.serializer.serialize(selection.fetched.regulars, out);
}
if (selection.queried != null) {
    Columns.serializer.serialize(selection.queried.statics, out);
    Columns.serializer.serialize(selection.queried.regulars, out);
}
```

---

## How to Spot

When a distributed operation computes a value lazily from local metadata on each node, any schema change that races with that operation can produce divergent behaviour. Reviewers should look for `metadata.partitionColumns()` or similar calls inside serialization/deserialization paths — if the data required to reconstruct a field is not included in the wire format, nodes with different schema versions will disagree. Explicit wire representation of the computed field is the reliable fix.

---

## Test

`ColumnFilterTest.columnFilterSerialisationRoundTrip` covers round-trip serialization for `ColumnFilter.all()` and `ColumnFilter.selection()` across messaging versions 3.0 and 3014, for tables with and without static columns.
