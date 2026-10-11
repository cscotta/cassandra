# Bug 1867: Fix updating column_metadata from thrift

**Commit**: 2c4b043f94 · **Ticket**: CASSANDRA-6182
**Subsystem**: config (CFMetaData, ColumnDefinition)
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnDefinition.deleteFromSchema()` constructed the schema column-key using the raw binary
`name` bytes directly, without first converting them through the column comparator's string
representation.  For Thrift-defined columns the comparator may not be UTF-8, so the raw bytes
produce a key that does not match what was originally written when the column was created.
As a result, the deletion range tombstone targeted the wrong key and the column metadata row
was never actually removed, leaving stale schema entries after a `column_metadata` update
from Thrift.

### Before / After

```java
// BEFORE – raw name bytes used as column key, wrong for non-UTF8 comparators
builder.add(ByteBufferUtil.bytes(cfName)).add(name);
```

```java
// AFTER – name decoded through the comparator to produce the correct string key
ByteBuffer nameBytes = ByteBufferUtil.bytes(comparator.getString(name));
builder.add(ByteBufferUtil.bytes(cfName)).add(nameBytes);
```

---

## How to Spot

Whenever a schema or index row is identified by a key that encodes a user-supplied name, verify
that the same encoding path is used for both writes and deletes.  Asymmetry between insert-time
encoding (comparator-aware) and delete-time encoding (raw bytes) will cause deletes to silently
miss their target.

---

## Test

No test added.
