**Commit**: 04567cdb22 · **Ticket**: KAFKA-18657
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE,
            partitionMaxBytes);
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE,
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE,
        // Should have 1 fetch recorded and no failed as the fetch did complete.
        validateBrokerTopicStatsMetrics(
            brokerTopicStats,
            new TopicMetrics(1, 0, 0, 0),
            Map.of("foo", new TopicMetrics(1, 0, 0, 0))
        );
        validateBrokerTopicStatsMetrics(
            brokerTopicStats,

// After
import org.apache.kafka.common.test.api.Flaky;
        kafka.utils.TestUtils.clearYammerMetrics();
        CompletableFuture<Map<TopicIdPartition, PartitionData>> future = sharePartitionManager.fetchMessages(
            groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE, partitionMaxBytes);
        assertTrue(future.isDone());
        future = sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE,
        assertTrue(future.isDone());
        future = sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, BATCH_SIZE,
        assertTrue(future.isDone());
    @Flaky("KAFKA-18657")
        // Should have 1 fetch recorded.
        assertEquals(1, brokerTopicStats.allTopicsStats().totalShareFetchRequestRate().count());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18657: fixing sharepartitionmanager flaky test (#18710).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
