**Commit**: 932759bd70 · **Ticket**: KAFKA-16731
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Added ShareGroupMetrics inner class to SharePartitionManager. Added code to record metrics at various checkpoints in code. Reviewers:  Andrew Schofield <aschofield@confluent.io>,Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        Persister persister
            persister
        Persister persister
            Persister persister
                    k -> new SharePartition(shareFetchPartitionData.groupId, topicIdPartition, maxInFlightMessages, maxDeliveryCount,
                        recordLockDurationMs, timer, time, persister));

// After
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.Max;
import org.apache.kafka.common.metrics.stats.Meter;
    /**
     * Class with methods to record share group metrics.
     */
    private final ShareGroupMetrics shareGroupMetrics;

        Persister persister,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16731: added share group metrics class. (#16488).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
