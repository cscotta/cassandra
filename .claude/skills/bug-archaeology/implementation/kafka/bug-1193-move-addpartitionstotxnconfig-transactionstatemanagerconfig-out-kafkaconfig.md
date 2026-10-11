**Commit**: b18f00b449 · **Ticket**: KAFKA-19121
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Both AddPartitionsToTxnConfig and TransactionStateManagerConfig are
static configs and they don't have specific config check. We can move
them out of KafkaConfig to simplify KafkaConfig. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.transaction.{ProducerIdManager, TransactionLogConfig}
    val txnConfig = TransactionConfig(config.transactionStateManagerConfig.transactionalIdExpirationMs,
      config.transactionStateManagerConfig.transactionMaxTimeoutMs,
      config.transactionStateManagerConfig.transactionAbortTimedOutTransactionCleanupIntervalMs,
      config.transactionStateManagerConfig.transactionRemoveExpiredTransactionalIdCleanupIntervalMs,
      config.transactionStateManagerConfig.transaction2PCEnabled,
import org.apache.kafka.coordinator.transaction.TransactionLogConfig
      maxTransactionTimeoutMs = config.transactionStateManagerConfig.transactionMaxTimeoutMs,
import org.apache.kafka.coordinator.transaction.{AddPartitionsToTxnConfig, TransactionStateManagerConfig}
  private val _transactionStateManagerConfig = new TransactionStateManagerConfig(this)
  private val _addPartitionsToTxnConfig = new AddPartitionsToTxnConfig(this)
  def transactionStateManagerConfig: TransactionStateManagerConfig = _tr

// After
import org.apache.kafka.coordinator.transaction.{ProducerIdManager, TransactionLogConfig, TransactionStateManagerConfig}
    val transactionStateManagerConfig = new TransactionStateManagerConfig(config)
    val txnConfig = TransactionConfig(transactionStateManagerConfig.transactionalIdExpirationMs,
      transactionStateManagerConfig.transactionMaxTimeoutMs,
      transactionStateManagerConfig.transactionAbortTimedOutTransactionCleanupIntervalMs,
      transactionStateManagerConfig.transactionRemoveExpiredTransactionalIdCleanupIntervalMs,
      transactionStateManagerConfig.transaction2PCEnabled,
import org.apache.kafka.coordinator.transaction.{TransactionLogConfig, TransactionStateManagerConfig}
      maxTransactionTimeoutMs = new TransactionStateManagerConfig(config).transactionMaxTimeoutMs,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19121 move addpartitionstotxnconfig and transactionstatemanagerconfig out of kafkaconfig (#19439).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
