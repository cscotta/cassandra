**Commit**: 0c5e5c5d2d · **Ticket**: KAFKA-18329
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch is the second of a series of patches to remove the old group
coordinator. With the release of Apache Kafka 4.0, the so-called new
group coordinator is the default and only option available now. The patch removes `group.coordinator.new.enable` (internal config) and
all its usages (integration tests, unit tests, etc.). It also cleans up
`KafkaApis` to remove logic only used by the old group coordinator.

**Before/After**
```java
// Before
        @ClusterTest(serverProperties = {
            @ClusterConfigProperty(key = "offsets.topic.num.partitions", value = "1"),
            @ClusterConfigProperty(key = "offsets.topic.replication.factor", value = "1"),
            @ClusterConfigProperty(key = "group.coordinator.new.enable", value = "false")
        }),
    public void testAsyncConsumerWithOldGroupCoordinator(ClusterInstance clusterInstance) throws Exception {
  override def isNewGroupCoordinator: Boolean = false

import kafka.coordinator.group.{CoordinatorLoaderImpl, CoordinatorPartitionWriter, GroupCoordinatorAdapter}
    if (config.isNewGroupCoordinatorEnabled) {
      val time = Time.SYSTEM
      val serde = new GroupCoordinatorRecordSerde

// After
    public void testAsyncConsumerWithConsumerProtocolDisabled(ClusterInstance clusterInstance) throws Exception {
import kafka.coordinator.group.{CoordinatorLoaderImpl, CoordinatorPartitionWriter}
    val time = Time.SYSTEM
    val serde = new GroupCoordinatorRecordSerde
    val timer = new SystemTimerReaper(
      "group-coordinator-reaper",
      new SystemTimer("group-coordinator")
    )
    val loader = new CoordinatorLoaderImpl[CoordinatorRecord](
      time,
      replicaManager,
      serde,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18329; [2/3] delete old group coordinator (kip-848) (#19251).

**Test**
Test modified in ConsumerIntegrationTest.java covering the corrected behaviour.
