# Bug 0443: Memory Leak in CompressedChunkReader via Unreleased ThreadLocal ByteBuffers

**Commit**: 3f73c16d50 · **Ticket**: CASSANDRA-15880
**Subsystem**: src/java/org/apache/cassandra/io/util/CompressedChunkReader.java, SimpleCachedBufferPool.java, ThreadLocalByteBufferHolder.java
**Category**: Resource leak

---

## Root Cause

`CompressedChunkReader.Standard` used a `ThreadLocal<ByteBuffer>` to hold per-thread compressed-data scratch buffers. When the buffer needed to grow beyond its current capacity, a new (potentially off-heap) buffer was allocated and stored in the thread-local, but the old buffer was never explicitly freed — relying on GC to reclaim off-heap memory. Because off-heap buffers are not tracked by GC heap pressure, they could accumulate indefinitely across reader instances, causing a native-memory leak. A parallel problem existed in the commit-log buffer pool: `SimpleCachedBufferPool` set the preferred buffer type lazily via `setPreferredReusableBufferType()` called from each new segment constructor, introducing a time-of-use vs. time-of-configuration race and allowing mismatched buffer types to escape into the pool.

### Before / After

```java
// BEFORE — leaked old buffer on resize
if (compressed.capacity() < length) {
    compressed = allocateBuffer(length);  // old buffer never cleaned
    compressedHolder.set(compressed);
}
```

```java
// AFTER — ThreadLocalByteBufferHolder cleans the old buffer on resize
public ByteBuffer getBuffer(int size) {
    ByteBuffer buffer = reusableBB.get();
    if (buffer.capacity() < size) {
        FileUtils.clean(buffer);          // explicitly free off-heap memory
        buffer = bufferType.allocate(size);
        reusableBB.set(buffer);
    }
    buffer.clear().limit(size);
    return buffer;
}
```

---

## How to Spot

Anywhere a `ThreadLocal<ByteBuffer>` is used for off-heap scratch space, look for capacity-check-and-replace patterns where the old buffer is silently discarded. Off-heap `ByteBuffer` objects must be explicitly cleaned (via `FileUtils.clean()` or `Cleaner`) whenever they are replaced, because the JVM will not reclaim direct memory until GC pressure is high enough to finalize the old reference — which may be too late or never occur under steady load.

---

## Test

No dedicated unit test added; the fix was validated by the existing commit-log and compaction read test suites plus manual memory profiling.
