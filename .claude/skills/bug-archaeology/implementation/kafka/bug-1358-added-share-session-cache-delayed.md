**Commit**: a6c53d0c37 · **Ticket**: KAFKA-18878
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR implements the ShareSessionCache and DelayedShareFetchMetrics as
defined in KIP-1103. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.server.metrics.KafkaMetricsGroup;
import com.yammer.metrics.core.Meter;

import java.util.concurrent.TimeUnit;
    private static final String EXPIRES_PER_SEC = "ExpiresPerSec";

    /**
     * Metric for the rate of expired delayed fetch requests.
     */
    private final Meter expiredRequestMeter;
        // Register metrics for DelayedShareFetch.
        KafkaMetricsGroup metricsGroup = new KafkaMetricsGroup("kafka.server", "DelayedShareFetchMetrics");
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18878: added share session cache and delayed share fetch metrics (kip-1103) (#19059).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
