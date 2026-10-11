**Commit**: 6d68f8a82c · **Ticket**: KAFKA-14485
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: Logic error in condition

**Root cause**
This patch moves `BrokerReconfigurable` to the `server-common module`
and decouples the `TransactionLogConfig` and `KafkaConfig` to unblock
KAFKA-14485. Reviewers: PoAn Yang <payang@apache.org>, TaiJuWu <tjwu1217@gmail.com>,
Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    <allow pkg="org.apache.kafka.common" exact-match="true" />
import org.apache.kafka.coordinator.transaction.ProducerIdManager
      config.transactionLogConfig.transactionTopicPartitions,
      config.transactionLogConfig.transactionTopicReplicationFactor,
      config.transactionLogConfig.transactionTopicSegmentBytes,
      config.transactionLogConfig.transactionLoadBufferSize,
      config.transactionLogConfig.transactionTopicMinISR,
      producerStateManagerConfig = new ProducerStateManagerConfig(config.transactionLogConfig.producerIdExpirationMs, config.transactionLogConfig.transactionPartitionVerificationEnable),
      producerIdExpirationCheckIntervalMs = config.transactionLogConfig.producerIdExpirationCheckIntervalMs,

// After
    <allow pkg="org.apache.kafka.common" />
  <allow pkg="org.apache.kafka.config"/>
import org.apache.kafka.coordinator.transaction.{ProducerIdManager, TransactionLogConfig}
    val transactionLogConfig = new TransactionLogConfig(config)
      transactionLogConfig.transactionTopicPartitions,
      transactionLogConfig.transactionTopicReplicationFactor,
      transactionLogConfig.transactionTopicSegmentBytes,
      transactionLogConfig.transactionLoadBufferSize,
      transactionLogConfig.transactionTopicMinISR,
import org.apache.kafka.coordinator.transaction.TransactionLogConfig
    val transactionLogConfig = new TransactionLogConfig(config)
      producerStateManagerConfig = new ProducerStateManagerConfig(transactionLogConfig.producerIdExpirationMs, transactionLogConfig.transactionPartitionVerificationEnable),
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: move brokerreconfigurable to the sever-common module (#19383).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
