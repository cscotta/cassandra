**Commit**: 87f8147ed0 · **Ticket**: KAFKA-16855
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Add field tieredEpoch to RemoteLogSegmentMetadata
Update relevant tests
Add two fields tieredEpoch and tieredState to TopicRecord.json

Reviewers: Luke Chen <showuon@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
                    segmentLeaderEpochs);
                RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentEpochs);
                        500, 539, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 500L, 539L)),
                        540, 700, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 540L, 700L))
                metadata1.customMetadata(), metadata1.state(), metadata1.segmentLeaderEpochs());
                    segmentLeaderEpochs);
      segmentMetadataList.add(new RemoteLogSegmentMetadata(new RemoteLogSegmentId(topicIdPartition, Uuid.randomUuid()),
        startOffset, endOffset, timestamp, 0, timestamp, segmentSize, Optional.empty(),
        RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentLeaderEpochs))

// After
            int tieredEpoch = 0;
                    segmentLeaderEpochs, tieredEpoch);
                RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentEpochs, 0);
                        500, 539, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 500L, 539L), 0),
                        540, 700, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 540L, 700L), 0)
                metadata1.customMetadata(), metadata1.state(), metadata1.segmentLeaderEpochs(), 0);
                    segmentLeaderEpochs,
                0);
      segmentMetadataList.add(new RemoteLogSegmentMetadata(new RemoteLogSegmentId(topicIdPartition, Uuid.randomUuid()), startOffset, endOffset, timestamp, 0, timestamp, segmentSize, Optional.empty(), RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentLeaderEpochs, 0))
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16855 : part 1 - new fields tieredepoch and tieredstate  (#16257).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
