**Commit**: 06400177bb · **Ticket**: KAFKA-18452
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR implements a way to divide acquired batches into batch size as desirable by client. The BatchSize is the soft limit and should align the batches in response and cached state in broker at the log batch boundaries. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Abhinav Dixit <adixit@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
                ShareAcquiredRecords shareAcquiredRecords = sharePartition.acquire(shareFetch.memberId(), shareFetch.maxFetchRecords() - acquiredRecordsCount, fetchPartitionData);
                AcquiredRecords acquiredRecords = acquireNewBatchRecords(memberId, fetchPartitionData.records.batches(),
                    firstBatch.baseOffset(), lastBatch.lastOffset(), maxFetchRecords);
                return ShareAcquiredRecords.fromAcquiredRecords(acquiredRecords);
                AcquiredRecords acquiredRecords = acquireNewBatchRecords(memberId, fetchPartitionData.records.batches(),
                    lastBatch.lastOffset(), maxFetchRecords - acquiredCount);
                result.add(acquiredRecords);
                acquiredCount += (int) (acquiredRecords.lastOffset() - acquiredRecords.firstOffset() + 1);
    private AcquiredRecords acquireNewBatchRecords(
            // Schedule acquisition lock timeout for the batch.
            AcquisitionLockTimerTask timerTask = scheduleAcquisitionLockTimeout(memberId, firstAcquiredOffset, lastAcquiredOffset);
            // Add the new batch to the in-flight records along with the acquisition lock timeout task for the batch.

// After
                ShareAcquiredRecords shareAcquiredRecords = sharePartition.acquire(shareFetch.memberId(), shareFetch.batchSize(), shareFetch.maxFetchRecords() - acquiredRecordsCount, fetchPartitionData);
     * @param batchSize          The number of records per acquired records batch.
        int batchSize,
                return acquireNewBatchRecords(memberId, fetchPartitionData.records.batches(),
                    firstBatch.baseOffset(), lastBatch.lastOffset(), batchSize, maxFetchRecords);
                ShareAcquiredRecords shareAcquiredRecords = acquireNewBatchRecords(memberId, fetchPartitionData.records.batches(),
                    lastBatch.lastOffset(), batchSize, maxFetchRecords - acquiredCount);
                result.addAll(shareAcquiredRecords.acquiredRecords());
                acquiredCount += shareAcquiredRecords.count();
    private ShareAcquiredRecords acquireNewBatchRecords(
        int batchSize,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18452: implemented batch size in acquired records (#18459).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
