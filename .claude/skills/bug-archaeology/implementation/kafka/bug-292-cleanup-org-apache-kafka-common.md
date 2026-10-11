**Commit**: a1c155ee28 · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This PR removes `CollectionUtils` entirely (KAFKA-20297) by replacing
all usages with alternatives. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.CollectionUtils;
        DescribeProducersRequest.Builder builder = new DescribeProducersRequest.Builder(request);
        CollectionUtils.groupPartitionsByTopic(
            topicPartitions,
            builder::addTopic,
            (topicRequest, partitionId) -> topicRequest.partitionIndexes().add(partitionId)
        );
        return builder;
import org.apache.kafka.common.utils.CollectionUtils;
        Map<String, ListOffsetsTopic> topicsByName = CollectionUtils.groupPartitionsByTopic(
            keys,
            topicName -> new ListOffsetsTopic().setName(topicName),

// After
        for (TopicPartition tp : topicPartitions) {
            DescribeProducersRequestData.TopicRequest topicRequest = request.topics().find(tp.topic());
            if (topicRequest == null) {
                topicRequest = new DescribeProducersRequestData.TopicRequest().setName(tp.topic());
                request.topics().add(topicRequest);
            }
            topicRequest.partitionIndexes().add(tp.partition());
        }
        return new DescribeProducersRequest.Builder(request);
        Map<String, ListOffsetsTopic> topicsByName = new HashMap<>();
        for (TopicPartition topicPartition : keys) {
            ListOffsetsTopic topic = topicsByName.computeIfAbsent(
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 cleanup `org.apache.kafka.common.utils.collectionutils` (#21818).

**Test**
Test modified in DescribeProducersHandlerTest.java covering the corrected behaviour.
