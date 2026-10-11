# Bug 0089: LocalPartitioner Duplicated per *_for_key Accord System Table

**Commit**: 5385e9c60a · **Ticket**: CASSANDRA-19265
**Subsystem**: service/accord — AccordKeyspace
**Category**: State not cleaned up

---

## Root Cause

The two Accord system tables that share the `(store_id, key_token, key)` composite partition key — `timestamps_for_key` and `commands_for_key` — each constructed a separate `new LocalPartitioner(CompositeType...)` instance inline in their table-builder call. Because `LocalPartitioner` registers itself or carries identity-sensitive state, having two distinct instances with identical type descriptors causes duplication and potential inconsistency when these partitioners are compared or used to route reads. The fix introduces a single shared `FOR_KEYS_LOCAL_PARTITIONER` constant and references it from both table definitions.

### Before / After

```java
// BEFORE (timestamps_for_key table)
.partitioner(new LocalPartitioner(CompositeType.getInstance(Int32Type.instance, BytesType.instance, KEY_TYPE)))

// BEFORE (commands_for_key table — another new instance)
.partitioner(new LocalPartitioner(CompositeType.getInstance(Int32Type.instance, BytesType.instance, KEY_TYPE)))
```

```java
// AFTER — one shared constant
private static final LocalPartitioner FOR_KEYS_LOCAL_PARTITIONER =
    new LocalPartitioner(CompositeType.getInstance(Int32Type.instance, BytesType.instance, KEY_TYPE));

// Both tables:
.partitioner(FOR_KEYS_LOCAL_PARTITIONER)
```

---

## How to Spot

When multiple schema objects (tables, indexes) share an identical partitioner type, look for constructor calls duplicated inline. Any `new LocalPartitioner(...)` or similar identity-sensitive factory call that appears more than once with the same arguments is a sign that a shared constant should be used instead. Code-review tools or static analysis can flag `new` expressions inside builder chains where the resulting object is never stored.

---

## Test

No test added.
