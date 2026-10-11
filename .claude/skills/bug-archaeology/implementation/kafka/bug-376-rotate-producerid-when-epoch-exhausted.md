**Commit**: 783e501377 · **Ticket**: KAFKA-20090
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
### Motivation
In TV2, there is an edge-case race at the epoch exhaustion boundary
(`Short.MaxValue - 1`) that can leave a transaction stuck in `ONGOING`. When a transaction times out at epoch `Short.MaxValue - 1`, the
coordinator fences and aborts it. During this flow, the epoch is bumped
to `Short.MaxValue` and the transaction proceeds toward abort
completion. If a delayed client `EndTxn(ABORT)` arrives with the old
epoch, the request may be treated as a retry path.

**Before/After**
```java
// Before
              // Maybe allocate new producer ID if we are bumping epoch and epoch is exhausted
                if (!isEpochFence && txnMetadata.isProducerEpochExhausted) {
import org.apache.kafka.server.common.TransactionVersion

      any[TxnTransitMetadata],
      capturedErrorsCallback.capture(),
      // Simulate the metadata update that would happen in the real appendTransactionToLog

// After
              // EndTxn completion on TV2 bumps epoch, so rotate producer ID whenever the current epoch is exhausted.
              // This must also apply to the epoch-fence path.
                if (txnMetadata.isProducerEpochExhausted) {
import org.apache.kafka.server.common.{RequestLocal, TransactionVersion}

    val bumpedUpProducerId = producerId + 1L
    val bumpedUpEpoch = 0.toShort
    when(pidGenerator.generateProducerId())
      .thenReturn(bumpedUpProducerId)
      capturedTxnTransitMetadata.capture(),
      any[Errors => Unit](),
      val callback = invocation.getArgument[Errors
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20090: rotate producerid when epoch exhausted during transaction completion. (#21506).

**Test**
Test modified in TransactionCoordinatorTest.scala covering the corrected behaviour.
