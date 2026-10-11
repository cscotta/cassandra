**Commit**: f0488ebbd4 · **Ticket**: KAFKA-19999
**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java
**Category**: Logic error in condition

**Root cause**
With Transaction Version 2, `ProducerAppendInfo.checkProducerEpoch` used a strict `<=` inequality (`producerEpoch <= current`) to detect invalid epochs. This meant that a COMMIT/ABORT marker retried with the same epoch as a previously-completed transaction (e.g., after coordinator recovery or a lost network response) was rejected with `InvalidProducerEpochException`, leaving the transaction permanently stuck in `PREPARE` state and causing clients to fail with `CONCURRENT_TRANSACTIONS`. The fix detects idempotent marker retries: when TV2 is active, `markerEpoch == currentEpoch`, and no transaction is ongoing (`currentTxnFirstOffset.isEmpty()`), the duplicate marker is allowed through.

**Before/After**
```java
// Before – no exception for TV2 idempotent retry; falls through to throw
boolean invalidEpoch = (transactionVersion >= 2)
    ? (producerEpoch <= current) : (producerEpoch < current);
if (invalidEpoch) { throw InvalidProducerEpochException; }

// After – detect and skip idempotent marker retry before throwing
if (transactionVersion >= 2 &&
        producerEpoch == current &&
        updatedEntry.currentTxnFirstOffset().isEmpty()) {
    log.info("Idempotent transaction marker retry ...");
    return;
}
```

**How to spot**
Any place that rejects a request purely on epoch inequality without considering whether the transaction has already ended can cause a livelock. The invariant to check: if the transaction state says "already committed", a repeated commit marker with the same epoch is safe.

**Test**
`ProducerStateManagerTest#testIdempotentTransactionMarkerRetryTV2`: commits a transaction with `markerEpoch = epoch+1`, then replays the same marker a second time and asserts no exception and unchanged state.
