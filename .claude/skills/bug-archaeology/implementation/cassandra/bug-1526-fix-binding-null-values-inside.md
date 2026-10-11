# Bug 1526: Fix Binding Null Values Inside UDT

**Commit**: a3d8375a2b · **Ticket**: CASSANDRA-7685
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: Missing null / bounds check

---

## Root Cause

When serializing a UDT for storage, `UserTypes.Value.bindAndGet()` iterated over each field, calling `reserializeToV3()` on the buffer when the protocol version was below 3 and the field type was a collection. If the field was bound to `null`, `buffers[i]` would be `null` at that point, and `reserializeToV3(null)` would throw a `NullPointerException`. The fix adds a `buffers[i] != null` guard before calling the serializer, so null-valued collection fields inside a UDT are passed through unchanged.

### Before / After

```java
// BEFORE — NPE when a collection field inside UDT is null
if (version < 3 && type.fieldType(i).isCollection())
    buffers[i] = ((CollectionType)type.fieldType(i)).getSerializer().reserializeToV3(buffers[i]);
```

```java
// AFTER — null field values are skipped safely
if (version < 3 && type.fieldType(i).isCollection() && buffers[i] != null)
    buffers[i] = ((CollectionType)type.fieldType(i)).getSerializer().reserializeToV3(buffers[i]);
```

---

## How to Spot

Any post-processing loop that applies a serialization transformation to elements of an array should guard against null elements, especially when those elements come from user-supplied bind values that may legitimately be null. Review all `getSerializer().reserialize*()` call sites for missing null checks.

---

## Test

No dedicated test added in this commit. The scenario is: INSERT into a table with a UDT column that contains a collection field, binding that collection field to `null` using a protocol v1/v2 client. The assertion is that the write succeeds instead of throwing a NullPointerException.
