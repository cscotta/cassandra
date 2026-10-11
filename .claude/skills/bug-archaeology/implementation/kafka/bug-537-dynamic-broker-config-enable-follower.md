**Commit**: 25a8ae0713 · **Ticket**: KAFKA-17266
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In this PR, we add a dynamic broker config to enable/disable follower
fetch using tiered offset. This is part of the implementation for

[KIP-1023](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1023%3A+Follower+fetch+from+tiered+offset)

Added tests for the new dynamic broker config. Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{DynamicProducerStateManagerConfig, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}

// After
import org.apache.kafka.server.config.{DynamicProducerStateManagerConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
    DynamicReplicationConfig.ReconfigurableConfigs ++
    addBrokerReconfigurable(new DynamicReplicationConfig(kafkaServer))

class DynamicReplicationConfig(server: KafkaBroker) extends BrokerReconfigurable with Logging {
  override def reconfigurableConfigs: Set[String] = {
    DynamicReplicationConfig.ReconfigurableConfigs
  }

  override def validateReconfiguration(newConfig: KafkaConfig): Unit = {
    // Currently it is a noop for reconfiguring the dynamic config follower.fetch.last.tiered.offset.enable
  }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17266: add dynamic broker config to enable follower fetch using tiered offset (#16834).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
