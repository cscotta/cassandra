**Commit**: c23d4a0d73 · **Ticket**: KAFKA-18499
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  * The TopicConfigHandler will process topic config changes from ZooKeeper or the metadata log.
          kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), false)
      () => LogConfig.validate(Collections.emptyMap(), props, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true))
    LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true)
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true))
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true))
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true))
      LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true)
        () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled(), true))

// After
  * The TopicConfigHandler will process topic config changes from the metadata log.
          kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled())
      () => LogConfig.validate(Collections.emptyMap(), props, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled()))
    LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled())
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled()))
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled()))
      () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled()))
      LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled())
        () => LogConfig.validate(Collections.emptyMap(), logProps, kafkaConfig.extractLogConfigMap, kafkaConfig.remoteLogManagerConfig.isRemoteStorageSystemEnabled()))
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18499 clean up zookeeper from logconfig (#18583).

**Test**
Test modified in LogConfigTest.scala covering the corrected behaviour.
