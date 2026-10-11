# Bug 0060: Corrupted SSTable Read During Compaction Leaks BufferPool Buffer and Skips markSuspect

**Commit**: 17cb89208c · **Ticket**: CASSANDRA-20396
**Subsystem**: io/sstable, cache
**Category**: Resource leak

---

## Root Cause

Two related defects existed in the SSTable I/O path during compaction. First, `ChunkCache` allocated a `ByteBuffer` from `BufferPool` before calling `key.file.readChunk()`; if `readChunk` threw any exception the buffer was never returned to the pool, leaking off-heap memory. Second, multiple `SSTableIdentityIterator.create` methods and `SSTableSimpleScanner.advanceRange` caught `IOException` / `IOError` and called `sstable.markSuspect()`, but they did not have a catch block for `CorruptSSTableException` (which is thrown by lower-level helpers that already detected corruption). So if a `CorruptSSTableException` propagated from a deeper layer it bypassed `markSuspect()` and left the SSTable in an undetected corrupt state for future reads.

### Before / After

```java
// BEFORE – ChunkCache: buffer leak on exception
key.file.readChunk(key.position, buffer);
return new Buffer(buffer, key.position);
```

```java
// AFTER – buffer released on any exception
try {
    key.file.readChunk(key.position, buffer);
    return new Buffer(buffer, key.position);
} catch (Throwable t) {
    bufferPool.put(buffer);
    throw t;
}
```

```java
// BEFORE – SSTableIdentityIterator: CorruptSSTableException silently not marked
catch (IOException e) {
    sstable.markSuspect();
    throw new CorruptSSTableException(e, file.getPath());
}
```

```java
// AFTER – CorruptSSTableException also calls markSuspect
catch (CorruptSSTableException e) {
    sstable.markSuspect();
    throw e;
}
```

---

## How to Spot

When a try-block allocates a pooled or closeable resource and the single-exception catch re-throws as a different exception type, exceptions thrown as the target type from callees will escape the catch entirely and bypass cleanup. Always add a broad `catch (Throwable)` (or a parallel catch for each expected exception type) to guard resource release, and add a separate catch for re-thrown checked exception subclasses so that `markSuspect`/cleanup is not skipped by exception-type promotion.

---

## Test

No dedicated new test added; the fix is covered by existing compaction and corruption-detection tests.
