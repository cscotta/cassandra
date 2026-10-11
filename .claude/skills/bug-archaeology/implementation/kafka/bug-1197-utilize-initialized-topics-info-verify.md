**Commit**: 270948bf9d · **Ticket**: KAFKA-19115
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Currently, in the deleteShareGroupOffsets method in
GroupCoordinatorService, the user request was simply forwarded to the
persister without checking if the requested share partitions were
initialized for the group or not. This PR introduces such a check to
make sure that the persister deleteState request is only called for
share partitions that have been initialized for the group. Reviewers: Andrew Schofield <aschofield@confluent.io>, Sushant Mahajan <smahajan@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.DeleteShareGroupStateRequestData;
    private void populateDeleteShareGroupOffsetsFuture(
        DeleteShareGroupOffsetsRequestData requestData,
        CompletableFuture<DeleteShareGroupOffsetsResponseData> future,
        Map<Uuid, String> requestTopicIdToNameMapping,
        List<DeleteShareGroupStateRequestData.DeleteStateData> deleteShareGroupStateRequestTopicsData,
        List<DeleteShareGroupOffsetsResponseData.DeleteShareGroupOffsetsResponseTopic> deleteShareGroupOffsetsResponseTopicList

        DeleteShareGroupStateRequestData deleteShareGroupStateRequestData = new DeleteShareGroupStateRequestData()
            .setGroupId(requestData.groupId())
            .setTopics(deleteShareGroupStateRequestTopicsData);


// After
    private CompletableFuture<DeleteShareGroupOffsetsResponseData> persistDeleteShareGroupOffsets(
        DeleteShareGroupStateParameters deleteStateRequestParameters,
        List<DeleteShareGroupOffsetsResponseData.DeleteShareGroupOffsetsResponseTopic> errorTopicResponseList
        return persister.deleteState(deleteStateRequestParameters)
            .thenCompose(result -> {
                    Exception exception = new IllegalStateException("Result is null for the delete share group state");
                    return CompletableFuture.completedFuture(
                        DeleteShareGroupOffsetsRequest.getErrorDeleteResponseData(Errors.forException(exception))
                    );
                    errorTopicResponseList.add(
                        new DeleteShareGroupOffsetsResponseData.DeleteShareGroupOffsetsResponseTopic()
                            .setTopicId(topicData.topicId())
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19115: utilize initialized topics info to verify delete share group offsets (#19431).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
