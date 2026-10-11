# Bug 1718: Fix nodetool getsstables for Blob Primary Key

**Commit**: 52cf09dbac · **Ticket**: CASSANDRA-6803
**Subsystem**: db/ColumnFamilyStore
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnFamilyStore.getSSTablesForKey()` converted the user-supplied string key to bytes using `String.getBytes()` and wrapped the result with `ByteBuffer.wrap()`, bypassing the column family's key validator entirely. For non-UTF-8 key types such as `blob`, `int`, or `uuid`, this produced an incorrectly encoded `DecoratedKey` that never matched any token in the SSTable index. The fix delegates to `metadata.getKeyValidator().fromString(key)` so the key is deserialized according to the actual column family type.

### Before / After

```java
// BEFORE
DecoratedKey dk = new DecoratedKey(
    partitioner.getToken(ByteBuffer.wrap(key.getBytes())),
    ByteBuffer.wrap(key.getBytes()));
```

```java
// AFTER
DecoratedKey dk = partitioner.decorateKey(
    metadata.getKeyValidator().fromString(key));
```

---

## How to Spot

Any code that converts a user-provided string into a Cassandra key using raw `String.getBytes()` or `ByteBuffer.wrap(str.getBytes())` is wrong whenever the column family uses a non-text key validator. Always use the validator's `fromString()` method.

---

## Test

No test added.
