# Bug 1251: JSON Map Keys Not Wrapped in Strings When Serialized

**Commit**: b7be1980b1 · **Ticket**: CASSANDRA-9190
**Subsystem**: src/java/org/apache/cassandra/db/marshal/MapType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The JSON specification requires that all object keys be strings. When Cassandra serialized a `map<K, V>` column to JSON for `SELECT JSON`, it called `toJSONString()` on each key and emitted the result verbatim. For non-string Cassandra types (int, boolean, float, UUID, composite types, etc.) `toJSONString()` produces an unquoted token such as `1` or `true`, yielding invalid JSON like `{1: true}`. The symmetric deserialization path (`fromJSONObject`) also failed when given a pre-encoded string representation instead of a parsed Java object, because it did not attempt to re-parse string inputs before checking the expected type. Both directions needed fixing.

### Before / After

```java
// BEFORE – serialization: emits bare non-string key
sb.append(keys.toJSONString(CollectionSerializer.readValue(buffer, protocolVersion), protocolVersion));
```

```java
// AFTER – serialization: wrap non-string keys in quotes
String key = keys.toJSONString(CollectionSerializer.readValue(buffer, protocolVersion), protocolVersion);
if (key.startsWith("\""))
    sb.append(key);
else
    sb.append('"').append(Json.JSON_STRING_ENCODER.quoteAsString(key)).append('"');
```

---

## How to Spot

Any `toJSONString` / `fromJSONObject` pair on a collection type that may hold non-string keys should verify that the serialized output is valid JSON. Reviewer heuristic: whenever a type's `toJSONString` is called in a context that becomes a map key, confirm the result is always quoted. Equally, `fromJSONObject` implementations should guard against string inputs that represent encoded collections, not raw values.

---

## Test

`JsonTest.testInsertJsonSyntaxWithNonNativeMapKeys` inserts and round-trips maps with every supported non-string key type (int, bigint, varint, boolean, float, double, decimal, tuple, UDT, set, list, and nested map) through `INSERT JSON` / `SELECT JSON`.
