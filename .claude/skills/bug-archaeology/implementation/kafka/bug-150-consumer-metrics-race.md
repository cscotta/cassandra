**Commit**: c47d68ae4b · **Ticket**: KAFKA-20296
**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/PlaintextConsumerTest.java
**Category**: Race condition / missing synchronization

**Root cause**
The consumer metrics tests `testPerPartitionLeadMetrics` and `testPerPartitionLagMetrics` asserted that fetch-manager metrics were removed after unsubscribing from a topic, but they waited only for a rebalance to complete. Metrics are cleaned up during `prepareFetchRequests()`, which runs asynchronously on the next poll cycle — the rebalance callback fires before the fetch request is prepared. The test therefore checked for absent metrics immediately after the rebalance event, often finding them still present and failing intermittently.

**Before/After**
```java
// BEFORE – rebalance completion does not guarantee metrics removal
consumer.subscribe(List.of(topic2), listener);
awaitRebalance(consumer, listener);
assertNull(consumer.metrics().get(new MetricName("records-lead", ...)));

// AFTER – polls until metrics actually disappear
consumer.subscribe(List.of(topic2), listener);
awaitMetricsCleanup(consumer, "records-lead", tags1, tags2);

private void awaitMetricsCleanup(...) throws InterruptedException {
    TestUtils.waitForCondition(() -> {
        consumer.poll(Duration.ofMillis(100));   // drives prepareFetchRequests
        return consumer.metrics().get(metric1) == null
            && consumer.metrics().get(metric2) == null;
    }, "Metrics for removed partitions should be cleaned up");
}
```

**How to spot**
Integration tests that assert the absence of a metric immediately after a rebalance event are susceptible to this race. Metric removal that happens as a side effect of the next request-preparation cycle requires active polling to trigger. Replace `assertNull(metric)` + `awaitRebalance` with a polling helper that drives `consumer.poll()` until the condition is met.

**Test**
This commit is itself a flaky-test fix. The `awaitMetricsCleanup` helper actively polls the consumer until both per-partition metrics disappear, replacing the static post-rebalance assertion.
