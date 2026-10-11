**Commit**: fa54065298 · **Ticket**: KAFKA-18086
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* KAFKA-18086: Enable propagation of the error message when writing state

* Propagate the error message in the writing state when calling SharePartitionManager.acknowledge and SharePartitionManager.releaseSession, and add corresponding tests to verify that the expected error message is propagated. * Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
        Map<TopicIdPartition, CompletableFuture<Errors>> futures = new HashMap<>();
                CompletableFuture<Errors> future = new CompletableFuture<>();
                        future.complete(Errors.forException(throwable));
                    future.complete(Errors.NONE);
                futures.put(topicIdPartition, CompletableFuture.completedFuture(Errors.UNKNOWN_TOPIC_OR_PARTITION));
            futures.forEach((topicIdPartition, future) -> result.put(topicIdPartition, new ShareAcknowledgeResponseData.PartitionData()
                .setPartitionIndex(topicIdPartition.partition())
                .setErrorCode(future.join().code())));
        Map<TopicIdPartition, CompletableFuture<Errors>> futuresMap = new HashMap<>();
                futuresMap.put(topicIdPartition, CompletableFuture.completedFuture(Errors.UNKNOWN_TOPIC_OR_PARTITION));
                CompletableFuture<Errors> future = new CompletableFuture<>();
                        future.complete(Errors.forException(throwable));

// After
        Map<TopicIdPartition, CompletableFuture<Throwable>> futures = new HashMap<>();
                CompletableFuture<Throwable> future = new CompletableFuture<>();
                        future.complete(throwable);
                    future.complete(null);
                futures.put(topicIdPartition, CompletableFuture.completedFuture(Errors.UNKNOWN_TOPIC_OR_PARTITION.exception()));
            futures.forEach((topicIdPartition, future) -> {
                ShareAcknowledgeResponseData.PartitionData partitionData = new ShareAcknowledgeResponseData.PartitionData()
                    .setPartitionIndex(topicIdPartition.partition());
                Throwable t = future.join();
                if (t != null) {
                    partitionData.setErrorCode(Errors.forException(t).code())
                        .setErrorMessage(t.getMessage());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18086: enable propagation of the error message when writing state (#17980).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
