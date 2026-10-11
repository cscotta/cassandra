# Bug 2038: RowIndexEntry.serializedSize Omits Promoted Index Size

**Commit**: e30c730355 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RowIndexEntry.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`RowIndexEntry.serializedSize()` returned only the size of the position field, omitting the promoted index data for indexed rows. Additionally, the serializer wrote `rie.serializedSize()` (the full entry size including position) as the length prefix for the promoted index block, when it should have written only the promoted portion's size. The fix introduces a `promotedSize()` method that returns the size of just the promoted data (0 for non-indexed rows, the full promoted block size for indexed rows), `serializedSize()` adds `promotedSize()` to the position size, and the serializer uses `promotedSize()` for the inline length field.

### Before / After

```java
// BEFORE
public int serializedSize()
{
    return TypeSizes.NATIVE.sizeof(position);
}
// Serializer:
dos.writeInt(rie.serializedSize());
```

```java
// AFTER
public int serializedSize()
{
    return TypeSizes.NATIVE.sizeof(position) + promotedSize();
}
protected int promotedSize() { return 0; }
// Serializer:
dos.writeInt(rie.promotedSize());
```

---

## How to Spot

When a size-reporting method is also used as a length prefix in a serialization stream, carefully distinguish between "total serialized size of this object" and "size of an embedded sub-record written inline." Using the wrong value for one of these purposes corrupts the stream.

---

## Test

No test added.
