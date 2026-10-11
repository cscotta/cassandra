# Bug 1306: Potential Data Loss in CompressedSequentialWriter After resetAndTruncate

**Commit**: f5f97d8153 · **Ticket**: CASSANDRA-8949
**Subsystem**: io/compress/CompressedSequentialWriter
**Category**: State not cleaned up

---

## Root Cause

After `resetAndTruncate` rolls the writer back to a previously saved mark, the in-memory write buffer still contains valid bytes representing the partial chunk up to the mark position. However, `isDirty` was not set to `true` by the reset path. The next call to `reBuffer` (triggered when the buffer fills and must be flushed to produce the next compressed chunk) checks `isDirty` to decide whether to flush; because `isDirty` was `false`, the buffered bytes were discarded rather than compressed and written to disk, producing silent data loss. The fix adds a single `isDirty = true` statement immediately after the state variables are restored, ensuring the buffer is treated as containing unflushed data.

### Before / After

```java
// BEFORE – isDirty stays false; bytes in buffer are silently dropped
bufferOffset = current - validBufferBytes;
chunkCount = realMark.nextChunkIndex - 1;

truncate(chunkOffset);
metadataWriter.resetAndTruncate(realMark.nextChunkIndex - 1);
```

```java
// AFTER – isDirty=true prevents reBuffer from discarding the buffer
bufferOffset = current - validBufferBytes;
chunkCount = realMark.nextChunkIndex - 1;

// mark as dirty so we don't lose the bytes on subsequent reBuffer calls
isDirty = true;

truncate(chunkOffset);
metadataWriter.resetAndTruncate(realMark.nextChunkIndex - 1);
```

---

## How to Spot

Mark/reset implementations in buffered writers must keep every piece of mutable state consistent after a reset. A reviewer should enumerate all state variables involved in the write pipeline (`bufferOffset`, `validBufferBytes`, `isDirty`, `chunkCount`, etc.) and confirm that the reset path sets each one to a value consistent with the restored position. A missing update to any one of them can cause silent data corruption.

---

## Test

`CompressedSequentialWriterTest` added: writes data, marks, writes a full chunk of garbage to force a chunk transition, calls `resetAndTruncate`, writes a second payload, then reads back and verifies the concatenated expected content.
