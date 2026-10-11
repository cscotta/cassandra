**Commit**: c8cfb4c7f1 · **Ticket**: KAFKA-17428
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
This change introduces a retry mechanism for cleaninig up remote segments that failed the copy to remote storage. It also makes sure that we always update the remote segment state whenever we attempt a deletion. When a segment copy fails, we immediately try to delete the segment, but this can also fail. The RLMExpirationTask is now also responsible for retring dangling segments cleanup.

**Before/After**
```java
// Before
                    remoteLogStorageManager.deleteLogSegmentData(copySegmentStartedRlsm);
                    logger.info("Successfully cleaned segment {} after failing to copy segment", segmentId);
                    logger.error("Error while cleaning segment {}, consider cleaning manually", segmentId, e1);
                    logger.error("Custom metadata size {} exceeds configured limit {}." +
                        // For deletion, we provide back the custom metadata by creating a new metadata object from the update.
                        // However, the update itself will not be stored in this case.
                        remoteLogStorageManager.deleteLogSegmentData(copySegmentStartedRlsm.createWithUpdates(copySegmentFinishedRlsm));
                        logger.info("Successfully cleaned segment after custom metadata size exceeded");
                        logger.error("Error while cleaning segment after custom metadata size exceeded, consider cleaning manually", e1);
            // Cleanup/delete expired remote log segments
                boolean isSegmentDeleted = deleteRemot

// After

                logger.info("Copy failed, cleaning segment {}", copySegmentStartedRlsm.remoteLogSegmentId());
                    deleteRemoteLogSegment(copySegmentStartedRlsm, ignored -> !isCancelled());
                    LOGGER.info("Cleanup completed for segment {}", copySegmentStartedRlsm.remoteLogSegmentId());
                    LOGGER.info("Cleanup failed, will retry later with segment {}: {}", copySegmentStartedRlsm.remoteLogSegmentId(), e1.getMessage());
                    logger.info("Custom metadata size {} exceeds configured limit {}." +
                    // For deletion, we provide back the custom metadata by creating a new metadata object from the update.
                    // However, the update itself will not be stored in this case.
                    RemoteLogSegmentMetadata newMetadata = copySegmentStartedRlsm.createWithUpdates(copySegmentFinishedRlsm);
                        deleteRemoteLogSegment(newMetadata, ignored -> !isCancelled());
                        LOGGER.info("Cleanup completed for segment {}", newMetadata.remoteLogSegmentId());
                        LOGGER.info("Cleanup failed, will retry later with segment {}: {}", newMetadata.remoteLogSegmentId(), e1.getMessage());
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17428: add retry mechanism for cleaning up dangling remote segments (#17335).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
