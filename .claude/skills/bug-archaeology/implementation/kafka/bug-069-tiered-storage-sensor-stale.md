**Commit**: 7dba91d025 · **Ticket**: KAFKA-19484
**Subsystem**: storage/src/main/java/org/apache/kafka/server/log/remote/quota/RLMQuotaMetrics.java
**Category**: State not cleaned up

**Root cause**
`RLMQuotaMetrics` obtained a `Sensor` once at construction time and cached it. Kafka's `Metrics` framework expires inactive sensors after a configurable timeout and removes them. Once the sensor was evicted (because no tiered-storage copy or fetch activity had occurred for a while), the cached reference became stale — recording to it had no effect, and the metrics stopped reporting. The fix removes the cached `Sensor` field; instead, `sensor()` calls `SensorAccess.getOrCreate()` each time, which transparently recreates the sensor if it has been evicted.

**Before/After**
```java
// Before: sensor obtained once and cached — becomes stale after expiry
public RLMQuotaMetrics(...) {
    SensorAccess sensorAccess = new SensorAccess(lock, metrics);
    this.sensor = sensorAccess.getOrCreate(name, expirationTime, s -> { ... });
}
public Sensor sensor() { return sensor; }

// After: sensor re-fetched (or recreated) on every call
public Sensor sensor() {
    return sensorAccess.getOrCreate(name, expirationTime, s -> { ... });
}
```

**How to spot**
When a `Sensor` object is stored in a field, ask whether the sensor can be evicted by the metrics framework. If the expiration time is finite, the cached reference will eventually go stale. Either disable expiration or delegate to `SensorAccess.getOrCreate()` each time.

**Test**
`RLMQuotaMetricsTest.testNewSensorWhenExpired` manually removes the sensor via `metrics.removeSensor()`, then calls `rlmQuotaMetrics.sensor()` again and asserts a different (new) sensor is returned.
