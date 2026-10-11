**Commit**: 33147d1089 · **Ticket**: KAFKA-17863
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Make max.poll.records a soft limit so that record batch boundaries can be respected in records returned by ShareConsumer.poll. This gives a significant performance gain because the broker is much more efficient at handling batches which have not been split. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
            while (recordsInBatch < maxRecords) {
                lastRecord = nextFetchedRecord(checkCrcs);
    private Record nextFetchedRecord(final boolean checkCrcs) {
                    return null;
                    return record;
        long firstMessageId = 5;
        int numRecords = 11;        // Records for 10-20
                .setRecords(newRecords(startingOffset, numRecords, firstMessageId))
        assertEquals(1, records.size());

// After
            boolean currentBatchHasMoreRecords = false;

            while (recordsInBatch < maxRecords || currentBatchHasMoreRecords) {
                currentBatchHasMoreRecords = nextFetchedRecord(checkCrcs);
    /**
     * Scans for the next record in the available batches, skipping control records
     *
     * @param checkCrcs Whether to check the CRC of fetched records
     *
     * @return true if the current batch has more records, else false
     */
    private boolean nextFetchedRecord(final boolean checkCrcs) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17863: share consumer max poll records soft limit (#17592).

**Test**
Test modified in ShareCompletedFetchTest.java covering the corrected behaviour.
