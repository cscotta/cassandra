# Bug 0682: Nested Tuple/UDT Validation Delegated to BytesSerializer, Not Type-Aware Serializer

**Commit**: b77e11cfd5 · **Ticket**: CASSANDRA-13646
**Subsystem**: `src/java/org/apache/cassandra/db/marshal/`, `src/java/org/apache/cassandra/serializers/`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`TupleType.getSerializer()` returned `BytesSerializer.instance`, whose `validate()` method performs no structural checking — it accepts any byte sequence as valid. As a result, tuples or UDTs nested inside a collection or another tuple could contain the wrong number of fields and pass server-side validation silently. The fix extracted the field-level validation logic out of `TupleType.validate()` and `UserType.validate()` into new dedicated serializer classes (`TupleSerializer` and `UserTypeSerializer`) whose `validate()` iterates each field against its declared type, and changed `getSerializer()` to return those serializers.

### Before / After

```java
// BEFORE – TupleType
public TypeSerializer<ByteBuffer> getSerializer()
{
    return BytesSerializer.instance;  // no structural validation
}
```

```java
// AFTER – TupleType
public TypeSerializer<ByteBuffer> getSerializer()
{
    return serializer;  // TupleSerializer validates each field
}
```

---

## How to Spot

Whenever a complex type overrides `validate()` directly on the `AbstractType` but returns a generic serializer (e.g., `BytesSerializer`) from `getSerializer()`, the validation is bypassed for any code path that uses the serializer's `validate()` rather than the type's `validate()`. Reviewers should confirm that complex types either centralise all validation inside the serializer returned by `getSerializer()`, or ensure every code path that needs validation calls the type-level method.

---

## Test

Tests in `CollectionsTest`, `TupleTypeTest`, and `UserTypesTest` insert tuples with too many elements (via both bound parameters and CQL literals) nested inside collections and UDTs, and assert the expected `MarshalException` message is produced.
