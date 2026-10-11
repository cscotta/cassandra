**Commit**: a6dfde7ce6 · **Ticket**: KAFKA-18629
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Currently, the delete share group code flow uses
`group.subscribedTopicNames()` to fetch information about all the share
partitions to which a share group is subscribed to. * However, this is incorrect since once the group is EMPTY, a
precondition for delete, the aforementioned method will return an empty
list. * In this PR we have leveraged the `ShareGroupStatePartitionMetadata`
record to grab the `initialized` and `initializing` partitions to build
the delete candidates, which remedies the situation. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                DeleteGroupsResponseData.DeletableGroupResultCollection collection = new DeleteGroupsResponseData.DeletableGroupResultCollection();
                List<String> retainedGroupIds = deleteCandidateGroupIds(groupErrMap, groupList, collection);
                    return CompletableFuture.completedFuture(collection);
                    .whenComplete((resp, __) -> resp.forEach(result -> collection.add(result.duplicate())))
                    .thenApply(__ -> collection);
            // - The above map is then used to decide whether to invoke the group coordinator delete groups logic
    private List<String> deleteCandidateGroupIds(
        Map<String, Errors> groupErrMap,

// After
    /**
     * Creates a ShareGroupStatePartitionMetadata tombstone.
     *
     * @param groupId   The share group id.
     * @return The record.
     */
    public static CoordinatorRecord newShareGroupStatePartitionMetadataTombstoneRecord(
        String groupId
    ) {
        return CoordinatorRecord.tombstone(
            new ShareGroupStatePartitionMetadataKey()
                .setGroupId(groupId)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: utilize share group partition metadata for delete group. (#19363).

**Test**
Test modified in Assertions.java covering the corrected behaviour.
