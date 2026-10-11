**Commit**: bb97d63d41 · **Ticket**: KAFKA-17578
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The ModernGroup#subscribedTopicMetadata takes too much memory due to partitionRacks. This is not being used at the moment as the consumer protocol does not support rack aware assignments. A heap dump from a group with 500 members, 2K subscribed topic partitions shows 654,400 bytes used for partitionRacks. The rest of the ConsumerGroup object holds 822,860 bytes.

**Before/After**
```java
// Before
            List<ConsumerGroupPartitionMetadataValue.PartitionMetadata> partitionMetadata = new ArrayList<>();
            // If the partition rack information map is empty, store an empty list in the record.
            if (!topicMetadata.partitionRacks().isEmpty()) {
                topicMetadata.partitionRacks().forEach((partition, racks) ->
                    partitionMetadata.add(new ConsumerGroupPartitionMetadataValue.PartitionMetadata()
                        .setPartition(partition)
                        .setRacks(new ArrayList<>(racks))
                    )
                );
            }
                .setPartitionMetadata(partitionMetadata)
            List<ShareGroupPartitionMetadataValue.PartitionMetadata> partitionMetadata = new ArrayList<>();

// After
                    topicImage.partitions().size()
                ));
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17578: remove partitionracks from topicmetadata (#17233).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
