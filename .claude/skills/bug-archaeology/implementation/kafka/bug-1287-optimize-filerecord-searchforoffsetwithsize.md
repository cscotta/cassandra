**Commit**: 31e1a57c41 · **Ticket**: KAFKA-18989
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
The `lastOffset` includes the entire batch header, so we should check `baseOffset` instead. To optimize this, we need to update the search logic. The previous
approach simply checked whether each batch's `lastOffset()` was greater
than or equal to the target offset. Once it found the first batch that
met this condition, it returned that batch immediately.

**Before/After**
```java
// Before
    public LogOffsetPosition searchForOffsetWithSize(long targetOffset, int startingPosition) {
            long offset = batch.lastOffset();
            if (offset >= targetOffset)
                return new LogOffsetPosition(batch.baseOffset(), batch.position(), batch.sizeInBytes());

// After
     * <p>
     * The following logic is intentionally designed to minimize memory usage
     * by avoiding unnecessary calls to {@link FileChannelRecordBatch#lastOffset()} for every batch.
     * Instead, we use {@link FileChannelRecordBatch#baseOffset()} comparisons when possible, and only
     * check {@link FileChannelRecordBatch#lastOffset()} when absolutely necessary.
    public LogOffsetPosition searchForOffsetFromPosition(long targetOffset, int startingPosition) {
        FileChannelRecordBatch prevBatch = null;

            // If baseOffset exactly equals targetOffset, return immediately
            if (batch.baseOffset() == targetOffset) {
                return LogOffsetPosition.fromBatch(batch);
            }
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18989 optimize filerecord#searchforoffsetwithsize (#19214).

**Test**
Test modified in FileRecordsTest.java covering the corrected behaviour.
