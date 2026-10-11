**Commit**: 369cc569b4 · **Ticket**: KAFKA-17747
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
* Add MetadataHash field to ConsumerGroupMetadataValue,
ShareGroupMetadataValue, and StreamGroupMetadataValue. * Add metadataHash field to
GroupCoordinatorRecordHelpers#newConsumerGroupEpochRecord,
GroupCoordinatorRecordHelpers#newShareGroupEpochRecord, and
StreamsCoordinatorRecordHelpers#newStreamsGroupEpochRecord. * Add deprecated message to ConsumerGroupPartitionMetadataKey and
ConsumerGroupPartitionMetadataValue. * ShareGroupPartitionMetadataKey / ShareGroupPartitionMetadataValue /
StreamGroupPartitionMetadataKey / StreamGroupPartitionMetadataValue will
be removed in next PR.

**Before/After**
```java
// Before
        int newGroupEpoch
                    .setEpoch(newGroupEpoch),
        int newGroupEpoch
                    .setEpoch(newGroupEpoch),
            records.add(newStreamsGroupEpochRecord(groupId, groupEpoch));
                records.add(newShareGroupEpochRecord(groupId, groupEpoch));
                records.add(newConsumerGroupEpochRecord(groupId, groupEpoch));
            records.add(newConsumerGroupEpochRecord(groupId, groupEpoch));
            records.add(newConsumerGroupEpochRecord(group.groupId(), groupEpoch));

// After
     * @param metadataHash  The consumer group metadata hash.
        int newGroupEpoch,
        long metadataHash
                    .setEpoch(newGroupEpoch)
                    .setMetadataHash(metadataHash),
     * @param metadataHash  The group metadata hash.
        int newGroupEpoch,
        long metadataHash
                    .setEpoch(newGroupEpoch)
                    .setMetadataHash(metadataHash),
            records.add(newStreamsGroupEpochRecord(groupId, groupEpoch, 0));
                records.add(newShareGroupEpochRecord(groupId, groupEpoch, 0));
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17747: [1/n] add metadatahash field to consumer/share/streamgroupmetadatavalue (#19504).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
