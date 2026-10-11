**Commit**: 00f712abb1 · **Ticket**: KAFKA-20266
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Rename
new{Consumer,Share,Streams}GroupTargetAssignmentEpoch(Tombstone)Record
methods to

new{Consumer,Share,Streams}GroupTargetAssignmentMetadata(Tombstone)Record. These were missed in the previous change. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static CoordinatorRecord newConsumerGroupTargetAssignmentEpochTombstoneRecord(
    public static CoordinatorRecord newShareGroupTargetAssignmentEpochTombstoneRecord(
        records.add(GroupCoordinatorRecordHelpers.newConsumerGroupTargetAssignmentEpochTombstoneRecord(groupId));
        records.add(GroupCoordinatorRecordHelpers.newConsumerGroupTargetAssignmentEpochTombstoneRecord(groupId));
        records.add(GroupCoordinatorRecordHelpers.newShareGroupTargetAssignmentEpochTombstoneRecord(groupId()));
    public static CoordinatorRecord newStreamsGroupTargetAssignmentEpochTombstoneRecord(

// After
    public static CoordinatorRecord newConsumerGroupTargetAssignmentMetadataTombstoneRecord(
    public static CoordinatorRecord newShareGroupTargetAssignmentMetadataTombstoneRecord(
        records.add(GroupCoordinatorRecordHelpers.newConsumerGroupTargetAssignmentMetadataTombstoneRecord(groupId));
        records.add(GroupCoordinatorRecordHelpers.newConsumerGroupTargetAssignmentMetadataTombstoneRecord(groupId));
        records.add(GroupCoordinatorRecordHelpers.newShareGroupTargetAssignmentMetadataTombstoneRecord(groupId()));
    public static CoordinatorRecord newStreamsGroupTargetAssignmentMetadataTombstoneRecord(
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20266 finish rename of targetassignmentepoch record methods (#21697).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
