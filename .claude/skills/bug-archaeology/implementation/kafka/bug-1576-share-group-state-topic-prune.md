**Commit**: 06a5e258e4 · **Ticket**: KAFKA-18232
**Subsystem**: share-coordinator/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            new ShareCoordinatorMetrics(),

// After
                        shareCoordinatorMetrics.recordPrune(
                            off,
                            tp
                        );
import org.apache.kafka.common.metrics.stats.Value;
    public static final String SHARE_COORDINATOR_STATE_TOPIC_PRUNE_SENSOR_NAME = "ShareCoordinatorStateTopicPruneSensorName";
    private Map<TopicPartition, ShareGroupPruneMetrics> pruneMetrics = new ConcurrentHashMap<>();
        pruneMetrics.values().forEach(v -> metrics.removeSensor(v.pruneSensor.name()));

    public void recordPrune(double value, TopicPartition tp) {
        pruneMetrics.computeIfAbsent(tp, k -> new ShareGroupPruneMetrics(tp))
            .pruneSensor.record(value);
```

**How to spot**
When reviewing share-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18232: add share group state topic prune metrics. (#18174).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
