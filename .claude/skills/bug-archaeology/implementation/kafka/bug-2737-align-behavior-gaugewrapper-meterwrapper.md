**Commit**: 0b11971f2c · **Ticket**: KAFKA-17016
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import com.yammer.metrics.core.{Gauge, Meter}
    @volatile private var gaugeObject: Gauge[Long] = _
    final private val gaugeLock = new Object
    final val aggregatedMetric = new AggregatedMetric()
    def gauge(): Gauge[Long] = gaugeLock synchronized {
      if (gaugeObject == null) {
        gaugeObject = metricsGroup.newGauge(metricType, () => aggregatedMetric.value(), tags)
      }
      return gaugeObject
    def close(): Unit = gaugeLock synchronized {
      if (gaugeObject != null) {
        metricsGroup.removeMetric(metricType, tags)

// After
import com.yammer.metrics.core.Meter
    // The map to store:
    //   - per-partition value for topic-level metrics. The key will be the partition number
    //   - per-topic value for broker-level metrics. The key will be the topic name
    private val metricValues = new ConcurrentHashMap[String, Long]()

    def setValue(key: String, value: Long): Unit = {
      newGaugeIfNeed()
      metricValues.put(key, value)
    }
    def removeKey(key: String): Unit = {
      newGaugeIfNeed()
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17016 align the behavior of gaugewrapper and meterwrapper (#16426).

**Test**
Test modified in KafkaRequestHandlerTest.scala covering the corrected behaviour.
