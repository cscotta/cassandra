**Commit**: 0d44415bac · **Ticket**: KAFKA-17774
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR adds capability to restrict the messages in Share Fetch. The max fetch records will be an additional way to limit the number of records sent from broker to client. In Share Fetch, with min and mx bytes, there exists 3 problems:

1. The max.poll.records client config sends the max number of records defined to application but might have fetched extra becuase of higher max bytes.

**Before/After**
```java
// Before
import org.apache.kafka.common.message.ShareFetchResponseData.AcquiredRecords;
import java.util.List;
        responseData.forEach((topicIdPartition, fetchPartitionData) -> {
                return;
                List<AcquiredRecords> acquiredRecords = sharePartition.acquire(shareFetchData.memberId(), fetchPartitionData);
                    topicIdPartition, shareFetchData, acquiredRecords);
                if (acquiredRecords.isEmpty()) {
                        .setAcquiredRecords(acquiredRecords);

// After
import org.apache.kafka.server.share.fetch.ShareAcquiredRecords;

        // Acquired records count for the share fetch request.
        int acquiredRecordsCount = 0;
        for (Map.Entry<TopicIdPartition, FetchPartitionData> entry : responseData.entrySet()) {
            TopicIdPartition topicIdPartition = entry.getKey();
            FetchPartitionData fetchPartitionData = entry.getValue();
                continue;
                ShareAcquiredRecords shareAcquiredRecords = sharePartition.acquire(shareFetchData.memberId(), shareFetchData.maxFetchRecords() - acquiredRecordsCount, fetchPartitionData);
                    topicIdPartition, shareFetchData, shareAcquiredRecords);
                if (shareAcquiredRecords.acquiredRecords().isEmpty()) {
                        // We set the records to the fetchPartitionData records. We do not alter the records
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17774: adding capability to handle max fetch records in share fetch (kip-932) (#17322).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
