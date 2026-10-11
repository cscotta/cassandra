# Bug 1468: Fix RowIndexEntry to Report Correct serializedSize

**Commit**: 85aa794e1f · **Ticket**: CASSANDRA-7948
**Subsystem**: src/java/org/apache/cassandra/db/RowIndexEntry.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`RowIndexEntry.serializedSize()` returned only `sizeof(position) + promotedSize()`, where `promotedSize()` is the raw byte count of the promoted index block. However, the actual serialized format also writes a 4-byte integer for the promoted-size field itself, plus (when the entry is indexed) a `DeletionTime`, a count integer, and one `IndexInfo` record per index block. The method therefore understated the true on-disk size, which could cause callers that pre-allocate buffers based on `serializedSize()` to overflow.

### Before / After

```java
// BEFORE — missing the length-prefix integer and indexed content
public int serializedSize() {
    return TypeSizes.NATIVE.sizeof(position) + promotedSize();
}
```

```java
// AFTER — accounts for the length prefix and all indexed sub-fields
public int serializedSize() {
    int size = TypeSizes.NATIVE.sizeof(position) + TypeSizes.NATIVE.sizeof(promotedSize());
    if (isIndexed()) {
        List<IndexHelper.IndexInfo> index = columnsIndex();
        size += DeletionTime.serializer.serializedSize(deletionTime(), TypeSizes.NATIVE);
        size += TypeSizes.NATIVE.sizeof(index.size());
        for (IndexHelper.IndexInfo info : index)
            size += info.serializedSize(TypeSizes.NATIVE);
    }
    return size;
}
```

---

## How to Spot

Whenever a `serializedSize()` method delegates to a helper (like `promotedSize()`), verify that every field written by the actual `serialize()` method is accounted for, including length-prefix integers and conditional sub-structures. Discrepancies between `serialize` and `serializedSize` are easiest to catch by writing a round-trip test that compares `serializedSize()` against the actual bytes written.

---

## Test

New `RowIndexEntryTest.testSerializedSize()`: serializes both a simple (non-indexed) and a promoted-index entry into a `DataOutputBuffer`, then asserts that `buffer.size() == entry.serializedSize()`.
