# Bug 0517: Partial Compressed SSTable Streaming Reads Wrong Chunks and Reports Wrong Progress

**Commit**: 602a5eef17 · **Ticket**: CASSANDRA-13938
**Subsystem**: `db/streaming/CompressedInputStream.java`, `db/streaming/CassandraCompressedStreamWriter.java`
**Category**: Logic error in condition

---

## Root Cause

The original `CompressedInputStream` used a background reader thread that pre-fetched all compressed chunks into a blocking queue, then decompressed them on demand. When only a partial set of chunks was being streamed (as happens in partial-range streaming), the decompressor kept its own absolute-position bookkeeping (`bufferOffset`/`current`) that went stale whenever `position()` was called to skip forward to the next section: the skip call would try to fast-forward by consuming buffered chunks, but `bufferOffset` was only updated relative to the decompressed buffer — not aligned to the underlying chunk boundaries. This caused position mismatches, checksum failures, and incorrect data being delivered to the deserializer. Additionally, `CassandraCompressedStreamReader` used `getTotalCompressedBytesRead()` which accumulated the total over the entire stream rather than the bytes sent for the current progress window, producing incorrect streaming progress reports. The fix rewrites `CompressedInputStream` to remove the background thread entirely, drive chunk loading synchronously, track position correctly as an uncompressed chunk offset aligned to chunk boundaries, and rename the metric to `chunkBytesRead()` to reflect actual semantics.

### Before / After

```java
// BEFORE – background thread + blocking queue; position tracking drifts
public void position(long position) throws IOException {
    assert position >= current : "stream can only read forward.";
    current = position;
    if (current > bufferOffset + buffer.limit())
        reBuffer(false);
    buffer.position((int)(current - bufferOffset));
}
// progress reporting used total bytes across all sections
session.progress(filename, ProgressInfo.Direction.IN,
    cis.getTotalCompressedBytesRead(), totalSize);
```

```java
// AFTER – synchronous chunk loading; position aligned to chunk boundaries
public void position(long position) throws IOException {
    if (position >= uncompressedChunkPosition + buffer.limit()) {
        loadNextChunk();
        uncompressedChunkPosition = position & -compressionParams.chunkLength();
    }
    buffer.position(Ints.checkedCast(position - uncompressedChunkPosition));
}
// progress reporting uses per-section chunk bytes
session.progress(filename, ProgressInfo.Direction.IN,
    cis.chunkBytesRead(), totalSize);
```

---

## How to Spot

Whenever a streaming abstraction maintains independent position state in both a "logical" (uncompressed) and a "physical" (compressed) layer, reviewers should verify that skipping forward in the logical layer is correctly reflected in the physical layer. Off-by-one or stale-state bugs are especially likely when a background producer thread and a consumer thread share mutable position variables without explicit synchronization, or when the "skip" code path is exercised only for partial transfers and not end-to-end tests.

---

## Test

An existing distributed repair test (`RepairTest`) was extended to cover partial compressed sstable streaming scenarios, asserting that streaming completes successfully and data is consistent after the transfer.
