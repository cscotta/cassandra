# Bug 1342: Off-Heap Buffer Copy Ignores arrayOffset, Corrupting HSHA and Offheap Objects (2nd Branch)

**Commit**: 708b0ce6da · **Ticket**: CASSANDRA-8719
**Subsystem**: src/java/org/apache/cassandra/utils/memory/MemoryUtil.java
**Category**: Off-by-one

---

## Root Cause

Same `arrayOffset()` omission as CASSANDRA-8719 applied to a second release branch. When copying from a heap `ByteBuffer` to an off-heap address, `MemoryUtil.setBytes` passed `start` directly as the array offset, ignoring any `arrayOffset()` the buffer carried from slicing or duplication. The result was silent memory corruption wherever the HSHA or offheap object allocator used a sliced buffer.

### Before / After

```java
// BEFORE — arrayOffset() not accounted for
setBytes(address, buffer.array(), start, count);
```

```java
// AFTER — correct absolute array position
setBytes(address, buffer.array(), buffer.arrayOffset() + start, count);
```

---

## How to Spot

Treat `buffer.array()` paired with a plain `start` index as a code smell in any utility operating on `ByteBuffer`. Because `ByteBuffer.slice()` and `ByteBuffer.duplicate()` are widely used across the Cassandra I/O stack, any helper that bypasses `get()`/`put()` and indexes the backing array directly must add `arrayOffset()` to every such index. The corruption is invisible at compile time and often only surfaces as data integrity failures under specific workloads.

---

## Test

No test added.
