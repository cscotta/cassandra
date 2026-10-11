**Commit**: e3276ae029 · **Ticket**: KAFKA-19427

**Subsystem**: Group Coordinator / CoordinatorRuntime

**Category**: Resource leak

**Root cause**
`CoordinatorRuntime` allocated buffers for writing coordinator records using a fixed `maxBatchSize` (e.g., 1 MB) as the initial allocation, even for small updates. When many coordinators were active, this ballooned memory usage. Additionally, recycled buffers larger than `maxMessageSize` were returned to the pool unconditionally, allowing arbitrarily large buffers to accumulate. The fix introduces `INITIAL_BUFFER_SIZE` (512 KB) for the starting allocation and adds a size check before recycling to prevent pool growth.

**Before/After**
```java
// Before: always allocate maxBatchSize; always recycle
ByteBuffer buffer = ByteBuffer.allocate(maxBatchSize);
// ... after use ...
bufferPool.recycle(buffer);

// After: start small; only recycle if within size limit
ByteBuffer buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
// ... after use ...
if (buffer.capacity() <= maxMessageSize) {
    bufferPool.recycle(buffer);
}
```

**How to spot**
Buffer pools that accept returned buffers without a size cap will grow without bound if callers occasionally allocate oversized buffers. Always guard pool `recycle`/`release` calls with a maximum-size check.

**Test**
`CoordinatorRuntimeTest` verifies that buffers exceeding `maxMessageSize` are not returned to the pool and that initial allocation starts at `INITIAL_BUFFER_SIZE`.
