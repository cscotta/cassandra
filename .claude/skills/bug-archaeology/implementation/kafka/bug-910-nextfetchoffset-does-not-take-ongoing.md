**Commit**: 7cb370b786 · **Ticket**: KAFKA-19463
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
### About
`nextFetchOffset` function in `SharePartition` updates the fetch offsets
without considering batches/offsets which might be undergoing state
transition. This can cause problems in updating to the right fetch
offset. ### Testing
The new code added has been tested with the help of unit tests. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                    if (entry.getValue().batchState() == RecordState.AVAILABLE) {
                        if (offsetState.getValue().state == RecordState.AVAILABLE) {

// After
                    if (entry.getValue().batchState() == RecordState.AVAILABLE && !entry.getValue().batchHasOngoingStateTransition()) {
                        if (offsetState.getValue().state == RecordState.AVAILABLE && !offsetState.getValue().hasOngoingStateTransition()) {
                    if (state.state == RecordState.AVAILABLE) {
                        findNextFetchOffset.set(true);
                    }
    @Test
    public void testAcquireWhenBatchHasOngoingTransition() {
        Persister persister = Mockito.mock(Persister.class);

        SharePartition sharePartition = SharePartitionBuilder.builder()
            .withState(SharePartitionState.ACTIVE)
            .withPersister(persister)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19463: nextfetchoffset does not take ongoing state transition into account (#20080).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
