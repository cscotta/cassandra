# Bug 018: Fix memory leak in BufferPoolAllocator when a capacity needs to be extended

**Commit**: 3f079b2345 · **Ticket**: CASSANDRA-20753
**Subsystem**: Networking / Buffer Pool
**Category**: Resource leak

---

## Root Cause

`BufferPoolAllocator`'s inner `AdoptedByteBuf.capacity(int)` method extended capacity by allocating a new `ByteBuf` via `super.capacity(newCapacity)` and then called `nioBuffer()` on it to get the underlying `ByteBuffer`. The `nioBuffer()` call on a Netty buffer can return a *sliced* view — a new `ByteBuffer` object wrapping the same memory — rather than the original buffer registered with the `BufferPool`. When the old `wrapped` reference was subsequently returned to `BufferPool.put(wrapped)`, the pool received the pre-extension buffer correctly, but `wrapped` was then set to the *slice*, not to the pool-tracked buffer. On the next `put()` or `release()`, the slice was passed back to the pool, which could not find a matching chunk for it and therefore leaked the underlying off-heap memory.

### Before / After

```java
// BEFORE — nioBuffer() may return a slice; pool cannot reclaim the slice
ByteBuf newBuffer = super.capacity(newCapacity);
ByteBuffer nioBuffer = newBuffer.nioBuffer(0, newBuffer.capacity());

bufferPool.put(wrapped);
wrapped = nioBuffer;  // nioBuffer may not be the chunk-managed buffer

// AFTER — unwrap to the pool-managed buffer before storing
ByteBuf newBuffer = super.capacity(newCapacity);
ByteBuffer nioBuffer = newBuffer.nioBuffer(0, newBuffer.capacity());
nioBuffer = bufferPool.unwrapBufferPoolManagedBuffer(nioBuffer);  // resolve the slice

bufferPool.put(wrapped);
wrapped = nioBuffer;
```

---

## How to Spot

Whenever a Netty `ByteBuf.nioBuffer()` result is stored as a long-lived handle that will later be returned to an off-heap pool, verify that the returned `ByteBuffer` is the pool-tracked object and not a derived view. Slicing, duplicating, or compositing operations on Netty buffers produce new `ByteBuffer` objects whose `attachment` chain does not directly point to the pool's `Chunk`; passing such objects to `BufferPool.put()` causes a silent no-op release and a permanent off-heap leak. The fix is to walk the attachment chain (as `unwrapBufferPoolManagedBuffer` does) to recover the original chunk-managed buffer before storing the reference.

---

## Test

`BufferPoolAllocatorTest.testAdoptedBufferContentAfterResize` allocates a `ByteBuf`, writes past initial capacity to trigger a resize, then verifies both `usedSizeInBytes()` and `overflowMemoryInBytes()` return zero after releasing, confirming no off-heap memory is leaked across a capacity extension cycle.
