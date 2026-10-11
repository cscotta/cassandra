# Bug 1341: Off-Heap Buffer Copy Ignores arrayOffset, Corrupting HSHA and Offheap Objects

**Commit**: 7cc1cf000c · **Ticket**: CASSANDRA-8719
**Subsystem**: src/java/org/apache/cassandra/utils/memory/MemoryUtil.java
**Category**: Off-by-one

---

## Root Cause

`MemoryUtil.setBytes(long address, ByteBuffer buffer, int start, int count)` handled heap-backed `ByteBuffer`s by copying from `buffer.array()` at offset `start`. However, `ByteBuffer` slices and duplicates can have a non-zero `arrayOffset()`, so `buffer.array()[start]` is not the same byte as `buffer.get(start)`. The copy silently read from the wrong position in the backing array, corrupting any off-heap allocation populated from a sliced or offset heap buffer, which is common in the HSHA (Half-Sync/Half-Async) thread pool and offheap object paths.

### Before / After

```java
// BEFORE — arrayOffset() ignored, reads wrong bytes for sliced buffers
setBytes(address, buffer.array(), start, count);
```

```java
// AFTER — correct absolute offset in the backing array
setBytes(address, buffer.array(), buffer.arrayOffset() + start, count);
```

---

## How to Spot

Any call to `ByteBuffer.array()` that uses a separately tracked `start` offset without adding `arrayOffset()` is potentially wrong. The pattern `buffer.array(), start` should nearly always be `buffer.array(), buffer.arrayOffset() + start`. Review every such site, especially in native/unsafe memory utilities where the corruption is silent and may manifest far from the call site.

---

## Test

No test added.
