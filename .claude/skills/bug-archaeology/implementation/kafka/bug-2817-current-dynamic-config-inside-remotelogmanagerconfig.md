**Commit**: 4fe08f3b29 · **Ticket**: KAFKA-16976
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
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

// After
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
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16976 update the current/dynamic config inside remotelogmanagerconfig (#16394).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
