# Bug 1861: PagedRangeCommand Serializes Row Filter Values with Wrong Length Prefix

**Commit**: 20793b069f · **Ticket**: CASSANDRA-6299
**Subsystem**: db (PagedRangeCommand, RangeSliceCommand)
**Category**: Wrong serialization / deserialization

---

## Root Cause

`IndexExpression.value` was serialized with a 4-byte length prefix (`writeWithLength`) but deserialized with a 2-byte short-length prefix (`readWithShortLength`). This length mismatch corrupted the byte stream for any paged range query that included a secondary-index filter, causing deserialization to read garbage data or throw an exception. The same mismatch existed in the `serializedSize` calculation, which added 4-byte overhead instead of 2-byte overhead, causing size estimation to be off. Additionally, the `start` and `stop` bookmark fields were omitted entirely from `serializedSize`.

### Before / After

```java
// BEFORE
ByteBufferUtil.writeWithLength(expr.value, out);
// and in serializedSize:
size += TypeSizes.NATIVE.sizeofWithLength(expr.value);
```

```java
// AFTER
ByteBufferUtil.writeWithShortLength(expr.value, out);
// and in serializedSize:
size += TypeSizes.NATIVE.sizeofWithShortLength(expr.value);
```

---

## How to Spot

For every field serialized with `writeWithLength` vs `writeWithShortLength`, verify the corresponding `readWithLength` / `readWithShortLength` and `sizeofWithLength` / `sizeofWithShortLength` calls all agree on the same prefix width. A unit test that round-trips a command with a non-empty row filter and secondary-index bookmark would immediately surface this.

---

## Test

No test added.
