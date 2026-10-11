**Commit**: 822b8ab3d7 · **Ticket**: KAFKA-18691
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
It appears this test was failing because the transaction was never aborting and the concurrent transactions errors would not go away. ccab9eb introduced the test failure because it requires the transaction to complete, but I suspect the lack of completion was happening before the change. The timeout for the write is based on the transactional timeout, and 100ms seemed too small -- thus the requests to update the state would often repeatedly time out. Also removed the loop since it was not necessary.

**Before/After**
```java
// Before
    val producer = createTransactionalProducer("expiringProducer", transactionTimeoutMs = 100)
    TestUtils.waitUntilTrue(() => {
      var foundException = false
      try {
        // Now that the transaction has expired, the second send should fail with a InvalidProducerEpochException. We may see some concurrentTransactionsExceptions.
        producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false)).get()
        fail("should have raised an error due to concurrent transactions or invalid producer epoch")
      } catch {
        case _: ConcurrentTransactionsException =>
        case _: InvalidProducerEpochException =>
        case e: ExecutionException =>
          foundException = e.getCause.isInstanceOf[InvalidProducerEpochException]

// After
    val producer = createTransactionalProducer("expiringProducer", transactionTimeoutMs = 300)
    try {
      // Now that the transaction has expired, the second send should fail with a InvalidProducerEpochException. We may see some concurrentTransactionsExceptions.
      producer.send(TestUtils.producerRecordWithExpectedTransactionStatus(topic1, null, "2", "2", willBeCommitted = false)).get()
      fail("should have raised an error due to concurrent transactions or invalid producer epoch")
    } catch {
      case _: ConcurrentTransactionsException =>
      case _: InvalidProducerEpochException =>
      case e: ExecutionException =>
        assertTrue(e.getCause.isInstanceOf[InvalidProducerEpochException], "Error was " + e.getCause + " and not InvalidProducerEpochException")
    }
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18691: flaky test testfencingontransactionexpiration (#18793).

**Test**
Test modified in TransactionsTest.scala covering the corrected behaviour.
