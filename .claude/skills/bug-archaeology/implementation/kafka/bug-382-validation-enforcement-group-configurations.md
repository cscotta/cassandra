**Commit**: e36319010c · **Ticket**: KAFKA-20162
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Implement KAFKA-20162 (KIP-1240) by adding broker-level bound evaluation
for dynamic group configs. Out-of-range values are capped to broker
limits with WARN logs, while Admin validation remains strict and
metadata-stored raw values are preserved. Reviewers: Sean Quah <squah@confluent.io>, Andrew Schofield
<aschofield@confluent.io>

**Before/After**
```java
// Before
      groupConfigManager = new GroupConfigManager(config.groupCoordinatorConfig.extractGroupConfigMap(config.shareGroupConfig))
import org.apache.kafka.coordinator.group.GroupConfigManager
        GroupConfigManager.validate(properties, kafkaConfig.groupCoordinatorConfig, kafkaConfig.shareGroupConfig)

// After
      groupConfigManager = new GroupConfigManager(config.groupCoordinatorConfig.extractGroupConfigMap(config.shareGroupConfig), config.groupCoordinatorConfig, config.shareGroupConfig)
import org.apache.kafka.coordinator.group.GroupConfig
        GroupConfig.validate(properties, kafkaConfig.groupCoordinatorConfig, kafkaConfig.shareGroupConfig)
  @Test
  def testGroupConfigEvaluatedAfterBrokerRestart(): Unit = {
    client = createAdminClient
    val groupId = "evaluated-config-test-group"
    val groupResource = new ConfigResource(ConfigResource.Type.GROUP, groupId)

    // Set a valid group config (55000 is within default [45000, 60000])
    val alterOps = util.List.of(
      new AlterConfigOp(new ConfigEntry(GroupConfig.CONSUMER_SESSION_TIMEOUT_MS_CONFIG, "55000"), AlterConfigOp.OpType.SET)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20162: validation and enforcement of group configurations (#21633).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
