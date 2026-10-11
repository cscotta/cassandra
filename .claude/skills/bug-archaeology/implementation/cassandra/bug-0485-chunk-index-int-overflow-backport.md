# Bug 0485: Chunk Index Integer Overflow for Large SSTables with Small Chunk Length (Backport)

**Commit**: 04be7366fc · **Ticket**: CASSANDRA-15595
**Subsystem**: `src/java/org/apache/cassandra/io/compress/CompressionMetadata.java`
**Category**: Off-by-one

---

## Root Cause

This is a backport of the same integer-overflow fix applied in commit 4886968c4e (bug-0484). In `CompressionMetadata.chunkFor()`, the chunk index was computed as `int idx = 8 * (int)(position / parameters.chunkLength())`. For a large SSTable with a small compression chunk length the quotient `position / chunkLength` can exceed `Integer.MAX_VALUE / 8`, causing the subsequent cast to `int` to silently overflow. The result is a wrong, potentially negative, byte offset into the chunk-offsets buffer — producing silent data corruption or an incorrect `CorruptSSTableException` for the wrong reason.

### Before / After

```java
// BEFORE — overflow when position/chunkLength > Integer.MAX_VALUE / 8
int idx = 8 * (int) (position / parameters.chunkLength());
```

```java
// AFTER — keep index as long; guard against both overflow and negative
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
