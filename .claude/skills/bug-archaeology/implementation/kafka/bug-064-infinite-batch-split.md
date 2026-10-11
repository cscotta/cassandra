**Commit**: 2dd2db7a1e · **Ticket**: KAFKA-8350
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java
**Category**: Logic error in condition

**Root cause**
`RecordAccumulator.splitAndReenqueue()` always split large batches using the original configured `batchSize` regardless of whether the batch had already been split before. When `batch.size` (e.g. 8 MB) was much larger than the broker's `message.max.bytes` (e.g. 1 MB), the first split produced sub-batches that were still too large and were split again — always at the same size. This caused an infinite retry loop and, through the `FutureRecordMetadata.chain()` recursion, a stack overflow.

**Before/After**
```java
// Before: always splits at the configured batchSize
Deque<ProducerBatch> dq = bigBatch.split(this.batchSize);

// After: for already-split batches, use geometric reduction
int targetSplitBatchSize = this.batchSize;
if (bigBatch.isSplitBatch()) {
    targetSplitBatchSize = Math.max(bigBatch.maxRecordSize,
                                    bigBatch.estimatedSizeInBytes() / 2);
}
Deque<ProducerBatch> dq = bigBatch.split(targetSplitBatchSize);
```

**How to spot**
Any retry loop that invokes an operation with the same parameters on a problem that was not solved the first time will loop forever. When a split does not reduce the batch below the limit, the same input is fed back, guaranteeing infinite repetition. Progressive/geometric reduction — halving the size each round — ensures convergence.

**Test**
`testSplitAndReenqueuePreventInfiniteRecursion` creates a 100-record oversized batch and repeatedly splits it, verifying the process terminates within 100 operations, produces no duplicates or data loss, and can reach single-record batch size.
