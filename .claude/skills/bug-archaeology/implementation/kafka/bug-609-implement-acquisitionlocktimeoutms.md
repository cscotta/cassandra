**Commit**: ab6755c0c1 · **Ticket**: KAFKA-19815
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implement KafkaShareConsumer.acquisitionLockTimeoutMs as part of
KIP-1222. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
    private final ShareFetchMetricsAggregator metricAggregator;
                        final ShareFetchMetricsAggregator metricAggregator,
        this.metricAggregator = metricAggregator;
        metricAggregator.record(partition.topicPartition(), bytes, records);

// After
            assertEquals(Optional.empty(), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.empty(), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.empty(), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());
    final Optional<Integer> acquisitionLockTimeoutMs;
    private final ShareFetchMetricsAggregator metricsAggregator;
                        final Optional<Integer> acquisitionLockTimeoutMs,
                        final ShareFetchMetricsAggregator metricsAggregator,
        this.acquisitionLockTimeoutMs = acquisitionLockTimeoutMs;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19815: implement acquisitionlocktimeoutms (#20895).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
