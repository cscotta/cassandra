**Commit**: 49e9bd4a5b · **Ticket**: KAFKA-16754
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
About
Implemented release acquired records functionality in SharePartition. This functionality is used when a share session gets closed, hence all the acquired records should either move to AVAILABLE or ARCHIVED state. Implemented the following functions -

1. releaseAcquiredRecords - This function is executed when the acquisition lock timeout is reached.

**Before/After**
```java
// Before
        log.trace("Release acquired records request for share partition: {}-{}-{}", groupId, memberId, topicIdPartition);
        CompletableFuture<Optional<Throwable>> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Not implemented"));

// After
        log.trace("Release acquired records request for share partition: {}-{} memberId: {}", groupId, topicIdPartition, memberId);
        Throwable throwable = null;
        lock.writeLock().lock();
        List<InFlightState> updatedStates = new ArrayList<>();
        List<PersisterStateBatch> stateBatches = new ArrayList<>();

        try {
            RecordState recordState = RecordState.AVAILABLE;
            // Iterate over multiple fetched batches. The state can vary per offset entry
            for (Map.Entry<Long, InFlightBatch> entry : cachedState.entrySet()) {
                InFlightBatch inFlightBatch = entry.getValue();

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16754: implemented release acquired records functionality to sharepartition (#16430).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
