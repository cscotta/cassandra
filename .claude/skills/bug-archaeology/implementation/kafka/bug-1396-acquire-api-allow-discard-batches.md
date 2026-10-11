**Commit**: 6e45ab7d84 · **Ticket**: KAFKA-17351
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR does following:
1. Adds `fetchOffset` to `acquire` API in SharePartition. 2. Adds a ShareFetchPartitionData class efficiently handle the
propagation of fetchOffset information.

**Before/After**
```java
// Before
import org.apache.kafka.server.storage.log.FetchPartitionData;
            LinkedHashMap<TopicIdPartition, FetchPartitionData> fetchPartitionsData = new LinkedHashMap<>();
            for (Map.Entry<TopicIdPartition, LogReadResult> entry : responseData.entrySet())
                fetchPartitionsData.put(entry.getKey(), entry.getValue().toFetchPartitionData(false));

            shareFetch.maybeComplete(ShareFetchUtils.processFetchResponse(shareFetch, fetchPartitionsData,
                sharePartitions, replicaManager, exceptionHandler));
            Map<TopicIdPartition, FetchPartitionData> responseData,
            BiConsumer<SharePartitionKey, Throwable> exceptionHandler) {
        for (Map.Entry<TopicIdPartition, FetchPartitionData> entry : responseData.entrySet()) {
            TopicIdPartition topicIdPartition = entry.getKey();
            FetchPartitionData fetchPartitionData = entry.getValue();

// After
import org.apache.kafka.server.share.fetch.ShareFetchPartitionData;
import java.util.ArrayList;
import java.util.List;
            List<ShareFetchPartitionData> shareFetchPartitionDataList = new ArrayList<>();
            responseData.forEach((topicIdPartition, logReadResult) ->
                shareFetchPartitionDataList.add(new ShareFetchPartitionData(
                    topicIdPartition,
                    topicPartitionData.get(topicIdPartition),
                    logReadResult.toFetchPartitionData(false)
                ))
            );

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17351: update tests and acquire api to allow discard batches from compacted topics (1/n) (#18978).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
