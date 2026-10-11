**Commit**: e4cd243abc · **Ticket**: KAFKA-20337
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
All GroupConfig fields are now Optional<T>, storing only explicitly
provided values. Broker-level defaults are resolved at access time via
flatMap().orElse(brokerDefault), eliminating stale-capture issues when
broker configs change dynamically. Key changes:
- All 21 GroupConfig fields are private Optional, using
optionalInt/Boolean/String helpers based on originals(). - GroupConfigManager no longer needs a defaultConfig; constructor
simplified.

**Before/After**
```java
// Before
      groupConfigManager = new GroupConfigManager(config.groupCoordinatorConfig.extractGroupConfigMap(config.shareGroupConfig), config.groupCoordinatorConfig, config.shareGroupConfig)
              val heartbeatIntervalMs = Option(groupConfigManager.groupConfig(streamsGroupHeartbeatRequest.data.groupId).orElse(null))
                .map(_.streamsHeartbeatIntervalMs().toLong)
                .getOrElse(config.groupCoordinatorConfig.streamsGroupHeartbeatIntervalMs().toLong)
        FetchIsolation.of(FetchRequest.CONSUMER_REPLICA_ID, groupConfigManager.groupConfig(groupId).map(_.shareIsolationLevel()).orElse(GroupConfig.defaultShareIsolationLevel)),
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(ShareGroupAutoOffsetResetStrategy.EARLIEST);
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(ShareGroupAutoOffsetResetStrategy.LATEST);
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(resetStrategy);

// After
      groupConfigManager = new GroupConfigManager(config.groupCoordinatorConfig, config.shareGroupConfig)
              val heartbeatIntervalMs = groupConfigManager.groupConfig(streamsGroupHeartbeatRequest.data.groupId)
                .flatMap[java.lang.Integer](gc => gc.streamsHeartbeatIntervalMs())
                .orElseGet(() => config.groupCoordinatorConfig.streamsGroupHeartbeatIntervalMs())
                .toLong
        FetchIsolation.of(FetchRequest.CONSUMER_REPLICA_ID, groupConfigManager.groupConfig(groupId).flatMap(_.shareIsolationLevel()).orElse(GroupConfig.defaultShareIsolationLevel)),
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(Optional.of(ShareGroupAutoOffsetResetStrategy.EARLIEST));
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(Optional.of(ShareGroupAutoOffsetResetStrategy.LATEST));
        Mockito.when(groupConfig.shareAutoOffsetReset()).thenReturn(Optional.of(resetStrategy));
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20337 make all groupconfig fields optional and clean up validation (#22003).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
