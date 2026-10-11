**Commit**: 3d23029d1c · **Ticket**: KAFKA-17584
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Several Kafka log configurations in have synonyms. For example, log retention can be configured
either by log.retention.ms, or by log.retention.minutes, or by log.retention.hours. There is also
a faculty in Kafka to dynamically change broker configurations without restarting the broker. These
dynamically set configurations are stored in the metadata log and override what is in the broker
properties file.

**Before/After**
```java
// Before
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef, ConfigException, SaslConfigs, SslConfigs}
import org.apache.kafka.server.config.{ConfigType, ServerConfigs, ReplicationConfigs, ServerLogConfigs, ServerTopicConfigSynonyms, ZooKeeperInternals}
  // Exclude message.format.version for now since we need to check that the version
  // is supported on all brokers in the cluster.
    ServerTopicConfigSynonyms.TOPIC_CONFIG_SYNONYMS.values.asScala.toSet - ServerLogConfigs.LOG_MESSAGE_FORMAT_VERSION_CONFIG
  val KafkaConfigToLogConfigName: Map[String, String] =
    ServerTopicConfigSynonyms.TOPIC_CONFIG_SYNONYMS.asScala.map { case (k, v) => (v, k) }
    val newBrokerDefaults = new util.HashMap[String, Object](originalLogConfig.originals)
    newConfig.valuesFromThisConfig.forEach { (k, v) =>
      if (DynamicLogConfig.ReconfigurableConfigs.contains(k)) {
        DynamicLogConfig.KafkaConfigToLogConfigName.get(k).foreach { configName =>
          if (v == null)

// After
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef, ConfigException, SaslConfigs, SslConfigs, TopicConfig}
import org.apache.kafka.server.config.{ConfigType, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms, ZooKeeperInternals}
  /**
   * The log configurations that are non-reconfigurable. This set contains the names you
   * would use when setting a dynamic configuration on a topic, which are different than the
   * corresponding broker configuration names.
   *
   * For now, message.format.version is not reconfigurable, since we need to check that
   * the version is supported on all brokers in the cluster.
   */
  val NonReconfigrableLogConfigs: Set[String] = Set(TopicConfig.MESSAGE_FORMAT_VERSION_CONFIG)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17584: fix incorrect synonym handling for dynamic log configurations (#17258).

**Test**
Test modified in DynamicBrokerReconfigurationTest.scala covering the corrected behaviour.
