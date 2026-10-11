**Commit**: 902fc33b27 · **Ticket**: KAFKA-17230
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Kafka Consumer client registers node/connection latency metrics in Selector.java but the values against the metric is never recorded. This seems to be an issue since inception. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Andrew Schofield <aschofield@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
            metricsManager.recordLatency(resp.requestLatencyMs());
    void recordLatency(long requestLatencyMs) {
        metricsManager.recordLatency(123);
        metricsManager.recordLatency(456);
        KafkaMetric metric = metrics.metric(metricName);
        return (Double) metric.metricValue();

// After
            metricsManager.recordLatency(resp.destination(), resp.requestLatencyMs());
    void recordLatency(String node, long requestLatencyMs) {
        if (!node.isEmpty()) {
            String nodeTimeName = "node-" + node + ".latency";
            Sensor nodeRequestTime = this.metrics.getSensor(nodeTimeName);
            if (nodeRequestTime != null)
                nodeRequestTime.record(requestLatencyMs);
        }
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.Max;
        metricsManager.recordLatency("", 123);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17230: fix for consumer node latency metrics (#16755).

**Test**
Test modified in FetchMetricsManagerTest.java covering the corrected behaviour.
