**Commit**: 94a1bfb128 · **Ticket**: KAFKA-18575
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
There is a subtle race condition with transactions V2 if a transaction is still completing when checking if we need to add a partition, but it completes when the request reaches the coordinator. One approach was to remove the verification for TV2 and just check the epoch on write, but a simpler one is to simply return concurrent transactions from the partition leader (before attempting to add the partition). I've done this and added a test for this behavior. Locally, I reproduced the race but adding a 1 second sleep when handling the WriteTxnMarkersRequest and a 2 second delay before adding the partition to the AddPartitionsToTxnManager.

**Before/After**
```java
// Before
          if (batch.isTransactional && !hasOngoingTransaction(batch.producerId, batch.producerEpoch()) && batchMissingRequiredVerification(batch, requestVerificationGuard))
    producerStateManager.producerStateManagerConfig().transactionVerificationEnabled() && !batch.isControlBatch &&

// After
    // With transactions V2, if we see a future epoch, we are likely in the process of completing the previous transaction.
    // Return early with ConcurrentTransactionsException until the transaction completes.
    if (entry != null && entry.currentTxnFirstOffset.isPresent && entry.producerEpoch() < producerEpoch)
      throw new ConcurrentTransactionsException("The producer attempted to update a transaction " +
        "while another concurrent operation on the same transaction was ongoing.")
          if (batch.isTransactional && !batch.isControlBatch && !hasOngoingTransaction(batch.producerId, batch.producerEpoch()) && batchMissingRequiredVerification(batch, requestVerificationGuard))
    producerStateManager.producerStateManagerConfig().transactionVerificationEnabled() &&
  @Test
  def testPreviousTransactionOngoing(): Unit = {
    val producerStateManagerConfig = new ProducerStateManagerConfig(86400000, true)

    val producerId = 23L
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-18575: transaction version 2 doesn't correctly handle race condition with completing and new transaction(#18604).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
