# Bug 1305: Potential Data Loss in CompressedSequentialWriter After resetAndTruncate (2.1 Patch)

**Commit**: 9ff82706f2 · **Ticket**: CASSANDRA-8949
**Subsystem**: io/compress/CompressedSequentialWriter
**Category**: State not cleaned up

---

## Root Cause

This is a 2.1-branch backport of the same fix as bug-1306. After `resetAndTruncate` rolls the writer back to a mark, the in-memory write buffer still contains valid bytes from before the mark. If `isDirty` is not set to `true`, the next `reBuffer` call treats the buffer as clean and discards those bytes without flushing them, silently losing the data that was just written via `resetAndTruncate`. The root cause is that the reset path restored file-level state (offsets and chunk count) but neglected to update the buffer-level `isDirty` flag that guards whether buffered bytes are flushed on the next chunk boundary transition.

### Before / After

```java
// BEFORE – isDirty not set after truncate; buffered bytes can be lost
bufferOffset = current - validBufferBytes;
chunkCount = realMark.nextChunkIndex - 1;

// truncate data and index file
truncate(chunkOffset);
metadataWriter.resetAndTruncate(realMark.nextChunkIndex - 1);
```

```java
// AFTER – mark dirty so reBuffer flushes the partial chunk
bufferOffset = current - validBufferBytes;
chunkCount = realMark.nextChunkIndex - 1;

// mark as dirty so we don't lose the bytes on subsequent reBuffer calls
isDirty = true;

// truncate data and index file
truncate(chunkOffset);
metadataWriter.resetAndTruncate(realMark.nextChunkIndex - 1);
```

---

## How to Spot

Any writer that has a mark/reset mechanism and a separate "dirty" flag needs to verify that every code path that modifies the logical write position also updates the dirty flag appropriately. A reviewer should trace all callers of `resetAndTruncate` and ask: "after this returns, are all state variables—buffer dirty flag, buffer offset, chunk counter—consistent with one another?"

---

## Test

`CompressedSequentialWriterTest` added: exercises write, mark, write-across-chunk-boundary, resetAndTruncate, then write again, confirming the full expected byte sequence survives.
