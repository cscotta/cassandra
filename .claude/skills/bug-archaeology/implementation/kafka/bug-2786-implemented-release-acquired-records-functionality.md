**Commit**: 399949ebcf · **Ticket**: KAFKA-16751
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
About
Implemented releaseAcquiredRecords functionality in SharePartitionManager which will act as a bridge between the call from KafkaApis to SharePartition for releasing the acquired records when a share session gets closed. Testing
The added function has been tested with unit tests. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
        CompletableFuture<Map<TopicIdPartition, ShareAcknowledgeResponseData.PartitionData>> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Not implemented yet"));

        return future;

// After
        Map<TopicIdPartition, CompletableFuture<Errors>> futuresMap = new HashMap<>();
        topicIdPartitions.forEach(topicIdPartition -> {
            SharePartition sharePartition = partitionCacheMap.get(sharePartitionKey(groupId, topicIdPartition));
            if (sharePartition == null) {
                log.error("No share partition found for groupId {} topicPartition {} while releasing acquired topic partitions", groupId, topicIdPartition);
                futuresMap.put(topicIdPartition, CompletableFuture.completedFuture(Errors.UNKNOWN_TOPIC_OR_PARTITION));
            } else {
                CompletableFuture<Errors> future = sharePartition.releaseAcquiredRecords(memberId).thenApply(throwable -> {
                    if (throwable.isPresent()) {
                        return Errors.forException(throwable.get());
                    }
                    return Errors.NONE;
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16751: implemented release acquired records functionality in sharepartitionmanager  (#16446).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
