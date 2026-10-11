**Commit**: bd9d68f24a · **Ticket**: KAFKA-15265
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Luke Chen <showuon@gmail.com>, Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
        return remoteStorageReaderThreadPool.submit(new RemoteLogReader(fetchInfo, this, callback, brokerTopicStats));
                           BrokerTopicStats brokerTopicStats) {
        // For consume fetch requests, create a dummy FetchDataInfo with the remote storage fetch information.
        // For the first topic-partition that needs remote data, we will use this information to read the data in another thread.
        val fetchDataInfo =
        new FetchDataInfo(new LogOffsetMetadata(offset), MemoryRecords.EMPTY, false, Optional.empty(),
          Optional.of(new RemoteStorageFetchInfo(adjustedMaxBytes, minOneMessage, tp.topicPartition(),
            fetchInfo, params.isolation, params.hardMaxBytesLimit())))

// After
    public boolean isRemoteLogFetchQuotaExceeded() {
        return rlmFetchQuotaManager.isQuotaExceeded();
    }

        return remoteStorageReaderThreadPool.submit(new RemoteLogReader(fetchInfo, this, callback, brokerTopicStats, rlmFetchQuotaManager));
import kafka.log.remote.quota.RLMQuotaManager;
    private final RLMQuotaManager quotaManager;
                           BrokerTopicStats brokerTopicStats,
                           RLMQuotaManager quotaManager) {
        this.quotaManager = quotaManager;
        quotaManager.record(result.fetchDataInfo.map(fetchDataInfo -> fetchDataInfo.records.sizeInBytes()).orElse(0));
        val fetchDataInfo = if (remoteLogManager.get.isRemoteLogFetchQuotaExceeded) {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: integrate rlmquotamanager for throttling fetches from remote storage (#16071).

**Test**
Test modified in RemoteLogReaderTest.java covering the corrected behaviour.
