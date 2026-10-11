**Commit**: 5914013219 · **Ticket**: KAFKA-17980
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
- The isReady API in RemoteLogMetadataManager (RLMM) is used to denote whether the partition metadata is ready for remote storage operations. The plugin implementors can use this API to denote the partition status while bootstrapping the RLMM. - Using this API, we are gracefully starting the remote log components. The segment copy, delete, and other operations that hits remote storage will be invoked once the metadata is ready for a given partition.

**Before/After**
```java
// Before
                    LOGGER.info("Cancelling the copy RLM task for tpId: {}", tpId);
                    LOGGER.info("Resetting remote copy lag metrics for tpId: {}", tpId);
                        LOGGER.info("Cancelling the copy RLM task for tpId: {}", tpId);
                        LOGGER.info("Cancelling the expiration RLM task for tpId: {}", tpId);
                        LOGGER.info("Cancelling the follower RLM task for tpId: {}", tpId);
            if (isCancelled())
                    logger.warn("Current thread for topic-partition-id {} is interrupted", topicIdPartition, ex);
                logger.debug("Encountered a retryable error while executing current task for topic-partition {}", topicIdPartition, ex);
                    logger.warn("Current task for topic-partition {} received error but it will be scheduled", topicIdPartition, ex);

// After
                    LOGGER.info("Cancelling the copy RLM task for partition: {}", tpId);
                    LOGGER.info("Resetting remote copy lag metrics for partition: {}", tpId);
                        LOGGER.info("Cancelling the copy RLM task for partition: {}", tpId);
                        LOGGER.info("Cancelling the expiration RLM task for partition: {}", tpId);
                        LOGGER.info("Cancelling the follower RLM task for partition: {}", tpId);
            if (isCancelled()) {
                logger.debug("Skipping the current run for partition {} as it is cancelled", topicIdPartition);
            }
            if (!remoteLogMetadataManager.isReady(topicIdPartition)) {
                logger.debug("Skipping the current run for partition {} as the remote-log metadata is not ready", topicIdPartition);
                return;
            }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17980: introduce `isready` api in remotelogmetadatamanager (#17737).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
