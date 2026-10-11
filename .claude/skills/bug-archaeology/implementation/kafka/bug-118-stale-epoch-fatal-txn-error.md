**Commit**: 0a483618b9 · **Ticket**: KAFKA-19690
**Subsystem**: storage / log / UnifiedLog (ProducerStateManager)
**Category**: Logic error in condition

**Root cause**
`UnifiedLog.analyzeAndValidateProducerState()` checked `!hasOngoingTransaction(producerId, epoch)` and then immediately checked `batchMissingRequiredVerification()`. If the transaction coordinator had aborted the transaction and bumped the producer epoch (KIP-890 / KIP-691), the client could still hold a stale epoch. The first check passes (no ongoing transaction at the new epoch) and the second throws `InvalidTxnStateException` — a fatal error in Kafka Streams. The fix inserts an epoch comparison before the verification check: if the batch's epoch is lower than the last seen epoch, the recoverable `InvalidProducerEpochException` is thrown instead.

**Before/After**
```java
// Before — verification guard checked without epoch comparison
if (batch.isTransactional()
    && !hasOngoingTransaction(batch.producerId(), batch.producerEpoch())
    && batchMissingRequiredVerification(batch, requestVerificationGuard)) {
    throw new InvalidTxnStateException("Record was not part of an ongoing transaction");
}

// After — epoch checked first; fatal error avoided for stale-epoch case
if (batch.isTransactional() && !hasOngoingTransaction(batch.producerId(), batch.producerEpoch())) {
    ProducerStateEntry entry = producerStateManager.activeProducers().get(batch.producerId());
    if (entry != null && batch.producerEpoch() < entry.producerEpoch()) {
        throw new InvalidProducerEpochException("Epoch ... is smaller than the last seen epoch ...");
    }
    if (batchMissingRequiredVerification(batch, requestVerificationGuard)) {
        throw new InvalidTxnStateException("Record was not part of an ongoing transaction");
    }
}
```

**How to spot**
In transaction state machines, conditions should be evaluated in order from most-recoverable to least-recoverable. A stale epoch is always recoverable; a missing verification guard is a real state violation. Checking them in the wrong order escalates a recoverable problem to a fatal one.

**Test**
`UnifiedLogTest.testStaleProducerEpochReturnsRecoverableErrorForTV1Clients` / `...TV2Clients` — simulates a coordinator epoch bump, then appends records with the old epoch; asserts `InvalidProducerEpochException` (not `InvalidTxnStateException`) is thrown.
