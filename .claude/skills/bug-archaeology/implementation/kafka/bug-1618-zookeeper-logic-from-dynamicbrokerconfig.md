**Commit**: 25f2ed090a · **Ticket**: KAFKA-18405
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      config.dynamicConfig.initialize(zkClientOpt = None, Some(clientMetricsReceiverPlugin))
      config.dynamicConfig.initialize(zkClientOpt = None, clientMetricsReceiverPluginOpt = None)
import kafka.zk.{AdminZkClient, KafkaZkClient}
import org.apache.kafka.server.config.{ConfigType, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms, ZooKeeperInternals}
  *   <li>DYNAMIC_BROKER_CONFIG: stored in ZK at /configs/brokers/{brokerId}</li>
  *   <li>DYNAMIC_DEFAULT_BROKER_CONFIG: stored in ZK at /configs/brokers/&lt;default&gt;</li>
  private[server] def initialize(zkClientOpt: Option[KafkaZkClient], clientMetricsReceiverPluginOpt: Option[ClientMetricsReceiverPlugin]): Unit = {

    zkClientOpt.foreach { zkClient =>
      val adminZkClient = new AdminZkClient(zkClient)
      updateDefaultConfig(adminZkClient.fetchEntityConfig(ConfigType.BROKER, ZooKeeperInternals.DEFAULT_STRING), false)
      val props = adminZkClient.fetchEntityConfig(ConfigType.BROKER, kafkaConfig.brokerId.toString)

// After
      config.dynamicConfig.initialize(Some(clientMetricsReceiverPlugin))
      config.dynamicConfig.initialize(clientMetricsReceiverPluginOpt = None)
import org.apache.kafka.server.config.{ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
  private[server] def initialize(clientMetricsReceiverPluginOpt: Option[ClientMetricsReceiverPlugin]): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18405 remove zookeeper logic from dynamicbrokerconfig (#18508).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
