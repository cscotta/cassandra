**Commit**: c7619ef8d1 · **Ticket**: KAFKA-17951
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before
        Map<TopicIdPartition, Integer> partitionMaxBytes
        processShareFetch(new ShareFetch(fetchParams, groupId, memberId, future, partitionMaxBytes, batchSize, maxFetchRecords, brokerTopicStats));
    val fetchResult: CompletableFuture[Map[TopicIdPartition, ShareFetchResponseData.PartitionData]] =
      handleFetchFromShareFetchRequest(
import java.util.HashMap;
        Map<TopicIdPartition, Integer> partitionMaxBytes = new HashMap<>();
        partitionMaxBytes.put(tp0, PARTITION_MAX_BYTES);
        partitionMaxBytes.put(tp1, PARTITION_MAX_BYTES);

// After
import org.apache.kafka.server.share.fetch.PartitionRotateStrategy;
import org.apache.kafka.server.share.fetch.PartitionRotateStrategy.PartitionRotateMetadata;
        int sessionEpoch,
        LinkedHashMap<TopicIdPartition, Integer> partitionMaxBytes
        LinkedHashMap<TopicIdPartition, Integer> topicIdPartitions = PartitionRotateStrategy
            .type(PartitionRotateStrategy.StrategyType.ROUND_ROBIN)
            .rotate(partitionMaxBytes, new PartitionRotateMetadata(sessionEpoch));

        processShareFetch(new ShareFetch(fetchParams, groupId, memberId, future, topicIdPartitions, batchSize, maxFetchRecords, brokerTopicStats));
    val fetchResult: CompletableFuture[Map[TopicIdPartition, ShareFetchResponseData.PartitionData]] = handleFetchFromShareFetchRequest(
      shareSessionEpoch,
                                       shareSessionEpoch: Int,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17951: share parition rotate strategy (#18651).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
