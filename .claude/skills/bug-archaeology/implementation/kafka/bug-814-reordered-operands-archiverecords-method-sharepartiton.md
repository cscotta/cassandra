**Commit**: def5f16c33 · **Ticket**: KAFKA-19630
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As per the current implementation in archiveRecords, when LSO is
updated, if we have multiple record batches before the new LSO, then
only the first one gets archived. This is because of the following lines
of code ->

`isAnyOffsetArchived = isAnyOffsetArchived ||
archivePerOffsetBatchRecords(inFlightBatch, startOffset, endOffset - 1,
initialState);`

`isAnyBatchArchived = isAnyBatchArchived ||
archiveCompleteBatch(inFlightBatch, initialState);`

The first record / batch will make `isAnyOffsetArchived` /
`isAnyBatchArchived` true, after which this line of code will
short-circuit and the methods `archivePerOffsetBatchRecords` /
`archiveCompleteBatch` will not be called again. This PR changes ...

**Before/After**
```java
// Before
                    isAnyOffsetArchived = isAnyOffsetArchived || archivePerOffsetBatchRecords(inFlightBatch, startOffset, endOffset - 1, initialState);
                isAnyBatchArchived = isAnyBatchArchived || archiveCompleteBatch(inFlightBatch, initialState);

// After
                    isAnyOffsetArchived = archivePerOffsetBatchRecords(inFlightBatch, startOffset, endOffset - 1, initialState) || isAnyOffsetArchived;
                isAnyBatchArchived = archiveCompleteBatch(inFlightBatch, initialState) || isAnyBatchArchived;
    @Test
    public void testLsoMovementForArchivingAllAvailableBatches() {
        SharePartition sharePartition = SharePartitionBuilder.builder().withState(SharePartitionState.ACTIVE).build();

        // A client acquires 4 batches, 11 -> 20, 21 -> 30, 31 -> 40, 41 -> 50.
        fetchAcquiredRecords(sharePartition, memoryRecords(10, 11), 10);
        fetchAcquiredRecords(sharePartition, memoryRecords(10, 21), 10);
        fetchAcquiredRecords(sharePartition, memoryRecords(10, 31), 10);
        fetchAcquiredRecords(sharePartition, memoryRecords(10, 41), 10);

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19630: reordered or operands in archiverecords method for sharepartiton (#20391).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
