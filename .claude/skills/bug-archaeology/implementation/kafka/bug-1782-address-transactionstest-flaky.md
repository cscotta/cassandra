**Commit**: e099fce567 · **Ticket**: KAFKA-18035
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
A lot of these tests assumed that the commit/abort happened immediately. Spoiler alert -- it does not. For some I ensure that the first send of the next transaction is successful before grabbing the epoch. I also loosened some checks since we don't need to guarantee the exact epoch.

**Before/After**
```java
// Before
  @Flaky("KAFKA-18035,KAFKA-18036")
      producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false))
  @Flaky("KAFKA-18092")
      // Get producerId and epoch after first commit
      var previousProducerEpoch = producerStateEntry.producerEpoch

      Thread.sleep(3000) // Wait for the markers to be persisted and the transaction state to be updated.

      // Second transaction: abort
      producer.beginTransaction()
      producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false))
      // Get producer epoch after abortTransaction and verify it has increased.

// After
      val successfulFuture = producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false))
      successfulFuture.get(20, TimeUnit.SECONDS)
      // Second transaction: abort
      producer.beginTransaction()
      val successfulFuture = producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false))
      successfulFuture.get(20, TimeUnit.SECONDS)

      // Get producerId and epoch after first commit. Check after the first successful send of the next transaction to confirm the commit is complete.
      val previousProducerEpoch = producerStateEntry.producerEpoch
      val nextSuccessfulFuture = producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic2, null, "2", "2", willBeCommitted = true))
      nextSuccessfulFuture.get(20, TimeUnit.SECONDS)

```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18035, kafka-18306, kafka-18092: address transactionstest flaky tests (#18264).

**Test**
Test modified in TransactionsTest.scala covering the corrected behaviour.
