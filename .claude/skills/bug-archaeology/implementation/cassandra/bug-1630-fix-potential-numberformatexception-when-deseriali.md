# Bug 1630: Fix potential NumberFormatException when deserializing

**Commit**: d6f32e4fc0 · **Ticket**: CASSANDRA-7088
**Subsystem**: serializers (IntegerSerializer, LongSerializer, UUIDSerializer, TimestampSerializer, InetAddressSerializer), db/marshal/AbstractType
**Category**: Wrong serialization / deserialization

---

## Root Cause

Several `TypeSerializer.toString(value)` implementations passed the deserialized value directly to string-conversion methods without guarding against `null`. `IntegerSerializer.deserialize` returned a non-null `BigInteger` even for an empty `ByteBuffer` (calling `new BigInteger(new byte[0])` which throws `NumberFormatException`). Meanwhile, `AbstractType.getString` called `serializer.toString(serializer.deserialize(bytes))` and separately null-checked the result, but the null-check could never fire because `toString` itself would NPE first. The fix makes `IntegerSerializer.deserialize` return `null` for empty input, and adds null guards to all `toString` methods across the affected serializers.

### Before / After

```java
// BEFORE – IntegerSerializer
public BigInteger deserialize(ByteBuffer bytes)
{
    return new BigInteger(ByteBufferUtil.getArray(bytes));
    // throws NumberFormatException for empty byte[]
}

public String toString(BigInteger value)
{
    return value.toString(10);  // NPE if value is null
}
```

```java
// AFTER
public BigInteger deserialize(ByteBuffer bytes)
{
    return bytes.hasRemaining() ? new BigInteger(ByteBufferUtil.getArray(bytes)) : null;
}

public String toString(BigInteger value)
{
    return value == null ? "" : value.toString(10);
}
```

---

## How to Spot

Any serializer that constructs an object from raw bytes without checking whether the buffer is empty first will throw on legitimate "null/unset" column values. Review every `deserialize` method to confirm it handles zero-remaining buffers, and every `toString(value)` to confirm it handles `null`.

---

## Test

No test added.
