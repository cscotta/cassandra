**Commit**: b2da186f21 · **Ticket**: KAFKA-15265
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As part of [KIP-956](https://cwiki.apache.org/confluence/display/KAFKA/KIP-956+Tiered+Storage+Quotas), we have added quota for remote fetches from remote storage. In this PR, we are adding the following metrics for remote fetch throttling. remote-fetch-throttle-time-avg : The average time in millis remote fetches was throttled by a broker 
remote-fetch-throttle-time-max : The max time in millis remote fetches was throttled by a broker

Reviewers: Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Luke Chen <showuon@gmail.com>, Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
    public boolean isRemoteLogFetchQuotaExceeded() {
        return rlmFetchQuotaManager.isQuotaExceeded();
                                while (rlmCopyQuotaManager.isQuotaExceeded()) {
    public boolean isQuotaExceeded() {

// After
import kafka.log.remote.quota.RLMQuotaMetrics;
import org.apache.kafka.common.metrics.Sensor;
import static kafka.log.remote.quota.RLMQuotaManagerConfig.INACTIVE_SENSOR_EXPIRATION_TIME_SECONDS;
    private final Sensor fetchThrottleTimeSensor;
        fetchThrottleTimeSensor = new RLMQuotaMetrics(metrics, "remote-fetch-throttle-time", RemoteLogManager.class.getSimpleName(),
            "The %s time in millis remote fetches was throttled by a broker", INACTIVE_SENSOR_EXPIRATION_TIME_SECONDS).sensor();

    public long getFetchThrottleTimeMs() {
        return rlmFetchQuotaManager.getThrottleTimeMs();
    }

    public Sensor fetchThrottleTimeSensor() {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: remote fetch throttle metrics (#16087).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
