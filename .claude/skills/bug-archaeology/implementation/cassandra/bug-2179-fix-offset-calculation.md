# Bug 2179: CompressionMetadata Off-by-One Detects Last Chunk Using Wrong Comparison

**Commit**: de6260ddcb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionMetadata.java
**Category**: Off-by-one

---

## Root Cause

The boundary condition for the last compressed chunk was computed incorrectly. The expression `i + 8 == chunkOffsets.size()` compared a byte offset (`i * 8 + 8`) with the total byte size, but only added the fixed stride `8` to the loop variable `i` rather than to the derived byte offset. The correct check is `offset + 8 == chunkOffsets.size()` where `offset = i * 8`. As a result, the last chunk was never correctly identified: the code attempted to read a next chunk offset past the end of the buffer instead of using `compressedFileLength` as the sentinel, causing incorrect chunk size calculations for the final compressed block.

### Before / After

```java
// BEFORE
long nextChunkOffset = (i + 8 == chunkOffsets.size())
                               ? compressedFileLength
                               : chunkOffsets.getLong(offset + 8);
```

```java
// AFTER
long nextChunkOffset = offset + 8 == chunkOffsets.size()
                     ? compressedFileLength
                     : chunkOffsets.getLong(offset + 8);
```

---

## How to Spot

When iterating over a byte buffer using a scaled loop index (`offset = i * stride`), boundary checks must use the derived `offset` variable, not the raw loop counter. A mismatch between the scale used in the index calculation and the scale used in the boundary check is a classic off-by-one.

---

## Test

No test added.
