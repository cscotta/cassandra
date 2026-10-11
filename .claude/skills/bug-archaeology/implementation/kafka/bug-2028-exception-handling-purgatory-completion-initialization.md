**Commit**: 1834030107 · **Ticket**: KAFKA-17510
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.share.fetch.ShareFetchData;
    private final ShareFetchData shareFetchData;
            ShareFetchData shareFetchData,
        super(shareFetchData.fetchParams().maxWaitMs, Optional.empty());
        this.shareFetchData = shareFetchData;
            + "topic partitions {}", shareFetchData.groupId(), shareFetchData.memberId(),
        if (shareFetchData.future().isDone())
            shareFetchData.future().complete(Collections.emptyMap());
            topicPartitionData, shareFetchData.groupId(), shareFetchData.fetchParams());
            shareFetchData.future().complete(ShareFetchUtils.processFetchResponse(shareFetchData, fetchPartitionsData,
            sharePartitionManager.handleFetchException(shareFetchData.groupId(), topicPartitionData.keySet(), shareFetchData.future(), e);
                    new DelayedShareFetchGroupKey(shareFetchData.groupId(), topicIdPartition.topicId(), topicIdPartition.partition()))));

// After
import org.apache.kafka.server.share.SharePartitionKey;
import org.apache.kafka.server.share.fetch.ShareFetch;
    private final ShareFetch shareFetch;
            ShareFetch shareFetch,
        super(shareFetch.fetchParams().maxWaitMs, Optional.empty());
        this.shareFetch = shareFetch;
            + "topic partitions {}", shareFetch.groupId(), shareFetch.memberId(),
        if (shareFetch.isCompleted())
            shareFetch.maybeComplete(Collections.emptyMap());
            topicPartitionData, shareFetch.groupId(), shareFetch.fetchParams());
            shareFetch.maybeComplete(ShareFetchUtils.processFetchResponse(shareFetch, fetchPartitionsData,
            handleFetchException(shareFetch, topicPartitionData.keySet(), e);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17510: exception handling and purgatory completion on initialization delay (#17709).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
