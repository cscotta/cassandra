# Bug 2223: RangeTombstone Size Calculated with Wrong Serializer Method

**Commit**: 074f4befaa · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstone.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When computing the serialized size of range tombstones being written to an SSTable, the code called `tombstone.serializedSize(typeSizes)` using the generic `TypeSizes.NATIVE` instance. This method computes the size for in-memory or network serialization, which differs from the on-disk SSTable format. The correct method is `serializedSizeForSSTable()`, which accounts for SSTable-specific framing. Using the wrong size could produce an incorrect pre-allocated buffer length or an inaccurate atom count, leading to corrupt output.

### Before / After

```java
// BEFORE
TypeSizes typeSizes = TypeSizes.NATIVE;
for (RangeTombstone tombstone : toWrite) {
    size += tombstone.serializedSize(typeSizes);
    ...
}
```

```java
// AFTER
for (RangeTombstone tombstone : toWrite) {
    size += tombstone.serializedSizeForSSTable();
    ...
}
```

---

## How to Spot

When a class provides both a generic `serializedSize(TypeSizes)` method and a format-specific variant such as `serializedSizeForSSTable()`, all SSTable write paths must use the format-specific variant. Mixed usage causes size mismatches that can silently corrupt files or cause read failures later.

---

## Test

No test added.
