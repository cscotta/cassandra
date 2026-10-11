**Commit**: a628d9bc4d · **Ticket**: KAFKA-18365
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{KRaftConfigs, ReplicationConfigs, ZkConfigs}
    assertEquals(1, config1.brokerId)
    val config2 = KafkaConfig.fromProps(Kafka.getPropsFromArgs(Array(propertiesFile, "--override", "broker.id=2")))
    assertEquals(2, config2.brokerId)
    assertEquals(1, config3.brokerId)
    val config4 = KafkaConfig.fromProps(Kafka.getPropsFromArgs(Array(propertiesFile, "--override", "log.cleanup.policy=compact,delete", "--override", "broker.id=2")))
    assertEquals(2, config4.brokerId)

    // Ensure that if neither process.roles nor controller.quorum.voters is populated, then an exception is thrown if zookeeper.connect is not defined
    propertiesFile.setProperty(KRaftConfigs.PROCESS_ROLES_CONFIG, "")
    assertBadConfigContainingMessage(propertiesFile,
      "Missing required configuration `zookeeper.connect` which has no default value.")

// After
import org.apache.kafka.server.config.{KRaftConfigs, ReplicationConfigs}
    assertEquals(1, config1.nodeId)
    val config2 = KafkaConfig.fromProps(Kafka.getPropsFromArgs(Array(propertiesFile, "--override", "node.id=2")))
    assertEquals(2, config2.nodeId)
    assertEquals(1, config3.nodeId)
    val config4 = KafkaConfig.fromProps(Kafka.getPropsFromArgs(Array(propertiesFile, "--override", "log.cleanup.policy=compact,delete", "--override", "node.id=2")))
    assertEquals(2, config4.nodeId)
    prepareConfig(Array(
      "node.id=1",
      "process.roles=controller",
      "controller.listener.names=CONTROLLER",
      "controller.quorum.voters=1@localhost:9093,2@localhost:9093",
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18365 remove zookeeper.connect in test (#18353).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
