**Commit**: 0b2e410d61 · **Ticket**: KAFKA-19367
**Subsystem**: core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala
**Category**: Logic error in condition

**Root cause**: When `InitProducerId` was called while a transaction was ongoing, the coordinator bumped the producer epoch (to fence stale requests) and then, for transaction version 2 (KIP-890 Part 2), triggered an abort via `prepareAbortOrCommit`. Under TV2 the abort itself performs a second epoch increment as part of the new protocol, resulting in two increments. In the worst case, starting from epoch 32766 (`Short.MaxValue - 1`), the first bump reached 32767 and the second overflowed to a negative value, causing an `IllegalArgumentException`. The fix removes the manual epoch assignment inside the `PREPARE_ABORT + isEpochFence` branch, letting `prepareAbortOrCommit` handle the single authoritative increment.

**Before/After**

```scala
// Before – manual epoch set + subsequent abort both increment
if (nextState == TransactionState.PREPARE_ABORT && isEpochFence) {
    txnMetadata.pendingState = None
    txnMetadata.producerEpoch = producerEpoch          // first increment stored
    txnMetadata.lastProducerEpoch = RecordBatch.NO_PRODUCER_EPOCH
}
// prepareAbortOrCommit then increments again under TV2

// After – let prepareAbortOrCommit be the single incrementor
if (nextState == TransactionState.PREPARE_ABORT && isEpochFence) {
    txnMetadata.pendingState = None
    // For TV2+, don't manually set the epoch.
}
```

**How to spot**: Look for places where the same mutable field is updated in two separate code paths that are both executed in one logical operation. Short-circuit epoch arithmetic is especially risky near `Short.MaxValue`.

**Test**: Sets up `TransactionMetadata` at epoch 32766 with state `ONGOING` and TV2, calls `handleInitProducerId`, and asserts the final epoch is exactly `Short.MaxValue` (32767) with state `PREPARE_ABORT`, confirming no overflow occurs.
