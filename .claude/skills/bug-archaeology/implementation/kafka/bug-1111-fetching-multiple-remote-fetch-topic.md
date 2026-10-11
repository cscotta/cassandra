**Commit**: 81c3a285a4 · **Ticket**: KAFKA-19133
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
### About
This PR removes the limitation in remote storage fetch for share groups
of only performing remote fetch for a single topic partition in a share
fetch request. With this PR, share groups can now fetch multiple remote
storage topic partitions in a single share fetch request. ### Testing
I have followed the [AK

documentation](https://kafka.apache.org/documentation/#tiered_storage_config_ex)
to test my code locally (by adopting `LocalTieredStorage.java`) and
verify with the help of logs that remote storage is happening for
multiple topic partitions in a single share fetch request. Also,
verified it with the help of unit tests.

**Before/After**
```java
// Before
    private Optional<RemoteFetch> remoteFetchOpt;
     * @param remoteFetchOpt Optional containing an in-flight remote fetch object or an empty optional.
        Optional<RemoteFetch> remoteFetchOpt
        this.remoteFetchOpt = remoteFetchOpt;
            } else if (remoteFetchOpt.isPresent()) {
            responseData.forEach((topicIdPartition, logReadResult) ->
                shareFetchPartitionDataList.add(new ShareFetchPartitionData(
                    topicIdPartition,
                    topicPartitionData.get(topicIdPartition),
                    logReadResult.toFetchPartitionData(false)
                ))
            );

// After
import static kafka.server.share.PendingRemoteFetches.RemoteFetch;

    private Optional<PendingRemoteFetches> pendingRemoteFetchesOpt;
     * @param pendingRemoteFetchesOpt Optional containing an in-flight remote fetch object or an empty optional.
        Optional<PendingRemoteFetches> pendingRemoteFetchesOpt
        this.pendingRemoteFetchesOpt = pendingRemoteFetchesOpt;
            } else if (pendingRemoteFetchesOpt.isPresent()) {
            resetFetchOffsetMetadataForRemoteFetchPartitions(topicPartitionData, responseData);

            responseData.forEach((topicIdPartition, logReadResult) -> {
                if (logReadResult.info().delayedRemoteStorageFetch.isEmpty()) {
                    shareFetchPartitionDataList.add(new ShareFetchPartitionData(
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19133: support fetching for multiple remote fetch topic partitions in a single share fetch request (#19592).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
