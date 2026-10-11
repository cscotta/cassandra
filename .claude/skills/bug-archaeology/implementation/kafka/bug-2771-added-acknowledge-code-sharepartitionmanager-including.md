**Commit**: e55c28c60b · **Ticket**: KAFKA-16750
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
About
This PR adds acknowledge code in SharePartitionManager. Internally, the record acknowledgements happen at the SharePartition level. SharePartitionManager identifies the SharePartitions and calls their acknowledge method to actually acknowledge the individual records

Testing
Added unit tests to cover the new functionality added in SharePartitionManagerTest

Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        CompletableFuture<Map<TopicIdPartition, ShareAcknowledgeResponseData.PartitionData>> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Not implemented yet"));

        return future;

// After
    <suppress checks="ClassDataAbstractionCoupling" files="SharePartitionManagerTest"/>
        Map<TopicIdPartition, CompletableFuture<Errors>> futures = new HashMap<>();
        acknowledgeTopics.forEach((topicIdPartition, acknowledgePartitionBatches) -> {
            SharePartition sharePartition = partitionCacheMap.get(sharePartitionKey(groupId, topicIdPartition));
            if (sharePartition != null) {
                CompletableFuture<Errors> future = sharePartition.acknowledge(memberId, acknowledgePartitionBatches).thenApply(throwable -> {
                    if (throwable.isPresent()) {
                        return Errors.forException(throwable.get());
                    }
                    return Errors.NONE;
                });
                futures.put(topicIdPartition, future);
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16750: added acknowledge code in sharepartitionmanager including unit tests (#16457).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
