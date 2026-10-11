**Commit**: f6698146ce · **Ticket**: KAFKA-20368
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Summary: use `MetricsUtils.getTags` to handle

[metricTags](https://github.com/apache/kafka/blob/5a2d79a1ffb31b0ebb8c8b13c4ac6e5a2af7cbce/storage/src/main/java/org/apache/kafka/server/log/remote/storage/RemoteLogManager.java#L1145)
in RLMExpirationTask. Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        private final Map<String, String> metricTags = new HashMap<>();
            metricTags.put("topic", topicIdPartition.topic());
            metricTags.put("partition", Integer.toString(topicIdPartition.partition()));
        String retentionMetricName = "name=RetentionSizeInPercent,partition=" + leaderTopicIdPartition.partition() + ",topic=" + leaderTopicIdPartition.topic();
        String localRetentionMetricName = "name=LocalRetentionSizeInPercent,partition=" + leaderTopicIdPartition.partition() + ",topic=" + leaderTopicIdPartition.topic();
        String retentionMetricName = "name=RetentionSizeInPercent,partition=" + leaderTopicIdPartition.partition() + ",topic=" + leaderTopicIdPartition.topic();
        String localRetentionMetricName = "name=LocalRetentionSizeInPercent,partition=" + leaderTopicIdPartition.partition() + ",topic=" + leaderTopicIdPartition.topic();
        String retentionMetricName = "name=RetentionSizeInPercent,partition=" + leaderTopicIdPartition.partition() +

// After
import org.apache.kafka.common.metrics.internals.MetricsUtils;
        private final Map<String, String> metricTags;
            this.metricTags = MetricsUtils.getTags("topic", topicIdPartition.topic(),
                    "partition", Integer.toString(topicIdPartition.partition()));
        String retentionMetricName = "name=RetentionSizeInPercent,topic=" + leaderTopicIdPartition.topic() + ",partition=" + leaderTopicIdPartition.partition();
        String localRetentionMetricName = "name=LocalRetentionSizeInPercent,topic=" + leaderTopicIdPartition.topic() + ",partition=" + leaderTopicIdPartition.partition();
        String retentionMetricName = "name=RetentionSizeInPercent,topic=" + leaderTopicIdPartition.topic() + ",partition=" + leaderTopicIdPartition.partition();
        String localRetentionMetricName = "name=LocalRetentionSizeInPercent,topic=" + leaderTopicIdPartition.topic() + ",partition=" + leaderTopicIdPartition.partition();

        // Verify JMX metrics are actually deregistered from the Yammer registry after cancellation
        assertThrows(NoSuchElementException.class, () -> yammerMetricValue(retentionMetricName));
        assertThrows(NoSuchElementException.class, () -> yammerMetricValue(localRetentionMetricName));
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20368 use linkedhashmap for deterministic metric tags order in rlmexpirationtask (#21931).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
