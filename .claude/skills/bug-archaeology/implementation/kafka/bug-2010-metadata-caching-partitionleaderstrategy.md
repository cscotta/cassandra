**Commit**: a592912ec9 · **Ticket**: KAFKA-17663
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Admin API operations have two phases: lookup and fulfilment. The lookup phase involves a METADATA request whose details depend upon the operation being performed. For some operations, the METADATA request can be quite expensive to serve. For example, if the user calls Admin.listOffsets for 1000 topics, the METADATA request will include all 1000 topics and the response will contain the leader information for all of these topics.

**Before/After**
```java
// Before
        SimpleAdminApiFuture<TopicPartition, DeletedRecords> future = DeleteRecordsHandler.newFuture(recordsToDelete.keySet());
        AdminApiFuture.SimpleAdminApiFuture<TopicPartition, ListOffsetsResultInfo> future =
            ListOffsetsHandler.newFuture(topicPartitionOffsets.keySet());
        AdminApiFuture.SimpleAdminApiFuture<TopicPartition, DescribeProducersResult.PartitionProducerState> future =
            DescribeProducersHandler.newFuture(topicPartitions);

// After
import org.apache.kafka.clients.admin.internals.PartitionLeaderStrategy;
    private final Map<TopicPartition, Integer> partitionLeaderCache;
        this.partitionLeaderCache = new HashMap<>();
        PartitionLeaderStrategy.PartitionLeaderFuture<DeletedRecords> future =
            DeleteRecordsHandler.newFuture(recordsToDelete.keySet(), partitionLeaderCache);
        PartitionLeaderStrategy.PartitionLeaderFuture<ListOffsetsResultInfo> future =
            ListOffsetsHandler.newFuture(topicPartitionOffsets.keySet(), partitionLeaderCache);
        PartitionLeaderStrategy.PartitionLeaderFuture<DescribeProducersResult.PartitionProducerState> future =
            DescribeProducersHandler.newFuture(topicPartitions, partitionLeaderCache);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17663 add metadata caching in partitionleaderstrategy (#17367).

**Test**
Test modified in PartitionLeaderStrategyIntegrationTest.java covering the corrected behaviour.
