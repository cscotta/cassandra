# Bug 0311: Data Corruption in AbstractCompositeType Due to Static Boolean Byte Buffers

**Commit**: fb66800a00 · **Ticket**: CASSANDRA-14752
**Subsystem**: src/java/org/apache/cassandra/db/marshal/AbstractCompositeType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`BooleanSerializer.serialize()` (and similar serializers) returned a reference to a static, pre-allocated `ByteBuffer` singleton rather than a fresh buffer on each call. `AbstractCompositeType.fromString()` collected these serialized component buffers and then wrote them into an output buffer via `bb.put(component)`, which advances the source buffer's `position` pointer to its `limit`. Because the source buffer was the shared static singleton, its position was permanently advanced to the end after the first use. Any subsequent call to serialize the same boolean value would return a buffer with `position == limit`, producing zero bytes in the output — silently truncating or corrupting the composite key.

The fix calls `component.duplicate()` before the `put`, so the position of the static singleton is never mutated.

### Before / After

```java
// BEFORE – consumes (advances position of) the component buffer, corrupts static singletons
bb.put(component);
```

```java
// AFTER – duplicates the buffer so the original position is preserved
bb.put(component.duplicate()); // it's not ok to consume component as we did not create it (CASSANDRA-14752)
```

---

## How to Spot

Any code that calls `ByteBuffer.put(ByteBuffer src)` must own the `src` buffer or operate on a duplicate, because `put` advances `src.position`. When reviewing serializer code that aggregates components from external serializers, confirm each component is duplicated before being consumed. Static or cached `ByteBuffer` singletons are especially dangerous here because the corruption is silent and only manifests on the second (or later) use.

---

## Test

`ColumnFamilyStoreCQLHelperTest.testBooleanCompositeKey` inserts a row with a boolean composite key, calls `getSSTablesForKey` (which triggers the corrupting serialization path), then inserts a second row and asserts both rows are returned correctly.
