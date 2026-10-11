**Commit**: 05d71ad1a8 · **Ticket**: KAFKA-19476
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
The PR fixes following:

1. In case share partition arrive at a state which should be treated as
final state
of that batch/offset (example - LSO movement which causes offset/batch
to be ARCHIVED permanently), the result of pending write state RPCs for
that offset/batch override the ARCHIVED state. Hence track such updates
and apply when transition is completed. 2.

**Before/After**
```java
// Before
    /**
     * The replica manager is used to check to see if any delayed share fetch request can be completed because of data
     * availability due to acquisition lock timeout.
     */
    private final ReplicaManager replicaManager;

                offsetState.getValue().archive(EMPTY_MEMBER_ID);
                if (initialState == RecordState.ACQUIRED) {
                    offsetState.getValue().cancelAndClearAcquisitionLockTimeoutTask();
                }
                inFlightBatch.archiveBatch(EMPTY_MEMBER_ID);
                if (initialState == RecordState.ACQUIRED) {

// After
    /**
     * The replica manager is used to check to see if any delayed share fetch request can be completed because of data
     * availability due to acquisition lock timeout.
     */
    private final ReplicaManager replicaManager;

                offsetState.getValue().archive();
                inFlightBatch.archiveBatch();

                    if (inFlightBatch.batchHasOngoingStateTransition()) {
                        log.debug("The batch has on-going transition, batch: {} for the share "
                            + "partition: {}-{}", inFlightBatch, groupId, topicIdPartition);
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19476: concurrent execution fixes for lock timeout and lso movement (#20286).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
