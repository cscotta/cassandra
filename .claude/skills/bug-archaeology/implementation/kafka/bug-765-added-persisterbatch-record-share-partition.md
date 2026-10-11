**Commit**: d5e624e918 · **Ticket**: KAFKA-19693
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The method rollbackOrProcessStateUpdates in SharePartition received 2
separate lists of updatedStates (InFlightState) and stateBatches
(PersisterStateBatch). This PR introduces a new subclass called
`PersisterBatch` which encompasses both these objects. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
        List<InFlightState> updatedStates = new ArrayList<>();
        List<PersisterStateBatch> stateBatches = new ArrayList<>();
                    updatedStates,
                    stateBatches
        rollbackOrProcessStateUpdates(future, throwable, updatedStates, stateBatches);
        List<InFlightState> updatedStates = new ArrayList<>();
        List<PersisterStateBatch> stateBatches = new ArrayList<>();
                    Optional<Throwable> releaseAcquiredRecordsThrowable = releaseAcquiredRecordsForPerOffsetBatch(memberId, inFlightBatch, recordState, updatedStates, stateBatches);
                Optional<Throwable> releaseAcquiredRecordsThrowable = releaseAcquiredRecordsForCompleteBatch(memberId, inFlightBatch, recordState, updatedStates, stateBatches);
        rollbackOrProcessStateUpdates(future, throwable, updatedStates, stateBatches);
                                                                        List<InFlightState> updatedStates,
                                                                        List<PersisterStateBatch> stateBatches) {

// After
        List<PersisterBatch>  persisterBatches = new ArrayList<>();
                    persisterBatches
        rollbackOrProcessStateUpdates(future, throwable, persisterBatches);
        List<PersisterBatch> persisterBatches = new ArrayList<>();
                    Optional<Throwable> releaseAcquiredRecordsThrowable = releaseAcquiredRecordsForPerOffsetBatch(memberId, inFlightBatch, recordState, persisterBatches);
                Optional<Throwable> releaseAcquiredRecordsThrowable = releaseAcquiredRecordsForCompleteBatch(memberId, inFlightBatch, recordState, persisterBatches);
        rollbackOrProcessStateUpdates(future, throwable, persisterBatches);
                                                                        List<PersisterBatch> persisterBatches) {
                // Successfully updated the state of the offset and created a persister state batch for write to persister.
                persisterBatches.add(new PersisterBatch(updateResult, new PersisterStateBatch(offsetState.getKey(),
                    offsetState.getKey(), updateResult.state().id(), (short) updateResult.deliveryCount())));
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19693: added persisterbatch record in share partition which includes updatedstate and statebatch (#20507).

**Test**
No test added.
