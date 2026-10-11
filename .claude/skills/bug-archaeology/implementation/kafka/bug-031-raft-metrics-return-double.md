**Commit**: a899cfc0f8 · **Ticket**: KAFKA-18679
**Subsystem**: raft/src/main/java/org/apache/kafka/raft/internals/KafkaRaftMetrics.java
**Category**: Wrong serialization / deserialization

**Root cause**
Several KRaft metrics gauges in `KafkaRaftMetrics` were registered without an explicit generic type parameter on the lambda, causing Java to infer `Gauge<Object>` and box the return value as a `Double` at the JMX/metrics layer even though the values were logically `Integer` or `Long`. For example, `log-end-offset` (a `long`) was returned as a `double`, making programmatic consumers that called `.metricValue()` and cast to `Long` throw a `ClassCastException`.

**Before/After**
```java
// Before – inferred as Gauge<Object>, returns Double
metrics.addMetric(logEndOffsetMetricName,
    (mConfig, currentTimeMs) -> logEndOffset.offset());

// After – explicit Gauge<Long>
metrics.addMetric(logEndOffsetMetricName,
    (Gauge<Long>) (mConfig, currentTimeMs) -> logEndOffset.offset());
```

**How to spot**
Lambda-based `Gauge` registrations without explicit type witnesses silently produce `Double` wrappers. Grep for `metrics.addMetric(.*->` and check whether the lambda's return type is `int`/`long`; if no `(Gauge<Long>)` or `(Gauge<Integer>)` cast is present, the metric will be a `Double`.

**Test**
`KafkaRaftMetricsTest` and `KafkaRaftClientTest` updated to assert exact typed values (e.g. `assertEquals(1L, ...)` rather than `assertEquals((double) 1L, ...)`).
