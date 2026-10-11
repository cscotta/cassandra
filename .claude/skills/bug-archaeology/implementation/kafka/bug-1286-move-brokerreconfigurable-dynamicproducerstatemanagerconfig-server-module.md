**Commit**: b83a23a4f9 · **Ticket**: KAFKA-18946
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch is to move `DynamicProducerStateManagerConfig` and `BrokerReconfigurable` to the server module. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
import org.apache.kafka.storage.internals.log.{LogConfig, ProducerStateManagerConfig}
class DynamicProducerStateManagerConfig(val producerStateManagerConfig: ProducerStateManagerConfig) extends BrokerReconfigurable with Logging {
  def reconfigure(oldConfig: KafkaConfig, newConfig: KafkaConfig): Unit = {
    if (producerStateManagerConfig.producerIdExpirationMs != newConfig.transactionLogConfig.producerIdExpirationMs) {
      info(s"Reconfigure ${TransactionLogConfig.PRODUCER_ID_EXPIRATION_MS_CONFIG} from ${producerStateManagerConfig.producerIdExpirationMs} to ${newConfig.transactionLogConfig.producerIdExpirationMs}")
      producerStateManagerConfig.setProducerIdExpirationMs(newConfig.transactionLogConfig.producerIdExpirationMs)
    }
    if (producerStateManagerConfig.transactionVerificationEnabled != newConfig.transactionLogConfig.transactionPartitionVerificationEnable) {
      info(s"Reconfigure ${TransactionLogConfig.TRANSACTION_PARTITION_VERIFICATION_ENABLE_CONFIG} from ${producerStateManagerConfig.transactionVerificationEnabled} to ${newConfig.transactionLogConfig.transactionPartitionVerificationEnable}")
      producerStateManagerConfig.setTransactionVerificationEnabled(newConfig.transactionLogConfig.transactionPartitionVerificationEnable)
    }

// After
import org.apache.kafka.server.config.{DynamicProducerStateManagerConfig, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
import org.apache.kafka.storage.internals.log.LogConfig
  def addBrokerReconfigurable(reconfigurable: org.apache.kafka.server.config.BrokerReconfigurable): Unit = {
    verifyReconfigurableConfigs(reconfigurable.reconfigurableConfigs.asScala)
    brokerReconfigurables.add(new BrokerReconfigurable {
      override def reconfigurableConfigs: Set[String] = reconfigurable.reconfigurableConfigs().asScala

      override def validateReconfiguration(newConfig: KafkaConfig): Unit = reconfigurable.validateReconfiguration(newConfig)

      override def reconfigure(oldConfig: KafkaConfig, newConfig: KafkaConfig): Unit = reconfigurable.reconfigure(oldConfig, newConfig)
    })
  }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18946 move brokerreconfigurable and dynamicproducerstatemanagerconfig to server module (#19174).

**Test**
No test added.
