**Commit**: 793dcee541 · **Ticket**: KAFKA-19376
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java
**Category**: Missing null / bounds check

**Root cause**
When the KIP-1071 STREAMS rebalance protocol was enabled, several unsupported features (pattern subscriptions, named topologies, static membership, warmup replicas) could be used without any early validation. The check for named topologies only existed deep inside `StreamThread.create()`, which ran after the admin client had already been created. The fix moves validation to a new `throwIfUnsupportedFeatureIsUsedWithStreamsRebalanceProtocol()` call that runs before the admin client is instantiated, so users receive a clear `UnsupportedOperationException` or `ConfigException` immediately on startup rather than a cryptic runtime failure.

**Before/After**
```java
// Before: validation only in StreamThread, after admin client creation
this.clientSupplier = clientSupplier;
adminClient = clientSupplier.getAdmin(applicationConfigs.getAdminConfigs(...));
// ... named topology check happens later in StreamThread.create()

// After: validation before admin client creation
this.clientSupplier = clientSupplier;
throwIfUnsupportedFeatureIsUsedWithStreamsRebalanceProtocol();
adminClient = clientSupplier.getAdmin(applicationConfigs.getAdminConfigs(...));
```

**How to spot**
When a new protocol mode is gated by a config flag, check that all incompatible features are explicitly enumerated and validated at startup, before any heavyweight resources are allocated. Missing early validation forces users to discover incompatibilities through runtime failures rather than clear configuration errors.

**Test**
`shouldThrowIfPatternSubscriptionUsedWithStreamsProtocol`: constructs a topology with a pattern source and a `group.protocol=streams` config, asserts `UnsupportedOperationException` is thrown. `shouldLogWarningIfNonDefaultClientSupplierUsedWithStreamsProtocol`: asserts a WARN log message when a `MockClientSupplier` is passed.
