**Commit**: 7c7751d8da · **Ticket**: KAFKA-14048
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch prepares the ground to switch on the new group coordinator by default. It basically ensures that the integration tests uses the internal flag to enable/disable the new group coordinator instead of relying on the auto-enabling that we had in place for the early access and the preview. The auto-enabling will be removed in a follow-up PR. Reviewers: Jeff Kim <jeff.kim@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  @ClusterTest(types = Array(Type.ZK))
      new ClusterConfigProperty(key = "group.coordinator.rebalance.protocols", value = "classic,consumer"),
      new ClusterConfigProperty(key = "offsets.topic.num.partitions", value = "1"),
      new ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1")
import org.apache.kafka.coordinator.group.GroupConfig
      new ClusterConfigProperty(key = "group.coordinator.rebalance.protocols", value = "classic,consumer"),
      new ClusterConfigProperty(key = "offsets.topic.num.partitions", value = "1"),
      new ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1")
      new ClusterConfigProperty(key = "group.coordinator.rebalance.protocols", value = "classic,consumer"),
      new ClusterConfigProperty(key = "offsets.topic.num.partitions", value = "1"),
      new ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1")

// After
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig
  @ClusterTest(types = Array(Type.ZK), serverProperties = Array(
    new ClusterConfigProperty(key = GroupCoordinatorConfig.NEW_GROUP_COORDINATOR_ENABLE_CONFIG, value = "false"),
  ))
      new ClusterConfigProperty(key = GroupCoordinatorConfig.NEW_GROUP_COORDINATOR_ENABLE_CONFIG, value = "true"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.GROUP_COORDINATOR_REBALANCE_PROTOCOLS_CONFIG, value = "classic,consumer"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, value = "1"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, value = "1")
import org.apache.kafka.coordinator.group.{GroupConfig, GroupCoordinatorConfig}
      new ClusterConfigProperty(key = GroupCoordinatorConfig.NEW_GROUP_COORDINATOR_ENABLE_CONFIG, value = "true"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.GROUP_COORDINATOR_REBALANCE_PROTOCOLS_CONFIG, value = "classic,consumer"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, value = "1"),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14048; [1/n] use the new group coordinator by default in 4.0 (#16937).

**Test**
Test modified in ConsumerGroupDescribeRequestTest.scala covering the corrected behaviour.
