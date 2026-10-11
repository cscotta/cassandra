**Commit**: ccab9eb8b4 · **Ticket**: KAFKA-18660
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Fixed the typo that used the wrong producer ID and epoch when returning so that we handle epoch overflow correctly. We also had to rearrange the concurrent transaction handling so that we don't self-fence when we start the new transaction with the new producer ID. I also tested this with a modified version of the code where epoch overflow happens on the first epoch bump (every request has a new producer id)

Reviewers: Artem Livshits <alivshits@confluent.io>, Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
            if (txnMetadata.producerId != producerId) {
            } else if (txnMetadata.pendingTransitionInProgress) {
              // return a retriable exception to let the client backoff and retry
              Left(Errors.CONCURRENT_TRANSACTIONS)
            if (txnMetadata.producerId != producerId && !retryOnOverflow)
              Left(Errors.INVALID_PRODUCER_ID_MAPPING)
            else if (txnMetadata.pendingTransitionInProgress && txnMetadata.pendingState.get != PrepareEpochFence)
                  responseCallback(Errors.NONE, txnMetadata.producerId, txnMetadata.producerEpoch)
import org.apache.kafka.common.errors.{InvalidProducerEpochException, ProducerFencedException, TimeoutException}
    try {
      // Now that the transaction has expired, the second send should fail with a InvalidProducerEpochException.
      producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false)).get()

// After
            if (txnMetadata.pendingTransitionInProgress) {
              // return a retriable exception to let the client backoff and retry
              // This check is performed first so that the pending transition can complete before subsequent checks.
              // With TV2, we may be transitioning over a producer epoch overflow, and the producer may be using the
              // new producer ID that is still only in pending state.
              Left(Errors.CONCURRENT_TRANSACTIONS)
            } else if (txnMetadata.producerId != producerId) {
            if (txnMetadata.pendingTransitionInProgress && txnMetadata.pendingState.get != PrepareEpochFence) {
              // This check is performed first so that the pending transition can complete before the next checks.
              // With TV2, we may be transitioning over a producer epoch overflow, and the producer may be using the
              // new producer ID that is still only in pending state.
            } else if (txnMetadata.producerId != producerId && !retryOnOverflow)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18660: transactions version 2 doesn't handle epoch overflow correctly (#18730).

**Test**
Test modified in TransactionsTest.scala covering the corrected behaviour.
