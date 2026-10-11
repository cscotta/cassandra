# Bug 0894: Legacy Non-Compound Range Tombstone Serialized Using Wrong Format

**Commit**: c98b6484e1 · **Ticket**: CASSANDRA-11930
**Subsystem**: Serialization / Legacy Layout
**Category**: Wrong serialization / deserialization

---

## Root Cause
`LegacyLayout.LegacyRangeTombstoneList.serialize()` always used the composite-type path to write range tombstones, even for non-compound (single-column-clustering) tables that existed in older Thrift schemas. For a non-compound table the comparator has exactly one subtype and the correct on-wire format is to write each start/end key directly with a `shortLength` prefix (the pre-3.0 simple format). Instead the code built a `CompositeType` from the single subtype and wrote the composite encoding, producing bytes that 2.x nodes could not parse, causing errors in mixed 3.x/2.x clusters whenever such range tombstones were streamed or forwarded. The fix splits `serialize()` into `serializeCompound()` (the original composite path) and `serializeSimple()` (direct `writeWithShortLength` per bound), dispatching on `metadata.isCompound()`.

### Before / After
```java
// BEFORE — always uses CompositeType even for non-compound tables
List<AbstractType<?>> types = new ArrayList<>(comparator.clusteringComparator.subtypes());
if (!metadata.isDense())
    types.add(UTF8Type.instance);
CompositeType type = CompositeType.getInstance(types);
// ... encodes start/end using composite serializer

// AFTER — branches on compound vs simple
if (metadata.isCompound())
    serializeCompound(out, metadata.isDense());
else
    serializeSimple(out);

// serializeSimple writes each bound directly:
ByteBufferUtil.writeWithShortLength(startClustering.get(0), out);
ByteBufferUtil.writeWithShortLength(endClustering.get(0), out);
out.writeInt(delTimes[i]);
out.writeLong(markedAts[i]);
```

---

## How to Spot
Legacy serialization paths that handle both compound and non-compound table schemas must explicitly branch on `isCompound()`. A single serialization path that always constructs a `CompositeType` will silently produce malformed bytes for single-component clustering schemas. When reviewing mixed-version serialization code, check whether every branch that writes a cell name or bound is conditional on the table type, and trace whether the resulting bytes match what older nodes expect.

---

## Test
No test added.
