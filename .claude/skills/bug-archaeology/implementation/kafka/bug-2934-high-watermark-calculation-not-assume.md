**Commit**: f880ad6ccf · **Ticket**: KAFKA-16530
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
1. Changing log message from error to info - We may expect the HW calculation to give us a smaller result than the current HW in the case of quorum reconfiguration. We will continue to not allow the HW to actually decrease. 2.

**Before/After**
```java
// Before
        if (state.updateLocalState(endOffsetMetadata)) {
                        log.error("The latest computed high watermark {} is smaller than the current " +
                                "value {}, which suggests that one of the voters has lost committed data. " +
        LogOffsetMetadata endOffsetMetadata
        state.updateLeaderState(endOffsetMetadata);

        Optional<LogOffsetMetadata> leaderEndOffsetOpt =
            voterStates.get(localId).endOffset;
            currentTimeMs - integerReplicaStateEntry.getValue().lastFetchTimestamp >= OBSERVER_SESSION_TIMEOUT_MS

// After
        if (state.updateLocalState(endOffsetMetadata, partitionState.lastVoterSet().voterIds())) {
import java.util.Iterator;
                        log.info("The latest computed high watermark {} is smaller than the current " +
                                "value {}, which should only happen when voter set membership changes. If the voter " +
                                "set has not changed this suggests that one of the voters has lost committed data. " +
     * @param lastVoterSet the up-to-date voter set
        LogOffsetMetadata endOffsetMetadata,
        Set<Integer> lastVoterSet
        state.updateLeaderEndOffset(endOffsetMetadata);
        updateVoterAndObserverStates(lastVoterSet);
        Optional<LogOffsetMetadata> leaderEndOffsetOpt = getOrCreateReplicaState(localId).endOffset;
    /**
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16530: fix high-watermark calculation to not assume the leader is in the voter set (#16079).

**Test**
Test modified in LeaderStateTest.java covering the corrected behaviour.
