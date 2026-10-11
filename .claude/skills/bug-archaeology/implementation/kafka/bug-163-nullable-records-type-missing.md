**Commit**: 58d62d1522 · **Ticket**: KAFKA-19634
**Subsystem**: clients/src/main/java/org/apache/kafka/common/protocol/types/ (ArrayOf, Type, Schema, Protocol)
**Category**: Wrong serialization / deserialization

**Root cause**
The Kafka protocol type system did not formally distinguish between nullable and non-nullable arrays, compact arrays, and record sets. `ArrayOf` always printed as `ARRAY(T)` regardless of nullability; `RECORDS` and `COMPACT_RECORDS` had no nullable counterparts tracked in `ApiKeys.hasFlexibleOrZeroCopyVersion`. The `Protocol.java` BNF renderer also used a local variable `responses` that shadowed the outer `schema` variable, causing some response schemas to be rendered with incorrect types. The commit formalises nullable/non-nullable type names, adds `NULLABLE_RECORDS` / `COMPACT_NULLABLE_RECORDS` constants, and fixes the BNF renderer to use the correct schema reference.

**Before/After**
```java
// Before: no nullable RECORDS type, ApiKeys buffer detector missed COMPACT_RECORDS
if (field == BYTES || field == NULLABLE_BYTES || field == RECORDS ||
    field == COMPACT_BYTES || field == COMPACT_NULLABLE_BYTES)
    hasBuffer.set(true);

// After: all four record/byte types covered
boolean isBytesType = field == BYTES || field == NULLABLE_BYTES ||
    field == COMPACT_BYTES || field == COMPACT_NULLABLE_BYTES;
boolean isRecordsType = field == RECORDS || field == NULLABLE_RECORDS ||
    field == COMPACT_RECORDS || field == COMPACT_NULLABLE_RECORDS;
if (isBytesType || isRecordsType) hasBuffer.set(true);
```

**How to spot**
Any place in the type system where nullability is a boolean on a shared type rather than expressed through distinct type objects can lead to inconsistent handling. When adding a new type variant, check all switch/if chains that enumerate type constants — each must be extended to include the new variant.

**Test**
New unit tests added to verify consistency between schema type declarations and message serialization for nullable vs. non-nullable fields.
