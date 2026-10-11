# Bug 0107: Corrupted SSTable Read During Compaction Skips markSuspect and Leaks BufferPool Buffer

**Commit**: db38529ab8 · **Ticket**: CASSANDRA-20396
**Subsystem**: io/sstable, cache
**Category**: Resource leak

---

## Root Cause

Two distinct failures occurred when a `CorruptSSTableException` was thrown from within a compaction read:

1. **`SSTableIdentityIterator`**: The constructors only called `sstable.markSuspect()` when wrapping a raw `IOException` into a `CorruptSSTableException`. If a lower-level component (e.g., `ChunkCache`) already produced a `CorruptSSTableException`, that exception propagated uncaught without triggering `markSuspect()`, leaving the SSTable in an un-suspected state and allowing continued reads from a corrupt file.

2. **`ChunkCache.load`**: After allocating a `ByteBuffer` from `BufferPool`, if `key.file.readChunk(...)` threw any exception, the buffer was never returned to the pool, causing a permanent `BufferPool` leak.

The fix adds `catch (CorruptSSTableException e)` blocks to all `SSTableIdentityIterator` factory methods and to `SSTableSimpleScanner.advanceRange`, each calling `markSuspect()` before re-throwing. `ChunkCache.load` is wrapped in a try/catch that calls `bufferPool.put(buffer)` on any throwable.

### Before / After

```java
// BEFORE — ChunkCache.load leaks buffer on exception
ByteBuffer buffer = bufferPool.get(key.file.chunkSize(), key.file.preferredBufferType());
key.file.readChunk(key.position, buffer);   // exception escapes; buffer is never returned
return new Buffer(buffer, key.position);
```

```java
// AFTER
ByteBuffer buffer = bufferPool.get(key.file.chunkSize(), key.file.preferredBufferType());
try
{
    key.file.readChunk(key.position, buffer);
    return new Buffer(buffer, key.position);
}
catch (Throwable t)
{
    bufferPool.put(buffer);
    throw t;
}
```

---

## How to Spot

Any code that allocates a pooled resource (buffer, connection, file handle) and then calls another method that can throw must wrap the call in try/finally or try/catch to release the resource. Similarly, exception-handler chains that only catch `IOException` but not the domain-specific subclass that wraps it (e.g., `CorruptSSTableException`) will silently skip side-effects like `markSuspect()`. During review, scan every catch block that handles I/O exceptions and verify that domain-exception subclasses are also handled.

---

## Test

No new unit test added; the fix is structural — all `SSTableIdentityIterator` factory methods and `SSTableSimpleScanner.advanceRange` now uniformly call `markSuspect()` for any `CorruptSSTableException`.
