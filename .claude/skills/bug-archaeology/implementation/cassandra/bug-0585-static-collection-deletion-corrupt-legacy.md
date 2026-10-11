# Bug 0585: Corrupted Static Collection Deletions in 3.0 to 2.x Legacy Serialization

**Commit**: d52c7b8c59 · **Ticket**: CASSANDRA-14568
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`, `db/marshal/CompositeType.java`, `db/partitions/PartitionUpdate.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

When decoding a legacy Cassandra 2.x composite key bound, the code assumed that any extra component beyond the clustering columns must be a collection name located at `clusteringSize` (the index after the last clustering column). For static rows the composite name has no clustering components at all, so the collection name is at index 0, not at `clusteringSize`. The code did not detect the static case before calling `components.remove(clusteringSize)`, which either indexed out-of-bounds or picked the wrong element, corrupting the collection-deletion range tombstone. A parallel defect in `LegacyLayout.encodeBound` always built the composite using a non-static `Builder`, again producing a malformed byte sequence when re-serializing a static range tombstone.

### Before / After

```java
// BEFORE — always removes at clusteringSize, wrong for static rows
if (components.size() > clusteringSize)
    collectionName = metadata.getColumnDefinition(components.remove(clusteringSize));
...
return new LegacyBound(sb, metadata.isCompound() && CompositeType.isStaticName(bound), collectionName);

// encode side
CompositeType.Builder startBuilder = type.builder();   // never static
```

```java
// AFTER — detects static upfront, removes at index 0 for static
boolean isStatic = metadata.isCompound() && CompositeType.isStaticName(bound);
if (components.size() > (isStatic ? 0 : clusteringSize))
{
    ByteBuffer collectionNameBytes = components.remove(isStatic ? 0 : clusteringSize);
    collectionName = metadata.getColumnDefinition(collectionNameBytes);
}
...
return new LegacyBound(sb, isStatic, collectionName);

// encode side
CompositeType.Builder startBuilder = type.builder(start.isStatic);
```

---

## How to Spot

Whenever code processes a composite key layout that differs structurally between static and regular rows, every place that uses a hardcoded index (like `clusteringSize`) should be audited to confirm it handles both the static layout (no clustering prefix) and the regular layout. Calling `isStaticName` after the index operation instead of before is a particularly dangerous pattern.

---

## Test

Two new round-trip tests verify that a static collection deletion serialized to 3.0 format and deserialized through the 2.1 path neither deletes an unexpected regular row nor produces a byte stream that causes a `CorruptSSTableException` on subsequent read.
