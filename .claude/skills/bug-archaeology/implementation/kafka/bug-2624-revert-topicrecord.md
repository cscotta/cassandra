**Commit**: 8438c4339e · **Ticket**: KAFKA-17245
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Revert KAFKA-16257 changes because KIP-950 doesn't need it anymore. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
            int tieredEpoch = 0;
                    segmentLeaderEpochs, tieredEpoch);
                RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentEpochs, 0);
                        500, 539, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 500L, 539L), 0),
                        540, 700, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 540L, 700L), 0)
                metadata1.customMetadata(), metadata1.state(), metadata1.segmentLeaderEpochs(), 0);
                    segmentLeaderEpochs,
                0);
      segmentMetadataList.add(new RemoteLogSegmentMetadata(new RemoteLogSegmentId(topicIdPartition, Uuid.randomUuid()), startOffset, endOffset, timestamp, 0, timestamp, segmentSize, Optional.empty(), RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentLeaderEpochs, 0))

// After
                    segmentLeaderEpochs);
                RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentEpochs);
                        500, 539, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 500L, 539L)),
                        540, 700, timestamp, brokerId, timestamp, segmentSize, truncateAndGetLeaderEpochs(epochEntries, 540L, 700L))
                metadata1.customMetadata(), metadata1.state(), metadata1.segmentLeaderEpochs());
                    segmentLeaderEpochs
            );
      segmentMetadataList.add(new RemoteLogSegmentMetadata(new RemoteLogSegmentId(topicIdPartition, Uuid.randomUuid()), startOffset, endOffset, timestamp, 0, timestamp, segmentSize, Optional.empty(), RemoteLogSegmentState.COPY_SEGMENT_FINISHED, segmentLeaderEpochs))
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17245: revert topicrecord changes. (#16780).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
