**Commit**: 96ef1c520a · **Ticket**: KAFKA-19436
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In the stress testing it was noticed that on acquisition lock timeout,
some offsets were not found in the cache. The cache can be tried to be
updated in different acknowledgement calls hence if there is an ongoing
transition which is not yet finished but another parallel
acknowledgement triggers the cache update then the cache can be updated
incorrectly, while first transition is not yet finished. Though the cache update happens for Archived and Acknowldeged records
hence this issue or existing implementation should not hamper the queues
functionality. But it might update the cache early when persister call
might fail or this issue triggers error logs with offset not found in
cache when acqu...

**Before/After**
```java
// Before
    private boolean canMoveStartOffset() {
        RecordState startOffsetState = entry.getValue().offsetState == null ?
                    if (!isRecordStateAcknowledged(inFlightBatch.batchState())) {
                        if (!isRecordStateAcknowledged(offsetState.getValue().state())) {
        private boolean batchHasOngoingStateTransition() {
        private boolean hasOngoingStateTransition() {

// After
    // Visible for testing.
    boolean canMoveStartOffset() {
        boolean isBatchState = entry.getValue().offsetState() == null;
        boolean isOngoingTransition = isBatchState ?
            entry.getValue().batchHasOngoingStateTransition() :
            entry.getValue().offsetState().get(startOffset).hasOngoingStateTransition();
        if (isOngoingTransition) {
            return false;
        }

        RecordState startOffsetState = isBatchState ?
                    if (inFlightBatch.batchHasOngoingStateTransition() || !isRecordStateAcknowledged(inFlightBatch.batchState())) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19436: restrict cache update for ongoing batch/offset state (#20041).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
