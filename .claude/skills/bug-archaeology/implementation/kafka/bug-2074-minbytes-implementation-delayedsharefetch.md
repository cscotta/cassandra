**Commit**: a0d4cbec40 · **Ticket**: KAFKA-17743
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers:  Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.ShareFetchResponseData;
    private Map<TopicIdPartition, FetchRequest.PartitionData> topicPartitionDataFromTryComplete;
            SharePartitionManager sharePartitionManager) {
        this.topicPartitionDataFromTryComplete = new LinkedHashMap<>();
            topicPartitionDataFromTryComplete.keySet());
        if (topicPartitionDataFromTryComplete.isEmpty())
            topicPartitionData = topicPartitionDataFromTryComplete;
            Seq<Tuple2<TopicIdPartition, LogReadResult>> responseLogResult = replicaManager.readFromLog(
                shareFetchData.fetchParams(),
                CollectionConverters.asScala(
                    topicPartitionData.entrySet().stream().map(entry ->
                        new Tuple2<>(entry.getKey(), entry.getValue())).collect(Collectors.toList())

// After
import kafka.cluster.Partition;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.server.storage.log.FetchIsolation;
import org.apache.kafka.storage.internals.log.LogOffsetMetadata;
import org.apache.kafka.storage.internals.log.LogOffsetSnapshot;
    private Map<TopicIdPartition, FetchRequest.PartitionData> partitionsAcquired;
    private Map<TopicIdPartition, LogReadResult> partitionsAlreadyFetched;
    // The topic partitions that need to be completed for the share fetch request are given by sharePartitions.
    // sharePartitions is a subset of shareFetchData. The order of insertion/deletion of entries in sharePartitions is important.
    private final LinkedHashMap<TopicIdPartition, SharePartition> sharePartitions;
            SharePartitionManager sharePartitionManager,
            LinkedHashMap<TopicIdPartition, SharePartition> sharePartitions) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17743: add minbytes implementation to delayedsharefetch (#17539).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
