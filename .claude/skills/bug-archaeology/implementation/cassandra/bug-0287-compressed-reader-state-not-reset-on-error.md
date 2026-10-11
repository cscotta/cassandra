# Bug 0287: Missing Buffer State Reset on CompressedRandomAccessReader Read Errors

**Commit**: 299bfaed35 · **Ticket**: CASSANDRA-17314
**Subsystem**: io/compress (CompressedRandomAccessReader), utils/ChecksumType
**Category**: State not cleaned up

---

## Root Cause

`CompressedRandomAccessReader.readChunk()` threw `CorruptSSTableException` on a checksum mismatch, but the reader's internal `buffer` was not reset before the exception propagated. On the next read attempt the reader could see stale data from the previous (corrupt) chunk because `buffer.position` and `buffer.limit` still pointed into old content. Additionally, the checksum was computed by mutating a shared `Checksum` instance and then calling `checksum.reset()` manually afterwards; if the exception path was taken before `reset()` was called, subsequent CRC computations on the same `Checksum` object would be based on accumulated state from the failed read. The fix resets the buffer to an empty view (`position(0).limit(0)`) in the `catch (CorruptBlockException)` handler and introduces a thread-local `ChecksumType.of()` helper that encapsulates creation, reset, and value retrieval atomically.

### Before / After

```java
// BEFORE — readChunk, on CorruptBlockException
catch (CorruptBlockException e)
{
    throw new CorruptSSTableException(e, getPath());
    // buffer left with stale position/limit
}
```

```java
// AFTER — readChunk, on CorruptBlockException
catch (CorruptBlockException e)
{
    // Make sure reader does not see stale data.
    buffer.position(0).limit(0);
    throw new CorruptSSTableException(e, getPath());
}
```

---

## How to Spot

Whenever a method throws before resetting shared mutable state (buffers, checksums, iterators), subsequent callers may observe leftover values. During review, trace every exception path out of a read/write method and check that all mutable objects shared across calls are left in a safe, well-defined state. Thread-local `Checksum` instances are especially risky because their accumulated value persists across calls if `reset()` is not called unconditionally.

---

## Test

No dedicated test was added; the fix is covered by correctness assertions in the existing `CompressedRandomAccessReaderTest`.
