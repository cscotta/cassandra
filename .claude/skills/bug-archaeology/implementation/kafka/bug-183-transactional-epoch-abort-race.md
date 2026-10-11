**Commit**: 400363b7e2 · **Ticket**: KAFKA-18035
**Subsystem**: core/src/test/scala/integration/kafka/api/TransactionsTest.scala
**Category**: Race condition / missing synchronization

**Root cause**: `testBumpTransactionalEpochWithTV2Disabled` assumed the producer would enter `ABORTABLE_ERROR` state before `abortTransaction()` was called, but the broker restart that triggers the abortable error is asynchronous. If the producer had not yet received the error response when `abortTransaction()` was called, the state machine would skip the epoch bump and the final assertion `producerEpoch > initialProducerEpoch` would fail. The fix polls until a send genuinely raises a `KafkaException` (confirming abortable state) before calling abort.

**Before/After**:
```scala
// BEFORE – abortTransaction called without waiting for abortable state
org.apache.kafka.test.TestUtils.assertFutureThrows(failedFuture, classOf[TimeoutException])
producer.abortTransaction()

// AFTER – spin until the producer is actually in abortable error state
TestUtils.waitUntilTrue(() => {
    var failed = false
    try { producer.send(...) } catch { case e: KafkaException => failed = true }
    failed
}, "The send request never failed as expected.")
assertThrows(classOf[KafkaException], () => producer.send(...))
producer.abortTransaction()
```

**How to spot**: In transactional producer tests, `abortTransaction()` is only legal in `ABORTABLE_ERROR` state. Any test that calls abort immediately after an asynchronous failure (future timeout, broker restart) without verifying the client-side state machine has transitioned is a latent race.

**Test**: The patched test adds a `waitUntilTrue` loop and a subsequent `assertThrows` to confirm the producer is in abortable state. It also improves the final assertion message to include `initialProducerEpoch` and `producerStateEntry` for diagnostics on failure.
