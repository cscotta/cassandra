**Commit**: 5316f436d7 · **Ticket**: KAFKA-18615
**Subsystem**: docs/streams
**Category**: Incorrect filtering / wrong query result

**Root cause**
This PR implements KAFKA-18615 by adding a windowSum aggregation that
computes the sum of values over a time window, so that `commit-ratio`,
`poll-ratio`, `process-ratio`, and `punctuate-ratio` represent the ratio
of the `{action}` over a window duration rather than a single iteration. The effective window duration is whatever you configure for metrics:
- `metrics.sample.window.ms` (per-sample window length)
- `times metrics.num.samples` (number of rolling windows)

With the default Kafka metrics config, that is typically:
`metrics.sample.window.ms = 30000 ms`  `metrics.num.samples = 2`  → ~60
seconds total rolling window. Reviewers: Matthias J. Sax <matthias@confluent.io>, Bill Bejeck
 <bil...

**Before/After**
```java
// Before
        processRatioSensor.record((double) totalProcessLatency / runOnceLatency, now);
        punctuateRatioSensor.record((double) totalPunctuateLatency / runOnceLatency, now);
        po

// After
    <h3><a id="streams_api_changes_430" href="#streams_api_changes_430">Streams API changes in 4.3.0</a></h3>

    <p>The streams thread metrics <code>commit-ratio</code>, <code>process-ratio</code>, <code>punctuate-ratio</code>, and <code>poll-ratio</code> have been updated.
        They now report, over a rolling measurement window,
        the ratio of time this thread spends performing the given action (<code>{action}</code>) to the total elapsed time in that window.
        The effective window duration is determined by the metrics configuration: <code>metrics.sample.window.ms</code> (per-sample window length)
        and <code>metrics.num.samples</code> (number of rolling windows).
    </p>

import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.stats.WindowedSum;
    private final WindowedSum pollLatencyWindowedSum = new WindowedSum();
```

**How to spot**
When reviewing docs/streams code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18615: streamthread *-ratio metrics suffer from sampling bias (#21160).

**Test**
Test modified in ThreadMetricsTest.java covering the corrected behaviour.
