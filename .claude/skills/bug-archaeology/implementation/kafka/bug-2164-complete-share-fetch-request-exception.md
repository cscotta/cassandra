**Commit**: cfa2edc7a7 · **Ticket**: KAFKA-17844
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR adds test cases and minor fix to complete share fetch requests on exception from Share Partition Manager.

**Before/After**
```java
// Before
        FetchParams fetchParams = new FetchParams(ApiKeys.SHARE_FETCH.latestVersion(), FetchRequest.ORDINARY_CONSUMER_ID, -1, DELAYED_SHARE_FETCH_MAX_WAIT_MS,
            1, 1024 * 1024, FetchIsolation.HIGH_WATERMARK, Optional.empty());
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), fetchParams, partitionMaxBytes);
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), fetchParams, partitionMaxBytes);
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), fetchParams, partitionMaxBytes);
        FetchParams fetchParams = new FetchParams(ApiKeys.SHARE_FETCH.latestVersion(), FetchRequest.ORDINARY_CONSUMER_ID, -1, DELAYED_SHARE_FETCH_MAX_WAIT_MS,
            1, 1024 * 1024, FetchIsolation.HIGH_WATERMARK, Optional.empty());
                    sharePartitionManager.fetchMessages(groupId, memberId1.toString(), fetchParams, partitionMaxBytes);
        FetchParams fetchParams = new FetchParams(ApiKeys.SHARE_FETCH.latestVersion(), FetchRequest.ORDINARY_CONSUMER_ID, -1,
                DELAYED_SHARE_FETCH_MAX_WAIT_MS, 1, 1024 * 1024, FetchIsolation.HIGH_WATERMARK, Optional.empty());

// After
            if (!shareFetchData.future().isDone()) {
                shareFetchData.future().completeExceptionally(e);
            }
    private static final FetchParams FETCH_PARAMS = new FetchParams(ApiKeys.SHARE_FETCH.latestVersion(),
        FetchRequest.ORDINARY_CONSUMER_ID, -1, DELAYED_SHARE_FETCH_MAX_WAIT_MS,
        1, 1024 * 1024, FetchIsolation.HIGH_WATERMARK, Optional.empty());

        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, partitionMaxBytes);
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, partitionMaxBytes);
        sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, partitionMaxBytes);
                    sharePartitionManager.fetchMessages(groupId, memberId1.toString(), FETCH_PARAMS, partitionMaxBytes);
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17844: complete share fetch request on exception  (#17566).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
