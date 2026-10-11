# Bug 1940: CqlRecordWriter corrupts composite partition key by reusing ByteBuffer position state

**Commit**: a35f1c288f · **Ticket**: CASSANDRA-5949
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlRecordWriter.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When building a composite partition key in `CqlRecordWriter`, the code retrieved each component `ByteBuffer` directly from the key-column map and passed it to `CompositeType.build`. `CompositeType.build` consumes (`get`/`flip`) the position of each buffer as it encodes components. Because the same `ByteBuffer` instance was reused without duplication, the first call to `build` advanced the buffer's position to its limit, and any subsequent use of the same buffer (e.g., retry or multi-row write) would read zero bytes, silently producing a truncated or empty key component.

### Before / After

```java
// BEFORE — original buffer handed directly to build()
keys[i] = keyColumns.get(partitionKeyColumns[i]);
```

```java
// AFTER — duplicate preserves original position/limit
keys[i] = keyColumns.get(partitionKeyColumns[i]).duplicate();
```

---

## How to Spot

Whenever a `ByteBuffer` is passed to a serialization helper that may advance its position (particularly Cassandra's composite type builders), the caller must `duplicate()` first if the buffer may be accessed again. This is especially important in loops or retry paths.

---

## Test

No test added.
