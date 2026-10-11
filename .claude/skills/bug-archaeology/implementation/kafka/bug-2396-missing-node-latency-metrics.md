**Commit**: 0c4ffc682c · **Ticket**: KAFKA-17231
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This is the equivalent of #16755 for the share group consumer. The node request-latency-max and request-latency-avg were not being recorded and thus reported as NaN for the share group consumer. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            metricsManager.recordLatency(resp.requestLatencyMs());

                metricsManager.recordLatency(resp.requestLatencyMs());
                            metricsManager.recordLatency(resp.requestLatencyMs());
                    metricsManager.recordLatency(resp.requestLatencyMs());
    void recordLatency(long requestLatencyMs) {


// After
            metricsManager.recordLatency(resp.destination(), resp.requestLatencyMs());

            metricsManager.recordLatency(resp.destination(), resp.requestLatencyMs());
    private final Metrics metrics;
        this.metrics = metrics;

    void recordLatency(String node, long requestLatencyMs) {
        if (!node.isEmpty()) {
            String nodeTimeName = "node-" + node + ".latency";
            Sensor nodeRequestTime = metrics.getSensor(nodeTimeName);
            if (nodeRequestTime != null)
                nodeRequestTime.record(requestLatencyMs);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17231 add missing node latency metrics (#17137).

**Test**
Test modified in ShareFetchMetricsManagerTest.java covering the corrected behaviour.
