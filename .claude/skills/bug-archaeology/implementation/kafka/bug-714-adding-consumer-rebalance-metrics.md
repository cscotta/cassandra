**Commit**: 2da02d9fcf · **Ticket**: KAFKA-19723
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Added Testcases for consumer rebalance metric manager test. Reviewers: Lianet Magrans <lmagrans@confluent.io>, TengYao Chi
 <frankvicky@apache.org>, Hong-Yi Chen <apalan60@gmail.com>

**Before/After**
```java
// Before
    private final Time time = new MockTime();
    private final Metrics metrics = new Metrics(time);
        SubscriptionState subscriptionState = new SubscriptionState(mock(LogContext.class), AutoOffsetResetStrategy.EARLIEST);
        ConsumerRebalanceMetricsManager consumerRebalanceMetricsManager = new ConsumerRebalanceMetricsManager(metrics, subscriptionState);

        assertNotNull(metrics.metric(consumerRebalanceMetricsManager.assignedPartitionsCount), "Metric assigned-partitions has not been registered as expected");
        assertEquals(2.0d, metrics.metric(consumerRebalanceMetricsManager.assignedPartitionsCount).metricValue());
        assertEquals(0.0d, metrics.metric(consumerRebalanceMetricsManager.assignedPartitionsCount).metricValue());
        assertEquals(0.0d, metrics.metric(consumerRebalanceMetricsManager.assignedPartitionsCount).metricValue());
        assertEquals(1.0d, metrics.metric(consumerRebalanceMetricsManager.assignedPartitionsCount).metricValue());

// After
import org.apache.kafka.common.metrics.MetricConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
    private Time time;
    private Metrics metrics;
    private SubscriptionState subscriptionState;
    private ConsumerRebalanceMetricsManager metricsManager;
    private MetricConfig metricConfig;
    private long windowSizeMs;
    private int numSamples;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19723 adding consumer rebalance metrics test (#20565).

**Test**
Test modified in ConsumerRebalanceMetricsManagerTest.java covering the corrected behaviour.
