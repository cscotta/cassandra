**Commit**: 2dd2db7a1e · **Ticket**: KAFKA-8350
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java
**Category**: Logic error in condition

**Root cause**: `RecordAccumulator.splitAndReenqueue()` always used the configured `batchSize` as the target size when splitting an oversized batch. When `batch.size` (e.g., 8 MB) was much larger than the topic's `message.max.bytes` (e.g., 1 MB), every split still produced batches larger than the broker would accept. The broker returned `MESSAGE_TOO_LARGE`, the client re-split with the same `batchSize`, and the cycle repeated indefinitely, ultimately causing a stack overflow in `FutureRecordMetadata.chain()`. The fix detects when a batch is already a split batch and switches to a progressively smaller target: `max(maxRecordSize, estimatedSizeInBytes / 2)`.

**Before/After**:
```java
// BEFORE – always uses original batchSize; infinite loop when batchSize > message.max.bytes
Deque<ProducerBatch> dq = bigBatch.split(this.batchSize);

// AFTER – progressive splitting for already-split batches
int targetSplitBatchSize = this.batchSize;
if (bigBatch.isSplitBatch()) {
    targetSplitBatchSize = Math.max(bigBatch.maxRecordSize,
                                    bigBatch.estimatedSizeInBytes() / 2);
}
Deque<ProducerBatch> dq = bigBatch.split(targetSplitBatchSize);
```

**How to spot**: Recursive or iterative split/retry loops need a termination guarantee. If the split target size is a constant (the original `batchSize`), the loop terminates only when `batchSize ≤ message.max.bytes`. Search for split logic that does not reduce the target size on re-entry.

**Test**: `RecordAccumulatorTest.testSplitAndReenqueuePreventInfiniteRecursion` creates a 1 MB batch with 100 × 1 KB records, verifies that iterative splitting reaches single-record batches within 100 operations (safety limit), and confirms no records are lost or duplicated.
