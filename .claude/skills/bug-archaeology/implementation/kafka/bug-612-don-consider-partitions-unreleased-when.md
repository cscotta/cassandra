**Commit**: 02bd3420da · **Ticket**: KAFKA-19449
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
During reconciliation, we transition members to UNRELEASED_PARTITIONS
when the target assignment contains a partition that is owned by another
member. Partitions are considered owned by another member if they are in
a member's assignment or partitions pending revocation. Ownership is
only updated after reconciliation has finished. Fix a bug where we consider a partition owned by another member when it
is actually in the current member's list of partitions pending
revocation.

**Before/After**
```java
// Before
                currentPartitionEpoch.apply(topicId, partitionId) != -1

// After
                currentPartitionEpoch.apply(topicId, partitionId) != -1 &&
                // Don't consider a partition unreleased if it is owned by the current member
                // because it is pending revocation. This is safe to do since only a single member
                // can own a partition at a time.
                !member.partitionsPendingRevocation().getOrDefault(topicId, Set.of()).contains(partitionId)
    @Test
    public void testUnrevokedPartitionsToStableWithReturnedPartitionsPendingRevocation() {
        String topic1 = "topic1";
        String topic2 = "topic2";
        Uuid topicId1 = Uuid.randomUuid();
        Uuid topicId2 = Uuid.randomUuid();

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19449: don't consider partitions unreleased when pending revocation by the current member (#20781).

**Test**
Test modified in CurrentAssignmentBuilderTest.java covering the corrected behaviour.
