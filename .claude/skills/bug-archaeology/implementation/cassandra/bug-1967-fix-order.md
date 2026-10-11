# Bug 1967: QueryOptions.encode writes flags before consistency level, violating protocol order

**Commit**: 791522597e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryOptions.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`QueryOptions.Codec.encode` added the flags byte to the serialization buffer before the consistency level, but the CQL native protocol specifies that the consistency level must precede the flags byte in the wire encoding. This inverted order would cause clients to misparse query responses — interpreting the flags byte as the consistency level and vice versa — resulting in silent data corruption at the protocol layer.

### Before / After

```java
// BEFORE – wrong order: flags first, then consistency
builder.add(CBUtil.byteToCB((byte) Flag.serialize(flags)));
builder.add(CBUtil.consistencyLevelToCB(options.getConsistency()));
```

```java
// AFTER – correct order: consistency first, then flags
builder.add(CBUtil.consistencyLevelToCB(options.getConsistency()));
builder.add(CBUtil.byteToCB((byte) Flag.serialize(flags)));
```

---

## How to Spot

Any serialization `encode` method must be the exact mirror image of its corresponding `decode` method with fields written in the same order as they are read. When reviewing protocol codecs, always cross-check the encode sequence against the decode sequence (and against the protocol specification if available) field by field.

---

## Test

No test added.
