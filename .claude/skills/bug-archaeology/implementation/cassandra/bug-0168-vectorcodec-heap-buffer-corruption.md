# Bug 0168: VectorCodec Data Corruption on Heap Buffers

**Commit**: 1e8a1a21ab · **Ticket**: CASSANDRA-19168
**Subsystem**: cql3/functions/types (VectorCodec)
**Category**: Wrong serialization / deserialization

---

## Root Cause

`VectorCodec.deserialize()` iterated over vector elements by calling `bytes.slice()` on each loop iteration, which always produces a new view anchored at the *current* position of the original buffer. For direct (`off-heap`) buffers this happened to work because the position was advanced with `bytes.position(bytes.position() + elementSize)`. For heap buffers the slice behaviour is identical in terms of the view, but at the end of the method the code called `bytes.rewind()`, resetting position to 0, so any caller that later read from the same buffer would see stale data starting at position 0 rather than the position after the vector. Additionally, each slice's limit was set to `elementSize` relative to the slice start, meaning the deserialized element was correct but the parent buffer state was corrupted for subsequent use.

The fix replaces the repeated `bytes.slice()` pattern with a single slice `bb = bytes.slice()` taken once before the loop, then advances `bb.position` and resets `bb.limit` explicitly on each iteration, leaving the original `bytes` buffer completely untouched and removing the `rewind()` call.

### Before / After

```java
// BEFORE
for (int i = 0; i < type.getDimensions(); ++i)
{
    ByteBuffer slice = bytes.slice();
    slice.limit(elementSize);
    values.add(subtypeCodec.deserialize(slice, protocolVersion));
    bytes.position(bytes.position() + elementSize);
}
// Restore the input ByteBuffer to its original state
bytes.rewind();
```

```java
// AFTER
ByteBuffer bb = bytes.slice();
for (int i = 0; i < type.getDimensions(); ++i)
{
    int originalPosition = bb.position();
    bb.limit(originalPosition + elementSize);
    values.add(subtypeCodec.deserialize(bb, protocolVersion));
    bb.position(originalPosition + elementSize);
    bb.limit(bb.capacity());
}
```

---

## How to Spot

Whenever a deserialization loop advances through a `ByteBuffer` by repeatedly calling `slice()` on the *same* buffer object and then calling `rewind()` at the end, look for the heap-buffer path: `slice()` on a heap buffer creates a view but does not affect the original position the same way across all JVM implementations, and `rewind()` is almost never the right way to "restore" a buffer — it should be left at the position the caller expects. The correct pattern is either to slice once and operate on the slice, or to save/restore the position with `mark()`/`reset()` and never call `rewind()`.

---

## Test

The existing `CQLVectorTest` exercises round-trip vector serialization; a one-line typo fix in that test (`identitiy` → `identity`) was included in the same commit, confirming the test was already covering the scenario.
