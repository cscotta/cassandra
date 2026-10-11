**Commit**: 206d0f809a · **Ticket**: KAFKA-16909
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    config.offsetMetadataMaxSize,
    config.offsetsLoadBufferSize,
    config.offsetsRetentionMinutes * 60L * 1000L,
    config.offsetsRetentionCheckIntervalMs,
    config.offsetsTopicPartitions,
    config.offsetsTopicSegmentBytes,
    config.offsetsTopicReplicationFactor,
    config.offsetsTopicCompressionType,
    config.offsetCommitTimeoutMs,
    config.offsetCommitRequiredAcks
    val groupConfig = GroupConfig(groupMinSessionTimeoutMs = config.groupMinSessionTimeoutMs,
      groupMaxSessionTimeoutMs = config.groupMaxSessionTimeoutMs,

// After
    config.groupCoordinatorConfig.offsetMetadataMaxSize,
    config.groupCoordinatorConfig.offsetsLoadBufferSize,
    config.groupCoordinatorConfig.offsetsRetentionMs,
    config.groupCoordinatorConfig.offsetsRetentionCheckIntervalMs,
    config.groupCoordinatorConfig.offsetsTopicPartitions,
    config.groupCoordinatorConfig.offsetsTopicSegmentBytes,
    config.groupCoordinatorConfig.offsetsTopicReplicationFactor,
    config.groupCoordinatorConfig.offsetTopicCompressionType,
    config.groupCoordinatorConfig.offsetCommitTimeoutMs,
    config.groupCoordinatorConfig.offsetCommitRequiredAcks
    val groupConfig = GroupConfig(groupMinSessionTimeoutMs = config.groupCoordinatorConfig.classicGroupMinSessionTimeoutMs,
      groupMaxSessionTimeoutMs = config.groupCoordinatorConfig.classicGroupMaxSessionTimeoutMs,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16909 refactor groupcoordinatorconfig with abstractconfig (#16458).

**Test**
Test modified in DynamicBrokerReconfigurationTest.scala covering the corrected behaviour.
