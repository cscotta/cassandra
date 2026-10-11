**Commit**: 24d03a18ef · **Ticket**: KAFKA-19517

**Subsystem**: KRaft / MetadataLoader

**Category**: Logic error in condition

**Root cause**
`LoadSummary#numRecords` counted only data records in a batch but skipped control records (COMMIT, ABORT, and other control batch types). The counter is used for observability and quota enforcement; under-counting control records means operators see an inaccurate picture of metadata throughput. The fix adds `loadStats.numRecords++` inside the control-batch processing loop so that every record type contributes to the count.

**Before/After**
```java
// Before: control batch loop does not increment numRecords
for (Record record : batch) {
    handleControlRecord(record);
    // numRecords not incremented
}

// After: control records counted
for (Record record : batch) {
    handleControlRecord(record);
    loadStats.numRecords++;
}
```

**How to spot**
Look for loops that iterate over `RecordBatch` entries in metadata loading code and check whether the `numRecords` (or equivalent counter) increment is inside all loops or only inside the data-record loop.

**Test**
`MetadataLoaderTest` verifies that `LoadSummary.numRecords` reflects the total count including control records when a batch containing both data and control entries is processed.
