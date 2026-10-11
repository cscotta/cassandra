**Commit**: 68b9770506 · **Ticket**: KAFKA-17608
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
This change fixes a few issues. KAFKA-17608; KRaft controller crashes when active controller is removed
When a control batch is committed, the quorum controller currently increases the last stable offset but fails to create a snapshot for that offset. This causes an issue if the quorum controller renounces and needs to revert to that offset (which has no snapshot present). Since the control batches are no-ops for the quorum controller, it does not need to update its offsets for control records.

**Before/After**
```java
// Before
                        if (isActive) {

                            // The active controller can delete up to the current committed offset.
                            snapshotRegistry.deleteSnapshotsUpTo(offsetControl.lastStableOffset());
                // It is posible that the old leader is not a voter in the new voter set.

// After
        handleCommitBatchMetrics(batch);
    }

    void handleCommitBatchMetrics(Batch<ApiMessageAndVersion> batch) {
                        if (messages.isEmpty()) {
                            log.debug("Skipping handling commit for batch with no data records with offset {} and epoch {}.", offset, epoch);
                            offsetControl.handleCommitBatchMetrics(batch);
                        } else if (isActive) {
                // It is possible that the old leader is not a voter in the new voter set.

            // Make sure that the listeners are updated
            state.updateListeners(voterNode.listeners());
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17608, kafka-17604, kafka-16963; kraft controller crashes when active controller is removed (#17146).

**Test**
Test modified in kafka.py covering the corrected behaviour.
