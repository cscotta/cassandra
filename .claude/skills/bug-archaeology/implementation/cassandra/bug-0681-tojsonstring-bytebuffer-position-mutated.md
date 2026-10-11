# Bug 0681: ByteBuffer Position Advanced in toJSONString for Collections, Tuples, and UDTs

**Commit**: cb6fad3efc · **Ticket**: CASSANDRA-13592
**Subsystem**: `src/java/org/apache/cassandra/db/marshal/`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ListType`, `MapType`, and `TupleType` all drove their `toJSONString` method by passing the caller-supplied `ByteBuffer` directly to `CollectionSerializer.readValue` / `readCollectionSize`, which advance the buffer's position as a side effect. Because the buffer was not duplicated before iteration, the callers' buffer state was mutated. When the same partition column was serialized across multiple result pages, the second and subsequent pages read from an already-advanced position and produced garbage JSON output.

### Before / After

```java
// BEFORE (ListType)
int size = CollectionSerializer.readCollectionSize(buffer, protocolVersion);
for (int i = 0; i < size; i++)
    sb.append(elementsType.toJSONString(
        CollectionSerializer.readValue(buffer, protocolVersion), protocolVersion));
```

```java
// AFTER (ListType)
ByteBuffer value = buffer.duplicate();
int size = CollectionSerializer.readCollectionSize(value, protocolVersion);
for (int i = 0; i < size; i++)
    sb.append(elementsType.toJSONString(
        CollectionSerializer.readValue(value, protocolVersion), protocolVersion));
```

---

## How to Spot

Any method that accepts a `ByteBuffer` and reads from it sequentially without calling `buffer.duplicate()` first is a candidate for this class of bug. Reviewers should check that `toJSONString`, `toDisplayString`, and similar read-only presentation methods never mutate the caller's buffer position. The symptom is correct output on the first read but corrupted output on subsequent reads of the same value (e.g., across paging boundaries).

---

## Test

New tests in `JsonConversionTest` verify that `toJSONString` leaves the buffer position unchanged and produces the expected JSON output for all compound types; paging tests in `JsonTest` confirm correct output across multiple page sizes for frozen tuples, maps, sets, lists, and UDTs used as partition keys.
