# Bug 0595: Compaction Fails Due to Stale lastFlushOffset After resetAndTruncate

**Commit**: 3a713827f4 · **Ticket**: CASSANDRA-12743
**Subsystem**: `org.apache.cassandra.io.compress`, `org.apache.cassandra.io.util`
**Category**: State not cleaned up

---

## Root Cause

`CompressedSequentialWriter` tracks the number of uncompressed bytes that have been physically flushed to disk via `lastFlushOffset`. Two separate code paths failed to keep this field consistent after truncation.

In `flushData()`, `lastFlushOffset` was incremented by `compressedLength + 4` (the compressed chunk size plus CRC), but the field is meant to track the *uncompressed* position; using the compressed size caused the offset to diverge from actual file content after any flush.

In `resetAndTruncate()`, the inherited `truncate(chunkOffset)` call truncated the file but never updated `lastFlushOffset`, so preemptive-open logic that uses `lastFlushOffset` to determine how much of the file is safe to read would see a stale, over-large value and attempt to read data that no longer existed.

### Before / After

```java
// BEFORE – flushData()
lastFlushOffset += compressedLength + 4;

// BEFORE – resetAndTruncate()
truncate(chunkOffset);
```

```java
// AFTER – flushData()
lastFlushOffset = uncompressedSize;

// AFTER – resetAndTruncate()
truncate(chunkOffset, bufferOffset);   // new overload resets lastFlushOffset

// New helper
private void truncate(long toFileSize, long toBufferOffset) {
    channel.truncate(toFileSize);
    lastFlushOffset = toBufferOffset;
}
```

Additionally, `SequentialWriter.truncate()` now also sets `lastFlushOffset = toSize` and `bufferOffset = truncateTarget` to keep the base class consistent.

---

## How to Spot

Whenever a writer class maintains multiple fields that together describe "how much data is durably on disk" (e.g., a file pointer and a flush-offset field), every code path that truncates or resets the file must update all of those fields atomically. Reviewing truncation methods for missing companion field updates — especially in subclass overrides where only one field was reset — catches this pattern.

---

## Test

`CompressedSequentialWriterReopenTest.compressionEnabled` exercises preemptive-open during a major compaction with a deliberately expansion-heavy compressor, asserting the compaction completes without error. `CompressedSequentialWriterTest.resetAndTruncateTest` verifies `lastFlushOffset` tracks correctly across a write/mark/write/reset/write cycle.
