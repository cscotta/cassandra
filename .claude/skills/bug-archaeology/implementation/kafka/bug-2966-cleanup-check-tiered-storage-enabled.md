**Commit**: 8507693229 · **Ticket**: KAFKA-16859
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.isRemoteLogStorageSystemEnabled());
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.isRemoteLogStorageSystemEnabled());
    LogConfig.validateBrokerLogConfigValues(defaultProps, config.isRemoteLogStorageSystemEnabled)
      brokerTopicStats = new BrokerTopicStats(config.isRemoteLogStorageSystemEnabled)
    logManager.updateTopicConfig(topic, props, kafkaConfig.isRemoteLogStorageSystemEnabled)
        LogConfig.validate(properties, kafkaConfig.extractLogConfigMap, kafkaConfig.isRemoteLogStorageSystemEnabled)

// After
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().enableRemoteStorageSystem());
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().enableRemoteStorageSystem());
    LogConfig.validateBrokerLogConfigValues(defaultProps, config.remoteLogManagerConfig.enableRemoteStorageSystem())
      brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig.enableRemoteStorageSystem())
    logManager.updateTopicConfig(topic, props, kafkaConfig.remoteLogManagerConfig.enableRemoteStorageSystem())
        LogConfig.validate(properties, kafkaConfig.extractLogConfigMap,
          kafkaConfig.remoteLogManagerConfig.enableRemoteStorageSystem())
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16859 cleanup check if tiered storage is enabled (#16153).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
