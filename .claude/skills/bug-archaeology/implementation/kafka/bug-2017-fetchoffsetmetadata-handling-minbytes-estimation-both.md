**Commit**: dfa5aa5484 · **Ticket**: KAFKA-18022
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
        // We are utilizing lock so that onComplete doesn't do a dirty read for global variables -
                maybeUpdateFetchOffsetMetadata(replicaManagerReadResponse);
        LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> partitionsMissingFetchOffsetMetadata = new LinkedHashMap<>();
            if (sharePartition.fetchOffsetMetadata().isEmpty()) {
                partitionsMissingFetchOffsetMetadata.put(topicIdPartition, partitionData);
        if (partitionsMissingFetchOffsetMetadata.isEmpty()) {
        return readFromLog(partitionsMissingFetchOffsetMetadata);
            sharePartition.updateFetchOffsetMetadata(Optional.of(replicaManagerLogReadResult.info().fetchOffsetMetadata));
            Optional<LogOffsetMetadata> optionalFetchOffsetMetadata = sharePartition.fetchOffsetMetadata();
     * We maintain the latest fetch offset metadata to estimate the minBytes requirement more efficiently.
    private Optional<LogOffsetMetadata> fetchOffsetMetadata;

// After
        // We are utilizing lock so that onComplete doesn't do a dirty read for instance variables -
                maybeUpdateFetchOffsetMetadata(topicPartitionData, replicaManagerReadResponse);
        LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> partitionsNotMatchingFetchOffsetMetadata = new LinkedHashMap<>();
            if (sharePartition.fetchOffsetMetadata(partitionData.fetchOffset).isEmpty()) {
                partitionsNotMatchingFetchOffsetMetadata.put(topicIdPartition, partitionData);
        if (partitionsNotMatchingFetchOffsetMetadata.isEmpty()) {
        return readFromLog(partitionsNotMatchingFetchOffsetMetadata);
        LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> topicPartitionData,
            sharePartition.updateFetchOffsetMetadata(
                topicPartitionData.get(topicIdPartition).fetchOffset,
                replicaManagerLogReadResult.info().fetchOffsetMetadata);
            Optional<LogOffsetMetadata> optionalFetchOffsetMetadata = sharePartition.fetchOffsetMetadata(partitionData.fetchOffset);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18022: fetchoffsetmetadata handling for minbytes estimation in both common/uncommon cases of share fetch (#17825).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
