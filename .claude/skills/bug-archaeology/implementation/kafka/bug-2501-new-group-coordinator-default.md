**Commit**: aaf887d3d9 · **Ticket**: KAFKA-14048
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch makes the new group coordinator, introduced as part of KIP-848, the default. This means that any KRaft cluster created from trunk defaults to using the new group coordinator. This includes all the integration tests which do not specify it. This patch also changes the default in system tests.

**Before/After**
```java
// Before
      if (!config.isNewGroupCoordinatorEnabled) {
          if (config.isNewGroupCoordinatorEnabled && partition.topic == GROUP_METADATA_TOPIC_NAME) {
    config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.CONSUMER)
    config.isNewGroupCoordinatorEnabled && config.shareGroupConfig.isShareGroupEnabled
      warn(s"The new '${GroupType.CONSUMER}' rebalance protocol is enabled along with the new group coordinator. " +
        "This is part of the preview of KIP-848 and MUST NOT be used in production.")
      // The CONSUMER protocol enables the new group coordinator, and that's a prerequisite for share groups.
      if (!protocols.contains(GroupType.CONSUMER)) {
        throw new ConfigException(s"Enabling the new '${GroupType.SHARE}' rebalance protocol requires '${GroupType.CONSUMER}' to be enabled also.")

// After
  override def isNewGroupCoordinator: Boolean = false

      if (!groupCoordinator.isNewGroupCoordinator) {
          if (groupCoordinator.isNewGroupCoordinator && partition.topic == GROUP_METADATA_TOPIC_NAME) {
    groupCoordinator.isNewGroupCoordinator && config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.CONSUMER)
    groupCoordinator.isNewGroupCoordinator && config.shareGroupConfig.isShareGroupEnabled
  val isNewGroupCoordinatorEnabled = getBoolean(GroupCoordinatorConfig.NEW_GROUP_COORDINATOR_ENABLE_CONFIG)
      if (!isNewGroupCoordinatorEnabled) {
        throw new ConfigException(s"The new '${GroupType.CONSUMER}' rebalance protocol is only supported by the new group coordinator.")
      }
      if (processRoles.isEmpty) {
        throw new ConfigException(s"The new '${GroupType.SHARE}' rebalance protocol is only supported in KRaft cluster.")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14048; [2/2] use the new group coordinator by default in 4.0 (#16945).

**Test**
Test modified in ClusterTestExtensionsTest.java covering the corrected behaviour.
