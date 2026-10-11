# Bug 1937: Index summary deserialization crashes on empty partition key

**Commit**: c49ad3ccf2 · **Ticket**: CASSANDRA-5965
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexSummary.java, src/java/org/apache/cassandra/utils/ByteBufferUtil.java
**Category**: Missing null / bounds check

---

## Root Cause

`IndexSummary.serializer.deserialize` called `ByteBufferUtil.readBytes(in, len)` for every entry, but `readBytes` asserted `length > 0`. A valid SSTable can have an empty byte array as a partition key (e.g., when using `ByteOrderedPartitioner` with an empty key), so deserializing such a summary would fail with an `AssertionError`. The fix special-cases a length of zero to return `new byte[0]` before delegating to `readBytes`.

### Before / After

```java
// BEFORE — crashes when len == 0
keys[i] = ByteBufferUtil.readBytes(in, in.readInt());
```

```java
// AFTER — handles zero-length key
int len = in.readInt();
keys[i] = len == 0 ? new byte[0] : ByteBufferUtil.readBytes(in, len);
```

---

## How to Spot

Whenever deserializing length-prefixed byte arrays, check whether a zero length is a valid sentinel or a real case before forwarding to helpers that assert positivity. Empty keys are legitimate in Cassandra's partitioner model and must not be dismissed as impossible.

---

## Test

`IndexSummaryTest.testAddEmptyKey` — builds a summary containing an empty key, serializes it, deserializes it, and verifies the key and position survive the round-trip.
