**Commit**: f59d829381 · **Ticket**: KAFKA-15853
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      <allow class="org.apache.kafka.coordinator.transaction.TransactionLogConfigs" />
    val txnConfig = TransactionConfig(config.transactionalIdExpirationMs,
      config.transactionMaxTimeoutMs,
      config.transactionTopicPartitions,
      config.transactionTopicReplicationFactor,
      config.transactionTopicSegmentBytes,
      config.transactionsLoadBufferSize,
      config.transactionTopicMinISR,
      config.transactionAbortTimedOutTransactionCleanupIntervalMs,
      config.transactionRemoveExpiredTransactionalIdCleanupIntervalMs,
import org.apache.kafka.coordinator.transaction.{TransactionLogConfigs, TransactionStateManagerConfigs}
  private val partitionLoadSensor = metrics.sensor(TransactionStateManagerConfigs.LOAD_TIME_SENSOR)

// After
      <allow class="org.apache.kafka.coordinator.transaction.TransactionLogConfig" />
    val txnConfig = TransactionConfig(config.transactionStateManagerConfig.transactionalIdExpirationMs,
      config.transactionStateManagerConfig.transactionMaxTimeoutMs,
      config.transactionLogConfig.transactionTopicPartitions,
      config.transactionLogConfig.transactionTopicReplicationFactor,
      config.transactionLogConfig.transactionTopicSegmentBytes,
      config.transactionLogConfig.transactionsLoadBufferSize,
      config.transactionLogConfig.transactionTopicMinISR,
      config.transactionStateManagerConfig.transactionAbortTimedOutTransactionCleanupIntervalMs,
      config.transactionStateManagerConfig.transactionRemoveExpiredTransactionalIdCleanupIntervalMs,
import org.apache.kafka.coordinator.transaction.{TransactionLogConfig, TransactionStateManagerConfig}
  private val partitionLoadSensor = metrics.sensor(TransactionStateManagerConfig.LOAD_TIME_SENSOR)
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15853 move transactionlogconfig and transactionstatemanagerconfig getters out of kafkaconfig (#16665).

**Test**
Test modified in AdminFenceProducersIntegrationTest.scala covering the corrected behaviour.
