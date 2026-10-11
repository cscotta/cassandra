**Commit**: 772aa241b2 · **Ticket**: KAFKA-18136
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // For Zk mode, the API forwarding is currently enabled only under migration flag. We can
  // directly do a static IBP check to see API forwarding is enabled here because IBP check is
  // static in Zk mode.
  private def enableApiForwarding(config: KafkaConfig) =
    config.migrationEnabled && config.interBrokerProtocolVersion.isApiForwardingEnabled

        threadNamePrefix = None,
        enableForwarding = enableApiForwarding(config)
import kafka.controller.KafkaController.{ActiveBrokerCountMetricName, ActiveControllerCountMetricName, AlterReassignmentsCallback, ControllerStateMetricName, ElectLeadersCallback, FencedBrokerCountMetricName, GlobalPartitionCountMetricName, GlobalTopicCountMetricName, ListReassignmentsCallback, OfflinePartitionsCountMetricName, PreferredReplicaImbalanceCountMetricName, ReplicasIneligibleToDeleteCountMetricName, ReplicasToDeleteCountMetricName, TopicsIneligibleToDeleteCountMetricName, TopicsToDeleteCountMetricName, UpdateFeaturesCallback, ZkMigrationStateMetricName}
import org.apache.kafka.metadata.migration.ZkMigrationState
  private val ZkMigrationStateMetricName = "ZkMigrationState"
    ZkMigrationStateMetricName,

// After
        threadNamePrefix = None
import kafka.controller.KafkaController.{ActiveBrokerCountMetricName, ActiveControllerCountMetricName, AlterReassignmentsCallback, ControllerStateMetricName, ElectLeadersCallback, FencedBrokerCountMetricName, GlobalPartitionCountMetricName, GlobalTopicCountMetricName, ListReassignmentsCallback, OfflinePartitionsCountMetricName, PreferredReplicaImbalanceCountMetricName, ReplicasIneligibleToDeleteCountMetricName, ReplicasToDeleteCountMetricName, TopicsIneligibleToDeleteCountMetricName, TopicsToDeleteCountMetricName, UpdateFeaturesCallback}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18136: remove zk migration from code base (#18016).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
