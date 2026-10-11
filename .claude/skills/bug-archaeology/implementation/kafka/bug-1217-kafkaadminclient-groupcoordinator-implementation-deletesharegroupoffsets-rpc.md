**Commit**: 5148174196 · **Ticket**: KAFKA-16718
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This PR contains the implementation of KafkaAdminClient and
GroupCoordinator for DeleteShareGroupOffsets RPC. - Added `deleteShareGroupOffsets` to `KafkaAdminClient`
- Added implementation for `handleDeleteShareGroupOffsetsRequest` in
`KafkaApis.scala`
- Added `deleteShareGroupOffsets` to `GroupCoordinator` as well. internally this makes use of `persister.deleteState` to persist the
changes in share coordinator

Reviewers: Andrew Schofield <aschofield@confluent.io>, Sushant Mahajan <smahajan@confluent.io>

**Before/After**
```java
// Before
     * Delete share groups from the cluster with the default options.
     * @param groupIds Collection of share group ids which are to be deleted.
     * @return The DeleteShareGroupsResult.
    default DeleteShareGroupsResult deleteShareGroups(Collection<String> groupIds) {
        return deleteShareGroups(groupIds, new DeleteShareGroupsOptions());

// After
     * Delete offsets for a set of partitions in a share group.
     * @param groupId The group for which to delete offsets.
     * @param partitions The topic-partitions.
     * @param options The options to use when deleting offsets in a share group.
     * @return The DeleteShareGroupOffsetsResult.
    DeleteShareGroupOffsetsResult deleteShareGroupOffsets(String groupId, Set<TopicPartition> partitions, DeleteShareGroupOffsetsOptions options);

    /**
     * Delete offsets for a set of partitions in a share group with the default options.
     *
     * <p>
     * This is a convenience method for {@link #deleteShareGroupOffsets(String, Set, DeleteShareGroupOffsetsOptions)} with default options.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16718-2/n: kafkaadminclient and groupcoordinator implementation for deletesharegroupoffsets rpc (#18976).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
