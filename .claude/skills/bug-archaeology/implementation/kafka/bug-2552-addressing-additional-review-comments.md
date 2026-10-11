**Commit**: 9f330c374d · **Ticket**: KAFKA-16753
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
     * offsets that are either released by acknowledgement API or lock timed out hence the next fetch
     * @param acknowledgementBatch The acknowledgement batch list for the share partition.
        List<ShareAcknowledgementBatch> acknowledgementBatch
            for (int i = 0; i < acknowledgementBatch.size(); i++) {
                ShareAcknowledgementBatch batch = acknowledgementBatch.get(i);

                    subMap = fetchSubMapForAcknowledgementBatch(batch, i == acknowledgementBatch.size() - 1);
                Optional<Throwable> ackThrowable = acknowledgementBatchRecords(
        ShareAcknowledgementBatch batch,
        boolean isLastBatch
                    // of 0 - 5, 5 - 10, 10 - 12, 12 - 15. The request batch for acknowledgement is 5 - 15,
            // Validate if the request batch has the first offset less than the last offset of the last

// After
     * offsets that are either released by acknowledge API or lock timed out hence the next fetch
     * @param acknowledgementBatches The acknowledgement batch list for the share partition.
        List<ShareAcknowledgementBatch> acknowledgementBatches
            for (ShareAcknowledgementBatch batch : acknowledgementBatches) {
                    subMap = fetchSubMapForAcknowledgementBatch(batch);
                Optional<Throwable> ackThrowable = acknowledgeBatchRecords(
        ShareAcknowledgementBatch batch
                    // of 0 - 5, 5 - 10, 10 - 12, 12 - 15. The request batch for acknowledge is 5 - 15,
            // Validate if the request batch has the first offset greater than the last offset of the last
            // fetched cached batch, then there will be no offsets in the request that can be acknowledged.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16753: [minor] addressing additional review comments (#16894).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
