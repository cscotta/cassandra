# Bug 1812: Fix streaming older SSTable yields row tombstones

**Commit**: 2111a20b4b · **Ticket**: CASSANDRA-6527
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When streaming pre-`ja` format SSTables, the code skipped 8 bytes for the row-size field after reading the deletion info, then read the column count. For older SSTable versions (`< ja`) the row-size field precedes the deletion info on disk, not after it. Consequently, 8 bytes of the actual data (including the deletion time) were misread as the row-size skip, causing real column data to be interpreted as a row tombstone and subsequently streamed as deleted rows. The fix moves the 8-byte skip to before `cf.delete(...)` so the field order matches the on-disk layout.

### Before / After

```java
// BEFORE
cf.delete(DeletionTime.serializer.deserialize(in));
...
if (version.hasRowSizeAndColumnCount)
{
    FileUtils.skipBytesFully(in, 8); // skip row size
    columnCount = in.readInt();
}
```

```java
// AFTER
// skip row size for version < ja
if (version.hasRowSizeAndColumnCount)
    FileUtils.skipBytesFully(in, 8);

cf.delete(DeletionTime.serializer.deserialize(in));
...
// read column count for version < ja
if (version.hasRowSizeAndColumnCount)
    columnCount = in.readInt();
```

---

## How to Spot

When reading versioned binary formats, field-ordering bugs are introduced by placing skip/read operations outside their correct version guard. Always map the skip/read sequence directly to the documented on-disk layout for each version branch, and verify it with a round-trip test against real legacy fixtures.

---

## Test

`LegacySSTableTest.testStreaming()` was added; it streams all legacy SSTable versions and asserts `DeletionInfo.live()` on each row (i.e., no spurious tombstones) and that the expected column value is present.
