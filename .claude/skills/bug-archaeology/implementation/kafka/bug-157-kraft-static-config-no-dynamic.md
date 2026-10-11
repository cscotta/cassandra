**Commit**: a5325e029e · **Ticket**: KAFKA-17431
**Subsystem**: core/src/main/scala/kafka/server/BrokerServer.scala, kafka/server/DynamicBrokerConfig.scala
**Category**: State not cleaned up

**Root cause**
During broker startup in KRaft mode, dynamic configurations (e.g., SSL keystore, listener configs) were not applied until after the broker had already attempted to validate static configs. If the static configuration alone was insufficient (e.g., missing keystore path), the broker refused to start even though valid dynamic configs stored in the latest metadata snapshot would have resolved the issue at runtime. The fix reads dynamic broker configs from the latest local snapshot before the broker proceeds with startup, applying them early so that static+dynamic together satisfy the validation requirements.

**Before/After**
```scala
// Before: dynamic configs applied after static validation only
config.dynamicConfig.initialize(Some(clientMetricsReceiverPlugin))
// quotaManagers and startup proceed with static config only

// After: snapshot-backed dynamic configs applied immediately after initialize
config.dynamicConfig.initialize(Some(clientMetricsReceiverPlugin))
quotaManagers = QuotaFactory.instantiate(config, metrics, time, ...)
DynamicBrokerConfig.readDynamicBrokerConfigsFromSnapshot(raftManager, config, quotaManagers)
```

**How to spot**
When a system supports both static and dynamic configuration, verify that the dynamic overlay is applied as early as possible in the startup sequence — before any validation that depends on the combined configuration. A gap between "load static config" and "load dynamic config" means the system can fail to start on valid configurations that rely on the dynamic layer.

**Test**
`DynamicBrokerReconfigurationTest` was refactored with a `defaultStaticConfig` helper that intentionally sets invalid top-level SSL properties, relying on listener-specific dynamic config to be loaded from the snapshot. This integration test verifies the broker can start successfully with an invalid static config when valid dynamic config is present.
