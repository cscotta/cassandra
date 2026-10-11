**Commit**: f3dbd7ed08 · **Ticket**: KAFKA-16904
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Satish Duggana <satishd@apache.org>, Christo Lolov <lolovc@amazon.com>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
        metricsGroup.newGauge(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC.getName(), new Gauge<Double>() {
        metricsGroup.removeMetric(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC.getName());
        return remoteStorageReaderThreadPool.submit(new RemoteLogReader(fetchInfo, this, callback, brokerTopicStats, rlmFetchQuotaManager));
                           RLMQuotaManager quotaManager) {

            FetchDataInfo fetchDataInfo = rlm.read(fetchInfo);

// After
import com.yammer.metrics.core.Timer;
import static org.apache.kafka.server.log.remote.storage.RemoteStorageMetrics.REMOTE_LOG_READER_FETCH_RATE_AND_TIME_METRIC;
    private final Timer remoteReadTimer;
        metricsGroup.newGauge(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC, new Gauge<Double>() {
        remoteReadTimer = metricsGroup.newTimer(REMOTE_LOG_READER_FETCH_RATE_AND_TIME_METRIC,
                TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        metricsGroup.removeMetric(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC);
        metricsGroup.removeMetric(REMOTE_LOG_READER_FETCH_RATE_AND_TIME_METRIC);
        return remoteStorageReaderThreadPool.submit(
                new RemoteLogReader(fetchInfo, this, callback, brokerTopicStats, rlmFetchQuotaManager, remoteReadTimer));
import com.yammer.metrics.core.Timer;
    private final Timer remoteReadTimer;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16904: metric to measure the latency of remote read requests (#16209).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
