**Commit**: ae192bdd41 · **Ticket**: KAFKA-16754
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The release API exposed Partitions which should be an internal implementation detail for releaseAcquiredRecords API. Also lessen the scope for cached topic partitions method as it's not needed. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.ShareSessionNotFoundException;
            futures.forEach((topicIdPartition, future) -> {
                result.put(topicIdPartition, new ShareAcknowledgeResponseData.PartitionData()
                    .setPartitionIndex(topicIdPartition.partition())
                    .setErrorCode(future.join().code()));
            });
     * @param topicIdPartitions The topic-partitions to release the acquired records for.
        String memberId,
        List<TopicIdPartition> topicIdPartitions
        log.trace("Release acquired records request for topicIdPartitions: {} with groupId: {}",
            topicIdPartitions, groupId);
     * @return The list of cached topic-partitions in the share session.

// After
            futures.forEach((topicIdPartition, future) -> result.put(topicIdPartition, new ShareAcknowledgeResponseData.PartitionData()
                .setPartitionIndex(topicIdPartition.partition())
                .setErrorCode(future.join().code())));
        String memberId
        log.trace("Release acquired records request for groupId: {}, memberId: {}", groupId, memberId);
        List<TopicIdPartition> topicIdPartitions = cachedTopicIdPartitionsInShareSession(
            groupId, Uuid.fromString(memberId));
        if (topicIdPartitions.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

     * @return The list of cached topic-partitions in the share session if present, otherwise an empty list.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16754: removing partitions from release api (kip-932) (#16513).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
