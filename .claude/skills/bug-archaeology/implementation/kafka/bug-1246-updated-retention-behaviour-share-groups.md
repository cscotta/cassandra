**Commit**: 793b47011c · **Ticket**: KAFKA-19016
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
This PR makes the required changes for retaining a share group forever
post creation. A new field `shouldExpire` is added to all types
implementing the interface `Group`. It returns false only in case of
`ShareGroup` and true otherwise. A check is also added in
`cleanupGroupMetadata` in `GroupCoordinatorShard` and it proceeds with
group expiration only if `shouldExpire` returns true

Reviewers: Andrew Schofield <aschofield@confluent.io>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
            boolean allOffsetsExpired = offsetMetadataManager.cleanupExpiredOffsets(groupId, records);
            if (allOffsetsExpired) {
                groupMetadataManager.maybeDeleteGroup(groupId, records);
    public void testCleanupGroupMetadata() {

// After

    /**
     * Returns whether this group should be expired or not.
     *
     * @return whether the group should be expired.
     */
    default boolean shouldExpire() {
        return true;
    }
            Group group = groupMetadataManager.group(groupId);
            if (group.shouldExpire()) {
                boolean allOffsetsExpired = offsetMetadataManager.cleanupExpiredOffsets(groupId, records);
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19016: updated the retention behaviour of share groups to retain them forever (#19343).

**Test**
Test modified in GroupCoordinatorShardTest.java covering the corrected behaviour.
