# Bug 1263: Compressed Reader Regression — No Buffer Pooling and Swapped allocate/allocateDirect Flags

**Commit**: aedce5fc6b · **Ticket**: CASSANDRA-9240
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressedRandomAccessReader.java, src/java/org/apache/cassandra/io/util/CompressedSegmentedFile.java, src/java/org/apache/cassandra/io/util/RandomAccessReader.java
**Category**: Resource leak

---

## Root Cause

A previous refactoring introduced two related regressions in compressed SSTable reading. First, `CompressedRandomAccessReader` was constructed with a `null` owner, bypassing the `PoolingSegmentedFile` mechanism entirely; every reader allocated fresh buffers and never returned them to a pool, causing excessive GC pressure and off-heap memory churn through repeated `mmap`/`unmap` cycles. Second, `RandomAccessReader.allocateBuffer` had its `useDirectBuffer` branch condition inverted — passing `true` allocated a heap buffer via `ByteBuffer.allocate` while `false` allocated a direct buffer via `ByteBuffer.allocateDirect`, the exact opposite of correct behaviour. The fix moves mmap segment lifecycle ownership into `CompressedSegmentedFile` / `CompressedPoolingSegmentedFile`, passes the owning file object through to the reader so pooling is restored, and corrects the `allocate`/`allocateDirect` swap.

### Before / After

```java
// BEFORE — allocate/allocateDirect swapped
return useDirectBuffer
        ? ByteBuffer.allocate(size)       // wrong: heap buffer for "direct" path
        : ByteBuffer.allocateDirect(size); // wrong: direct buffer for "heap" path
```

```java
// AFTER — correct mapping
return useDirectBuffer
        ? ByteBuffer.allocateDirect(size)
        : ByteBuffer.allocate(size);
```

```java
// BEFORE — reader opened without owner, no pooling
public static CompressedRandomAccessReader open(ChannelProxy channel, CompressionMetadata metadata)
{
    return open(channel, metadata, null);  // null owner skips pool
}
```

```java
// AFTER — reader receives owning file, pooling restored
public static CompressedRandomAccessReader open(ICompressedFile file)
{
    return new CompressedRandomAccessReader(file.channel(), file.getMetadata(), file);
}
```

---

## How to Spot

When refactoring resource-pooled readers, verify that every construction path passes a non-null owner to the pool mechanism; passing `null` is a common shortcut that silently disables pooling and is easy to miss in review. Additionally, boolean-flag methods like `useDirectBuffer` that gate `allocate` vs `allocateDirect` are high-risk for inversion bugs — the two methods look similar and are easy to transpose. A reviewer should confirm that the semantics of the flag name match the branch taken.

---

## Test

No test added.
