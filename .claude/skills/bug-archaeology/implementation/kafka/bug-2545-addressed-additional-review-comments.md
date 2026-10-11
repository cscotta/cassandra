**Commit**: b63b20eeee · **Ticket**: KAFKA-16749
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.ShareFetchResponseData;
        this(
            replicaManager,
    public CompletableFuture<Map<TopicIdPartition, ShareFetchResponseData.PartitionData>> fetchMessages(
        CompletableFuture<Map<TopicIdPartition, ShareFetchResponseData.PartitionData>> future = new CompletableFuture<>();
    CompletableFuture<Map<TopicIdPartition, ShareFetchResponseData.PartitionData>> processFetchResponse(
                    ShareFetchResponseData.PartitionData partitionData = new ShareFetchResponseData.PartitionData()
            Map<TopicIdPartition, ShareFetchResponseData.PartitionData> processedResult = new HashMap<>();
        // TODO: We need to know the isolation level from group configs, for now we are passing Option.empty() for isolationLevel
        private final CompletableFuture<Map<TopicIdPartition, ShareFetchResponseData.PartitionData>> future;
                                       CompletableFuture<Map<TopicIdPartition, ShareFetchResponseData.PartitionData>> future,

// After
        this(replicaManager,
    public CompletableFuture<Map<TopicIdPartition, PartitionData>> fetchMessages(
        CompletableFuture<Map<TopicIdPartition, PartitionData>> future = new CompletableFuture<>();
            shareFetchPartitionData.future.completeExceptionally(e);
    CompletableFuture<Map<TopicIdPartition, PartitionData>> processFetchResponse(
                    PartitionData partitionData = new PartitionData()
            Map<TopicIdPartition, PartitionData> processedResult = new HashMap<>();
        // Isolation level is only required when reading from the latest offset hence use Option.empty() for now.
        private final CompletableFuture<Map<TopicIdPartition, PartitionData>> future;

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16749: [minor] addressed additional review comments (#16890).

**Test**
No test added.
