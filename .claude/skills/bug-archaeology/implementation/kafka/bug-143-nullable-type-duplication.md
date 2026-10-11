**Commit**: 9a9e497ff8 · **Ticket**: KAFKA-19833
**Subsystem**: clients/src/main/java/org/apache/kafka/common/protocol/types/Type.java
**Category**: Wrong serialization / deserialization

**Root cause**
Nullable protocol types (`NULLABLE_STRING`, `NULLABLE_BYTES`, `COMPACT_NULLABLE_STRING`, etc.) in `Type.java` duplicated the read/write/sizeOf logic from their non-nullable counterparts. This duplication meant that any future correctness fix to the base type would need to be replicated in each nullable variant — a source of divergence bugs. The commit extracts `stringRead()` and `bytesRead()` helpers and delegates nullable type write/read to the non-nullable implementation after null checks, reducing duplication without changing semantics.

**Before/After**
```java
// BEFORE – NULLABLE_STRING.read() duplicated STRING.read() logic inline
if (length > buffer.remaining())
    throw new SchemaException("Error reading string of length " + length + ...);
String result = Utils.utf8(buffer, length);
buffer.position(buffer.position() + length);
return result;

// AFTER – delegates to shared helper
return stringRead(buffer, length);
```

**How to spot**
Copy-pasted read/write logic across sibling type classes is a maintenance hazard. When one copy is patched the others are often missed. Grep for near-identical blocks of `buffer.remaining()` / `Utils.utf8` / `buffer.position()` logic spread across multiple anonymous class bodies in the same file.

**Test**
No new test added; this is a code-quality refactor. Existing serialisation round-trip tests cover correctness.
