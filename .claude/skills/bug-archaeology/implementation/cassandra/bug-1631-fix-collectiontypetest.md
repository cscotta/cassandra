# Bug 1631: Fix CollectionTypeTest

**Commit**: 3b299c4ca1 · **Ticket**: n/a
**Subsystem**: db/marshal (ListType, MapType), serializers/CollectionSerializer
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ListType.compare` and `MapType.compare` were reading collection sizes and element lengths using the old 2-byte (`short`) wire-format helpers (`ByteBufferUtil.readShortLength` / `readBytesWithShortLength`) instead of the version-aware `CollectionSerializer.readCollectionSize` / `readValue` helpers. CQL protocol v3 changed the collection encoding to use 4-byte (`int`) lengths. Using the wrong reader caused comparisons to mis-parse the buffer, producing wrong ordering results. The `readCollectionSize` and `readValue` helpers were also `protected`, preventing the type classes from calling them; they were widened to `public`.

### Before / After

```java
// BEFORE – hard-coded 2-byte reads
int size1 = ByteBufferUtil.readShortLength(bb1);
ByteBuffer v1 = ByteBufferUtil.readBytesWithShortLength(bb1);
```

```java
// AFTER – version-aware reads (passing protocol version 3)
int size1 = CollectionSerializer.readCollectionSize(bb1, 3);
ByteBuffer v1 = CollectionSerializer.readValue(bb1, 3);
```

---

## How to Spot

Whenever a wire-format layout changes between protocol versions, all code that reads that format must go through the version-dispatching helper. Flag any direct use of fixed-width buffer read helpers (e.g., `readShortLength`) inside comparison or serialization logic that may encounter data encoded with a newer protocol version.

---

## Test

`CollectionTypeTest` was fixed (argument-order correction in `assertEquals` calls) and now passes. It asserts that `ListType`, `SetType`, and `MapType` comparators produce strictly increasing order over a sequence of pre-encoded values.
