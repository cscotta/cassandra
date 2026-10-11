# Bug 1181: Fix CFMetaData IDs Serialization

**Commit**: 6249a3b1e6 · **Ticket**: CASSANDRA-9848
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CFMetaData.Serializer` hand-rolled UUID serialization using `Long.reverseBytes` (little-endian encoding) instead of delegating to `UUIDSerializer`. The rest of the storage engine serialization stack was migrated to use `DataInputPlus` / `DataOutputPlus` (which support vint encoding and consistent byte ordering), but this custom serializer kept raw `DataInput` and a bespoke 16-byte little-endian format. When the deserialization path was wired to a `DataInputPlus` stream, the type mismatch and encoding mismatch caused reads to produce wrong UUID values, failing to look up the correct `CFMetaData` from the schema. The fix replaces the custom encoding entirely with `UUIDSerializer.serializer`, aligns the input type to `DataInputPlus`, and derives `serializedSize` from the serializer rather than hard-coding 16.

### Before / After

```java
// BEFORE
out.writeLong(Long.reverseBytes(metadata.cfId.getMostSignificantBits()));
out.writeLong(Long.reverseBytes(metadata.cfId.getLeastSignificantBits()));
// ...
UUID cfId = new UUID(Long.reverseBytes(in.readLong()), Long.reverseBytes(in.readLong()));
```

```java
// AFTER
UUIDSerializer.serializer.serialize(metadata.cfId, out, version);
// ...
UUID cfId = UUIDSerializer.serializer.deserialize(in, version);
```

---

## How to Spot

Duplicate serialization implementations for the same logical type (UUID, timestamp, etc.) are a maintenance hazard: when the canonical serializer's encoding changes, the hand-rolled copy silently diverges. Reviewers should flag any manual `reverseBytes` / raw `readLong` patterns for known types that have a designated `IVersionedSerializer` and flag `DataInput` parameters where `DataInputPlus` is expected throughout the surrounding codebase.

---

## Test

No dedicated test added; the fix is validated by existing read-path integration tests that exercise `CFMetaData` round-trip deserialization during node communication.
