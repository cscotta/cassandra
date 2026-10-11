**Commit**: 875537f54b · **Ticket**: KAFKA-19555
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR restricts the records being acquired post max-inflight limit. Previously the max in-flight limit was only enforced while considering
the share partition for further fetches i.e. once the limit was reached
the share partition was not considered for further fetches. However,
when the records are actively released then there might be some records
being acquired post max-inflight limit.

**Before/After**
```java
// Before
    @SuppressWarnings("cyclomaticcomplexity") // Consider refactoring to avoid suppression
                    firstBatch.baseOffset(), lastBatch.lastOffset(), batchSize, maxFetchRecords);
                if (acquiredCount >= maxFetchRecords) {
                            maybeGapStartOffset, entry.getKey() - 1, batchSize, maxFetchRecords);
                    if (acquiredCount >= maxFetchRecords) {
                    int acquiredSubsetCount = acquireSubsetBatchRecords(memberId, firstBatch.baseOffset(), lastBatch.lastOffset(), inFlightBatch, result);


// After
    @SuppressWarnings({"cyclomaticcomplexity", "methodlength"}) // Consider refactoring to avoid suppression
        LastOffsetAndMaxRecords lastOffsetAndMaxRecords = lastOffsetAndMaxRecordsToAcquire(fetchOffset,
            maxFetchRecords, lastBatch.lastOffset());
        if (lastOffsetAndMaxRecords.maxRecords() <= 0) {
            return ShareAcquiredRecords.empty();
        }
        // The lastOffsetAndMaxRecords contains the last offset to acquire and the maximum number of records
        // to acquire.
        int maxRecordsToAcquire = lastOffsetAndMaxRecords.maxRecords();
        long lastOffsetToAcquire = lastOffsetAndMaxRecords.lastOffset();

                // Do not send the lastOffsetToAcquire as when the subMap is empty, it means that
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19555: restrict records acquisition post max in-flight limit (#20253).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
