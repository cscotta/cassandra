**Commit**: 9cb271f1e1 · **Ticket**: KAFKA-18654
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
During the transaction commit phase, it is normal to hit CONCURRENT_TRANSACTION error before the transaction markers are fully propagated. Instead of letting the client to retry the produce request, it is better to retry on the server side. Reviewers: Artem Livshits <alivshits@confluent.io>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.transaction.{TransactionLogConfig, TransactionStateManagerConfig}
      // Wrap the callback to be handled on an arbitrary request handler thread
      // when transaction verification is complete. The request local passed in
      // is only used when the callback is executed immediately.
      KafkaRequestHandler.wrapAsyncCallback(
        postVerificationCallback,
        requestLocal
      ),

// After
import org.apache.kafka.coordinator.transaction.{AddPartitionsToTxnConfig, TransactionLogConfig, TransactionStateManagerConfig}
  private val _addPartitionsToTxnConfig = new AddPartitionsToTxnConfig(this)
  def addPartitionsToTxnConfig: AddPartitionsToTxnConfig = _addPartitionsToTxnConfig
    // Wrap the callback to be handled on an arbitrary request handler thread
    // when transaction verification is complete. The request local passed in
    // is only used when the callback is executed immediately.
    val wrappedPostVerificationCallback = KafkaRequestHandler.wrapAsyncCallback(
      postVerificationCallback,
      requestLocal
    )

    val retryTimeoutMs = Math.min(config.addPartitionsToTxnConfig.addPartitionsToTxnRetryBackoffMaxMs(), config.requestTimeoutMs)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18654[2/2]: transction v2 retry add partitions on the server side when handling produce request. (#18810).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
