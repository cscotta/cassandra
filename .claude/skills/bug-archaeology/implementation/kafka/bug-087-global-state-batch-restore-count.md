**Commit**: eeb1214ba8 · **Ticket**: KAFKA-18962
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/GlobalStateManagerImpl.java
**Category**: Logic error in condition

**Root cause**: `GlobalStateManagerImpl` maintains a `restoreCount` that accumulates across all batches for a given partition/store, and a separate `numRestored` that should represent only the records restored in the *current* batch. When calling `stateRestoreListener.onBatchRestored(...)`, the code passed `restoreCount` (the running total) instead of a per-batch counter. The `StateRestoreListener` contract specifies `numRestored` as the batch size, so listeners received an ever-growing total, making it impossible to compute per-batch progress. The fix introduces a `batchRestoreCount` variable reset to 0 at the start of each batch.

**Before/After**

```java
// Before – total count passed as per-batch count
restoreCount++;
...
stateRestoreListener.onBatchRestored(topicPartition, storeName, offset, restoreCount);

// After – separate per-batch counter
batchRestoreCount = 0;
for (...) {
    restoreCount++;
    batchRestoreCount++;
}
stateRestoreListener.onBatchRestored(topicPartition, storeName, offset, batchRestoreCount);
```

**How to spot**: Whenever both a running total and a per-iteration count are needed, introduce two variables. Passing the accumulated total to an API that expects a per-batch count is a common variable-reuse mistake.

**Test**: `shouldListenForRestoreEventsWhenReprocessing` — verifies that the count passed to `onBatchRestored` reflects only the records in the current batch, not the cumulative total.
