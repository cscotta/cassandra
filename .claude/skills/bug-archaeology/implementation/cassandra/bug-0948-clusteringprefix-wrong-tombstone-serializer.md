# Bug 0948: ClusteringPrefix Uses Wrong Serializer for Tombstone Bounds

**Commit**: fe37e0644c · **Ticket**: CASSANDRA-11158
**Subsystem**: Storage Engine / Serialization
**Category**: Wrong serialization / deserialization

---

## Root Cause
`ClusteringPrefix.Serializer` dispatched non-`CLUSTERING` prefixes (i.e., range tombstone start/end bounds) to `Slice.Bound.serializer` for both serialization and deserialization. The correct serializer for those kinds is `RangeTombstone.Bound.serializer`. The two serializers differ in their on-disk layout, so reading a `RangeTombstone.Bound` through `Slice.Bound.serializer` produced corrupted clustering values or threw deserialization errors when iterating SSTables that contained range tombstone boundaries. `RangeTombstone.Bound.serializer` was also missing the `deserializeValues` method, which was added as part of the fix.

### Before / After
```java
// BEFORE — wrong serializer for tombstone bounds
Slice.Bound.serializer.serialize((Slice.Bound) clustering, out, version, types);
return Slice.Bound.serializer.deserializeValues(in, kind, version, types);
return 1 + Slice.Bound.serializer.serializedSize((Slice.Bound) clustering, version, types);

// AFTER — correct serializer for tombstone bounds
RangeTombstone.Bound.serializer.serialize((RangeTombstone.Bound) clustering, out, version, types);
return RangeTombstone.Bound.serializer.deserializeValues(in, kind, version, types);
return 1 + RangeTombstone.Bound.serializer.serializedSize((RangeTombstone.Bound) clustering, version, types);
```

---

## How to Spot
When a serializer dispatch uses an `if/else` or switch on `Kind`, check that every non-default branch uses the serializer that matches the concrete type being cast to. A cast to `RangeTombstone.Bound` paired with a call to `Slice.Bound.serializer` is a clear type/serializer mismatch that will silently corrupt round-trips. This pattern is especially easy to miss during refactors that split what was once a single bound type into two.

---

## Test
`TombstonesWithIndexedSSTableTest` was added to verify that range tombstone boundaries are correctly read back from indexed SSTables; `QueryWithIndexedSSTableTest` was also updated.
