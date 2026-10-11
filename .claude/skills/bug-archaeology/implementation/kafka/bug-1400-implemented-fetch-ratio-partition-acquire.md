**Commit**: f543eac4fe · **Ticket**: KAFKA-18733
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
PR implements the final set of ShareGroupMetrics,
RequestTopicPartitionsFetchRatio and TopicPartitionsAcquireTimeMs, as
defined in KIP-1103:
https://cwiki.apache.org/confluence/display/KAFKA/KIP-1103%3A+Additional+metrics+for+cooperative+consumption

Note: Metric `RequestTopicPartitionsFetchRatio` is calculated as
percentage as Histogram API doesn't record double. Reviewers: Andrew Schofield <aschofield@confluent.io>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before
import java.util.Collections;
     * This function constructs an instance of delayed share fetch operation for completing share fetch requests instantaneously or with delay.
     * @param shareFetch - The share fetch parameters of the share fetch request.
     * @param replicaManager - The replica manager instance used to read from log/complete the request.
     * @param exceptionHandler - The handler to complete share fetch requests with exception.
     * @param sharePartitions - The share partitions referenced in the share fetch request.
            LinkedHashMap<TopicIdPartition, SharePartition> sharePartitions) {
        this(shareFetch, replicaManager, exceptionHandler, sharePartitions, PartitionMaxBytesStrategy.type(PartitionMaxBytesStrategy.StrategyType.UNIFORM));
        PartitionMaxBytesStrategy partitionMaxBytesStrategy) {

// After
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.server.share.metrics.ShareGroupMetrics;
    private final ShareGroupMetrics shareGroupMetrics;
    private final Time time;
    // Tracks the start time to acquire any share partition for a fetch request.
    private long acquireStartTimeMs;
     * This function constructs an instance of delayed share fetch operation for completing share fetch
     * requests instantaneously or with delay.
     *
     * @param shareFetch The share fetch parameters of the share fetch request.
     * @param replicaManager The replica manager instance used to read from log/complete the request.
     * @param exceptionHandler The handler to complete share fetch requests with exception.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18733: implemented fetch ratio and partition acquire time metrics (3/n) (#18959).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
