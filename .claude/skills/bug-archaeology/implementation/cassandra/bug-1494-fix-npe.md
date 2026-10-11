# Bug 1494: Fix NPE in FileCacheService.sizeInBytes

**Commit**: ad6ba3d243 · **Ticket**: CASSANDRA-7756
**Subsystem**: src/java/org/apache/cassandra/io/util
**Category**: Missing null / bounds check

---

## Root Cause

`RandomAccessReader.getTotalBufferSize()` read `buffer.length` directly. The `buffer` field is set to `null` when the reader is closed, and `FileCacheService.sizeInBytes()` iterates all cached readers summing their buffer sizes. Under concurrent access a reader could be closed (nullifying `buffer`) between the time it was retrieved from the cache and the time `getTotalBufferSize()` was called, producing a `NullPointerException`. The fix captures `buffer` in a local reference first and returns 0 if that reference is null.

### Before / After

```java
// BEFORE
public int getTotalBufferSize()
{
    return buffer.length;  // NPE if reader was closed concurrently
}
```

```java
// AFTER
public int getTotalBufferSize()
{
    byte[] ref = buffer;
    return ref != null ? ref.length : 0;
}
```

---

## How to Spot

Whenever a field can be nulled by a concurrent close/cleanup method, reading the field twice (once to check null, once to dereference) creates a TOCTOU window. Always capture the field in a local variable first and operate on the local, even when the null check and use appear adjacent.

---

## Test

`BufferedRandomAccessFileTest.testFileCacheService` added: 40 threads concurrently call `FileCacheService.sizeInBytes()` in a loop while readers are being cached and evicted, asserting no exception is thrown.
