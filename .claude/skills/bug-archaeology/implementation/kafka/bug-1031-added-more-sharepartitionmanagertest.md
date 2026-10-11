**Commit**: c398261834 · **Ticket**: KAFKA-19285
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
This PR includes more unit tests in the file SharePartitionManagerTest
to improve the coverage

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        ShareFetchContext context2 = sharePartitionManager.newContext(groupId, List.of(), List.of(), reqMetadata2, true, CONNECTION_ID);
            sharePartitionManager.newContext(groupId, reqData2, List.of(), reqMetadata2, true, CONNECTION_ID));

// After
import org.apache.kafka.common.errors.ShareSessionLimitReachedException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
    private static final List<ShareFetchResponseData.AcquiredRecords> EMPTY_ACQUIRED_RECORDS = List.of();
        ShareFetchContext context2 = sharePartitionManager.newContext(groupId, EMPTY_PART_LIST, EMPTY_PART_LIST, reqMetadata2, true, CONNECTION_ID);
            sharePartitionManager.newContext(groupId, reqData2, EMPTY_PART_LIST, reqMetadata2, true, CONNECTION_ID));
    }

    @Test
    public void testNewContextThrowsErrorWhenShareSessionNotFoundOnFinalEpoch() {
        ShareSessionCache cache = new ShareSessionCache(10);
        sharePartitionManager = SharePartitionManagerBuilder.builder()
            .withCache(cache)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19285: added more tests in sharepartitionmanagertest (#19778).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
