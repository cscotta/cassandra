**Commit**: 27102b3187 · **Ticket**: KAFKA-20090
**Subsystem**: transaction-coordinator/src/main/java/org/apache/kafka/coordinator/transaction/TransactionMetadata.java
**Category**: Off-by-one

**Root cause**
When a transaction times out and the coordinator aborts it, the producer epoch is bumped by 1 for the next transaction. If the epoch was already at `Short.MaxValue - 1` (32766), the bump would push it to `Short.MaxValue` (32767). A subsequent timeout would then try to increment to 32768, which overflows a `short` to a negative value. `TransactionMetadata` previously threw an `IllegalArgumentException` at that point, permanently wedging the transactional producer. The fix changes the overflow case to log an error and allow the epoch to stay at `MAX_VALUE`, and `ProducerAppendInfo` is updated to accept transaction marker writes at `MAX_VALUE` epoch so recovery (producer ID rotation) can proceed.

**Before/After**
```java
// BEFORE – threw on epoch overflow, wedging the producer
if (newEpoch < 0)
    throw new IllegalArgumentException("Epoch " + newEpoch + " is invalid.");

// AFTER – logs error, keeps epoch at MAX_VALUE to allow recovery
if (newEpoch < 0) {
    log.error("Epoch overflow detected for producer ...");
    // epoch stays at Short.MAX_VALUE; rotation will happen on next initTransactions
}
```

**How to spot**
Any counter stored in a `short` that is incremented on each error/timeout is vulnerable to an overflow cliff. Look for epoch-increment code without a `== Short.MAX_VALUE` guard. The symptom is a permanently wedged transactional producer after enough transaction timeouts.

**Test**
`TransactionsTest.testRecoveryFromEpochOverflow`: sets epoch to `Short.MaxValue - 2` via internal state manipulation, triggers two transaction timeouts to reach overflow, and verifies that new transactions succeed with a rotated producer ID.
