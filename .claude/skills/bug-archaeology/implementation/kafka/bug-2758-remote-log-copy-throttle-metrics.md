**Commit**: 641469e4ac · **Ticket**: KAFKA-17069
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As part of KIP-956, we have added quota for remote copies to remote storage. In this PR, we are adding the following metrics for remote copy throttling. 1. remote-copy-throttle-time-avg 	The average time in millis remote copies was throttled by a broker
2.

**Before/After**
```java
// Before
                                while (rlmCopyQuotaManager.getThrottleTimeMs() > 0) {

// After
    private final Sensor copyThrottleTimeSensor;
        copyThrottleTimeSensor = new RLMQuotaMetrics(metrics, "remote-copy-throttle-time", RemoteLogManager.class.getSimpleName(),
            "The %s time in millis remote copies was throttled by a broker", INACTIVE_SENSOR_EXPIRATION_TIME_SECONDS).sensor();
                                long throttleTimeMs = rlmCopyQuotaManager.getThrottleTimeMs();
                                while (throttleTimeMs > 0) {
                                    copyThrottleTimeSensor.record(throttleTimeMs, time.milliseconds());
                                    throttleTimeMs = rlmCopyQuotaManager.getThrottleTimeMs();
import org.apache.kafka.common.metrics.KafkaMetric;
    private final long quotaExceededThrottleTime = 1000L;
    private final long quotaAvailableThrottleTime = 0L;
        when(rlmCopyQuotaManager.getThrottleTimeMs()).thenReturn(quotaAvailableThrottleTime);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17069: remote log copy throttle metrics (#16086).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
