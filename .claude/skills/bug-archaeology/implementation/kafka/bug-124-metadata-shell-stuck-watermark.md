**Commit**: 17ab374bb5 · **Ticket**: KAFKA-15371
**Subsystem**: metadata / util / SnapshotFileReader
**Category**: Race condition / missing synchronization

**Root cause**
`SnapshotFileReader` exposes the high watermark only in its `ShutdownEvent.run()`, which is enqueued to its own `EventQueue` after the last batch is processed. However, `MetadataLoader` processes metadata events on a different queue. If the `MetadataLoader` drains all metadata events *before* the `ShutdownEvent` updates `highWaterMark`, every call to `maybePublishMetadata` will see the watermark as `OptionalLong.empty()` and skip publishing. The main thread then waits on a condition that is never signalled, causing the metadata shell to hang indefinitely.

**Before/After**
```java
// Before — high watermark set only in ShutdownEvent (too late)
private void handleNextBatch() {
    ...
    FileChannelRecordBatch batch = batchIterator.next();
    if (batch.isControlBatch()) { handleControlBatch(batch); }
    else { handleMetadataBatch(batch); }
    lastOffset = batch.lastOffset();
    scheduleHandleNextBatch();
}
class ShutdownEvent { void run() {
    highWaterMark = OptionalLong.of(lastOffset); // too late
    ...
}}

// After — high watermark set before dispatching the last batch's events
private void handleNextBatch() {
    FileChannelRecordBatch batch = batchIterator.next();
    lastOffset = batch.lastOffset();
    if (!batchIterator.hasNext()) {
        highWaterMark = OptionalLong.of(lastOffset); // set before metadata events
    }
    if (batch.isControlBatch()) { handleControlBatch(batch); }
    else { handleMetadataBatch(batch); }
    scheduleHandleNextBatch();
}
```

**How to spot**
Whenever a producer and consumer run on separate event queues, ordering guarantees must be established by sequencing operations within the same queue rather than relying on shutdown callbacks. If the consumer can drain the producer's output before the producer's "done" signal is delivered, the consumer will hang.

**Test**
No new unit test added; the fix is verified by running `kafka-metadata-shell.sh --snapshot` against a real snapshot file and observing that the shell prompt appears promptly.
