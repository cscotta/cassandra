# Bug 0484: Chunk Index Integer Overflow for Large SSTables with Small Chunk Length

**Commit**: 4886968c4e · **Ticket**: CASSANDRA-15595
**Subsystem**: `src/java/org/apache/cassandra/io/compress/CompressionMetadata.java`
**Category**: Off-by-one

---

## Root Cause

In `CompressionMetadata.chunkFor()`, the chunk index was computed as `int idx = 8 * (int)(position / parameters.chunkLength())`. When an SSTable is large and the chunk length is small, the expression `position / chunkLength` can produce a value that exceeds `Integer.MAX_VALUE` before the multiply by 8, causing silent integer overflow and a silently wrong (possibly negative or wrapping) index. The cast to `int` was applied too early, discarding the high bits of a value that needed to remain a `long`.

### Before / After

```java
// BEFORE — overflow when position/chunkLength > Integer.MAX_VALUE / 8
int idx = 8 * (int) (position / parameters.chunkLength());
```

```java
// AFTER — full long arithmetic; also detect unexpected negatives
long idx = 8 * (position / parameters.chunkLength());
if (idx >= chunkOffsetsSize)
    throw new CorruptSSTableException(new EOFException(), indexFilePath);
if (idx < 0)
    throw new CorruptSSTableException(
        new IllegalArgumentException(...), indexFilePath);
```

---

## How to Spot

Whenever an array or buffer offset is computed by dividing a file position (which can be multi-gigabyte) by a block/chunk size and then multiplying, keep all intermediate values as `long`. An early `(int)` cast is safe only if the result is guaranteed to fit in 32 bits; for positions in an arbitrarily large SSTable that guarantee does not hold. Grep for `(int) (position /` or similar patterns in I/O and compression code.

---

## Test

`CompressedRandomAccessReaderTest` was extended to cover reading from a large simulated SSTable where the chunk index exceeds `Integer.MAX_VALUE`.
