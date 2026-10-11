**Commit**: 27a3c75216 · **Ticket**: KAFKA-17488
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR simply StreamsMetricsImpl to avoid passing in the unused "metric version" parameter. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            applicationConfigs.getString(StreamsConfig.BUILT_IN_METRICS_VERSION_CONFIG),
                              final String builtInMetricsVersion,
        Objects.requireNonNull(builtInMetricsVersion, "Built-in metrics version cannot be null");
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(metrics, "test", StreamsConfig.METRICS_LATEST, time);
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(new Metrics(), "clientId", StreamsConfig.METRICS_LATEST, new MockTime());
            new StreamsMetricsImpl(new Metrics(), "test-client", StreamsConfig.METRICS_LATEST, time),
            new StreamsMetricsImpl(new Metrics(), "test-client", StreamsConfig.METRICS_LATEST, time),

// After
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(metrics, "test", time);
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(new Metrics(), "clientId", new MockTime());
            new StreamsMetricsImpl(new Metrics(), "test-client", time),
            new StreamsMetricsImpl(new Metrics(), "test-client", time),
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17488: cleanup (test) code for kafka streams "metric version" (#17182).

**Test**
Test modified in KStreamSessionWindowAggregateProcessorTest.java covering the corrected behaviour.
