**Commit**: 8abeaf3cb4 · **Ticket**: KAFKA-15265
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The below remote log configs can be configured dynamically:
1. remote.log.manager.copy.max.bytes.per.second
2. remote.log.manager.fetch.max.bytes.per.second and
3. remote.log.index.file.cache.total.size.bytes

If those values are configured dynamically, then during the broker restart, it ensures the dynamic values are loaded instead of the static values from the config.

**Before/After**
```java
// Before
    private final RemoteLogManagerConfig rlmConfig;
     * @param rlmConfig Configuration required for remote logging subsystem(tiered storage) at the broker level.
    public RemoteLogManager(RemoteLogManagerConfig rlmConfig,
        this.rlmConfig = rlmConfig;
        indexCache = new RemoteIndexCache(rlmConfig.remoteLogIndexFileCacheTotalSizeBytes(), remoteLogStorageManager, logDir);
        return new RLMQuotaManager(copyQuotaManagerConfig(rlmConfig), metrics, QuotaType.RLMCopy$.MODULE$,
        return new RLMQuotaManager(fetchQuotaManagerConfig(rlmConfig), metrics, QuotaType.RLMFetch$.MODULE$,
    static RLMQuotaManagerConfig copyQuotaManagerConfig(RemoteLogManagerConfig rlmConfig) {
        return new RLMQuotaManagerConfig(rlmConfig.remoteLogManagerCopyMaxBytesPerSecond(),
    static RLMQuotaManagerConfig fetchQuotaManagerConfig(RemoteLogManagerConfig rlmConfig) {
        return new RLMQuotaManagerConfig(rlmConfig.remoteLogManagerFetchMaxBytesPerSecond(),

// After
import kafka.server.KafkaConfig;
    private final KafkaConfig config;
     * @param config Configuration required for remote logging subsystem(tiered storage) at the broker level.
    public RemoteLogManager(KafkaConfig config,
        this.config = config;
        RemoteLogManagerConfig rlmConfig = config.remoteLogManagerConfig();
        indexCache = new RemoteIndexCache(config.remoteLogIndexFileCacheTotalSizeBytes(), remoteLogStorageManager, logDir);
        return new RLMQuotaManager(copyQuotaManagerConfig(config), metrics, QuotaType.RLMCopy$.MODULE$,
        return new RLMQuotaManager(fetchQuotaManagerConfig(config), metrics, QuotaType.RLMFetch$.MODULE$,
    static RLMQuotaManagerConfig copyQuotaManagerConfig(KafkaConfig config) {
        RemoteLogManagerConfig rlmConfig = config.remoteLogManagerConfig();
        return new RLMQuotaManagerConfig(config.remoteLogManagerCopyMaxBytesPerSecond(),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: reapply dynamic remote configs after broker restart (#16353).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
