**Commit**: 15206d5083 · **Ticket**: KAFKA-18084
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                        if (inFlightBatch.batchState() != RecordState.AVAILABLE) {
                if (inFlightBatch.batchState() != RecordState.AVAILABLE) {
                if (offsetState.getValue().state != RecordState.AVAILABLE) {
                    log.trace("The offset is not available skipping, offset: {} batch: {}"
                            + " for the share partition: {}-{}", offsetState.getKey(), inFlightBatch,
                        groupId, topicIdPartition);
            // Update acquisition lock timeout task for the batch to null since it is completed now.
            updateResult.updateAcquisitionLockTimeoutTask(null);
            // Update acquisition lock timeout task for the offset to null since it is completed now.
            updateResult.updateAcquisitionLockTimeoutTask(null);
            if (batchState == null) {
                throw new IllegalStateException("The batch state is not available as the offset state is maintained");

// After
                        if (inFlightBatch.batchState() != RecordState.AVAILABLE || inFlightBatch.batchHasOngoingStateTransition()) {
                if (inFlightBatch.batchState() != RecordState.AVAILABLE || inFlightBatch.batchHasOngoingStateTransition()) {
                if (offsetState.getValue().state != RecordState.AVAILABLE || offsetState.getValue().hasOngoingStateTransition()) {
                    log.trace("The offset {} is not available in share partition: {}-{}, skipping: {}",
                        offsetState.getKey(), groupId, topicIdPartition, inFlightBatch);
            // Cancel the acquisition lock timeout task for the batch since it is completed now.
            updateResult.cancelAndClearAcquisitionLockTimeoutTask();
            // Cancel the acquisition lock timeout task for the offset since it is completed now.
            updateResult.cancelAndClearAcquisitionLockTimeoutTask();
            return inFlightState().state;
            return inFlightState().acquisitionLockTimeoutTask;
        private InFlightState inFlightState() {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18084 added usage for rollback state while sharepartition acquires records (#17965).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
