**Commit**: 01587d09d8 · **Ticket**: KAFKA-18494
**Subsystem**: core / share / SharePartition
**Category**: State not cleaned up

**Root cause**
After reading persisted share-partition state from the persister, `SharePartition` reconstructed `cachedState` from the returned batches but did not account for gaps between consecutive batches (e.g., batches covering [0,10] and [21,30] left offsets 11–20 with no entry). `nextFetchOffset()` then returned a position inside the gap that was never fetchable, and `acquireRecords()` silently skipped those offsets. The fix introduces `InitialReadGapOffset` to track the first gap detected during initialization and drives subsequent fetches into the gap until it is filled.

**Before/After**
```java
// Before — no gap detection during init
for (PersisterStateBatch stateBatch : stateBatches) {
    InFlightBatch inFlightBatch = new InFlightBatch(...);
    cachedState.put(stateBatch.firstOffset(), inFlightBatch);
}

// After — track gap start offset
long gapStartOffset = -1;
long previousBatchLastOffset = startOffset - 1;
for (PersisterStateBatch stateBatch : stateBatches) {
    if (gapStartOffset == -1 && stateBatch.firstOffset() > previousBatchLastOffset + 1) {
        gapStartOffset = previousBatchLastOffset + 1;
    }
    previousBatchLastOffset = stateBatch.lastOffset();
    cachedState.put(stateBatch.firstOffset(), new InFlightBatch(...));
}
if (gapStartOffset != -1) {
    initialReadGapOffset = new InitialReadGapOffset(endOffset, gapStartOffset);
}
```

**How to spot**
Whenever a data structure is rebuilt from a list of persisted ranges, check that gaps between adjacent entries are tracked explicitly. Unit tests that only use contiguous batches will not reveal this; use non-contiguous input batches in test setup.

**Test**
`SharePartitionTest` — initializes a `SharePartition` with persisted batches that have a gap, then calls `nextFetchOffset()` and `acquireRecords()`; asserts that offsets inside the gap are returned for fetch and eventually acquired rather than being skipped.
