**Commit**: 03e6aa4398 · **Ticket**: KAFKA-20266
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Add Timestamp fields to TargetAssignmentMetadata records which default
to 0 when absent. The new timestamp field records when the last target
assignment calculation finished. When upgrading a classic group, the new timestamp field is initialized
to 0. Reviewers: khilesh Chaganti <akhileshchg@users.noreply.github.com>,
 Chia-Ping Tsai <chia7712@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * @param groupId           The consumer group id.
     * @param assignmentEpoch   The consumer group epoch.
    public static CoordinatorRecord newConsumerGroupTargetAssignmentEpochRecord(
        int assignmentEpoch
                    .setAssignmentEpoch(assignmentEpoch),
     * @param groupId           The group id.
     * @param assignmentEpoch   The group epoch.
    public static CoordinatorRecord newShareGroupTargetAssignmentEpochRecord(
        int assignmentEpoch
                    .setAssignmentEpoch(assignmentEpoch),
            group.setTargetAssignmentEpoch(value.assignmentEpoch());

// After
     * @param groupId             The consumer group id.
     * @param assignmentEpoch     The consumer group epoch.
     * @param assignmentTimestamp The time at which the target assignment calculation finished.
    public static CoordinatorRecord newConsumerGroupTargetAssignmentMetadataRecord(
        int assignmentEpoch,
        long assignmentTimestamp
                    .setAssignmentEpoch(assignmentEpoch)
                    .setAssignmentTimestamp(assignmentTimestamp),
     * @param groupId             The group id.
     * @param assignmentEpoch     The group epoch.
     * @param assignmentTimestamp The time at which the target assignment calculation finished.
    public static CoordinatorRecord newShareGroupTargetAssignmentMetadataRecord(
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20266: add tracking of assignment timestamps (#21652).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
