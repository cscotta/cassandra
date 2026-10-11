# Bug 0460: NPE on Empty/Null JSON String Representation in Type Serializers

**Commit**: 64fa75dd92 · **Ticket**: CASSANDRA-15896
**Subsystem**: `src/java/org/apache/cassandra/db/marshal/` (AbstractType, DecimalType, DoubleType, FloatType, InetAddressType, Int32Type, IntegerType, LongType, ShortType, TimestampType)
**Category**: Missing null / bounds check

---

## Root Cause

The `toJSONString(ByteBuffer, int)` methods across multiple concrete `AbstractType` subclasses called `deserialize(buffer).toString()` or `getHostAddress()` directly on the result of deserialization without guarding against a `null` return value. When a column was stored with an empty byte buffer (a valid Cassandra representation of a "meaningless empty value"), the deserializers returned `null`, causing a `NullPointerException` when the JSON formatting path invoked instance methods on that null reference. The base class default implementation in `AbstractType` had the same gap via `getSerializer().deserialize(buffer).toString()`.

### Before / After

```java
// BEFORE — NPE when deserialize() returns null for empty buffer
public String toJSONString(ByteBuffer buffer, int protocolVersion) {
    return '"' + getSerializer().deserialize(buffer).toString() + '"';
}

// InetAddressType
return '"' + getSerializer().deserialize(buffer).getHostAddress() + '"';

// Numeric types (Int32Type, LongType, etc.)
return getSerializer().deserialize(buffer).toString();
```

```java
// AFTER — null-safe via Objects.toString or explicit null guard
public String toJSONString(ByteBuffer buffer, int protocolVersion) {
    return '"' + Objects.toString(getSerializer().deserialize(buffer), "") + '"';
}

// FloatType / DoubleType — explicit guard before further method calls
Float value = getSerializer().deserialize(buffer);
if (value == null)
    return "\"\"";
```

---

## How to Spot

Any `toJSONString` override that chains a method call directly onto the result of a deserializer should be treated as a latent NPE risk: Cassandra allows empty-byte-buffer values for non-string types, and the corresponding deserializers return `null` for zero-length input. Code reviewers should verify that every `toJSONString` implementation calls `Objects.toString(x, fallback)` or guards with an explicit `null` check before accessing fields or calling methods on the deserialized value.

---

## Test

`EmptyValuesTest` inserts an empty byte buffer via both plain `INSERT` and `INSERT JSON` for each affected type, flushes to SSTable, then asserts that `SELECT JSON` returns `"v": ""` (not a NullPointerException) and that `sstabledump` output also contains the empty-string representation.
