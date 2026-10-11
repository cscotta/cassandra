**Commit**: e5b8712993 · **Ticket**: KAFKA-16885
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Kamal Chandraprakash <kchandraprakash@uber.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (this.rlmConfig.enableRemoteStorageSystem() && !isRemoteLogManagerConfigured()) {
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().enableRemoteStorageSystem());
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().enableRemoteStorageSystem());
    LogConfig.validateBrokerLogConfigValues(defaultProps, config.remoteLogManagerConfig.enableRemoteStorageSystem())
      remoteStorageSystemEnable = config.remoteLogManagerConfig.enableRemoteStorageSystem(),
      brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig.enableRemoteStorageSystem())
    if (config.remoteLogManagerConfig.enableRemoteStorageSystem()) {

// After
        if (this.rlmConfig.isRemoteStorageSystemEnabled() && !isRemoteLogManagerConfigured()) {
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().isRemoteStorageSystemEnabled());
        if (brokerTopicStats == null) brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig().isRemoteStorageSystemEnabled());
    LogConfig.validateBrokerLogConfigValues(defaultProps, config.remoteLogManagerConfig.isRemoteStorageSystemEnabled())
      remoteStorageSystemEnable = config.remoteLogManagerConfig.isRemoteStorageSystemEnabled(),
      brokerTopicStats = new BrokerTopicStats(config.remoteLogManagerConfig.isRemoteStorageSystemEnabled())
    if (config.remoteLogManagerConfig.isRemoteStorageSystemEnabled()) {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16885 renamed the enableremotestoragesystem to isremotestoragesystemenabled (#16256).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
