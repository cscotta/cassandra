**Commit**: 848e3d0092 · **Ticket**: KAFKA-19722
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Adding the missing metric to track the number of partitions assigned. This metric should be registered whenever the consumer is using a
groupId, and should track the number of partitions from the subscription
state, regardless of the subscription type (manual or automatic). This PR registers the missing metric as part of the
ConsumerRebalanceMetricsManager setup. This manager is created if there
is a group ID, and reused by the consumer membershipMgr and the
streamsMemberhipMgr, so we ensure that the metric is registered for the
new consumer and streams.

**Before/After**
```java
// Before
            new ConsumerRebalanceMetricsManager(metrics),
        metricsManager = new ConsumerRebalanceMetricsManager(metrics);
    synchronized int numAssignedPartitions() {
    public ConsumerRebalanceMetricsManager(Metrics metrics) {

// After
            new ConsumerRebalanceMetricsManager(metrics, subscriptions),
        metricsManager = new ConsumerRebalanceMetricsManager(metrics, subscriptionState);
    public synchronized int numAssignedPartitions() {
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.SubscriptionState;
import java.util.Collection;
import java.util.regex.Pattern;
    public final MetricName assignedPartitionsCount;
    private final Metrics metrics;
    public ConsumerRebalanceMetricsManager(Metrics metrics, SubscriptionState subscriptions) {
        this.metrics = metrics;
        assignedPartitions
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19722: adding missing metric assigned-partitions for new consumer (#20557).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
