**Commit**: 53543bcf63 · **Ticket**: KAFKA-18733
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Sushant Mahajan <smahajan@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.Max;
import org.apache.kafka.common.metrics.stats.Meter;
import java.util.Objects;
        Metrics metrics,
            metrics,
        Metrics metrics,
            metrics,
            Metrics metrics,

// After
    /**
     * The load start time is used to track the time taken to load the share partition.
     */
    private final long loadStartTimeMs;

        this.loadStartTimeMs = time.hiResClockMs();
    long loadStartTimeMs() {
        return loadStartTimeMs;
    }

import org.apache.kafka.server.share.metrics.ShareGroupMetrics;
            new ShareGroupMetrics(time),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18733: updating share group metrics (1/n) (#18826).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
