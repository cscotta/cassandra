**Commit**: 324a74d4fd · **Ticket**: KAFKA-17801
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
            RecordBatch firstBatch = null;

            while (firstBatch == null && rlsMetadataOptional.isPresent()) {
                firstBatch = findFirstBatch(remoteLogInputStream, offset);
                if (firstBatch == null) {
                    new LogOffsetMetadata(offset, remoteLogSegmentMetadata.startOffset(), startPos),
            Utils.closeQuietly(remoteSegInputStream, "RemoteLogSegmentInputStream");
    RecordBatch findFirstBatch(RemoteLogInputStream remoteLogInputStream, long offset) throws IOException {
        RecordBatch nextBatch;

        return nextBatch;

// After
        EnrichedRecordBatch enrichedRecordBatch = new EnrichedRecordBatch(null, 0);
            while (enrichedRecordBatch.batch == null && rlsMetadataOptional.isPresent()) {
                enrichedRecordBatch = findFirstBatch(remoteLogInputStream, offset);
                if (enrichedRecordBatch.batch == null) {
                    Utils.closeQuietly(remoteSegInputStream, "RemoteLogSegmentInputStream");
            RecordBatch firstBatch = enrichedRecordBatch.batch;
            startPos = startPos + enrichedRecordBatch.skippedBytes;
                    new LogOffsetMetadata(firstBatch.baseOffset(), remoteLogSegmentMetadata.startOffset(), startPos),
            if (enrichedRecordBatch.batch != null) {
                Utils.closeQuietly(remoteSegInputStream, "RemoteLogSegmentInputStream");
            }
    EnrichedRecordBatch findFirstBatch(RemoteLogInputStream remoteLogInputStream, long offset) throws IOException {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17801: remotelogmanager may compute inaccurate upperboundoffset for aborted txns (#17676).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
