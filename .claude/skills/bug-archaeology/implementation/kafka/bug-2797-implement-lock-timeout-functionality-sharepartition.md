**Commit**: 3f3b070a6a · **Ticket**: KAFKA-16755
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Implemented acquisition lock timeout functionality in SharePartition. Implemented the following functions -

1. releaseAcquisitionLockOnTimeout - This function is executed when the acquisition lock timeout is reached. The function releases the acquired records.

**Before/After**
```java
// Before
        // TODO: Implement the logic to release the acquisition lock on timeout.

// After
        lock.writeLock().lock();
        try {
            Map.Entry<Long, InFlightBatch> floorOffset = cachedState.floorEntry(firstOffset);
            if (floorOffset == null) {
                log.error("Base offset {} not found for share partition: {}-{}", firstOffset, groupId, topicIdPartition);
                return;
            }
            List<PersisterStateBatch> stateBatches = new ArrayList<>();
            NavigableMap<Long, InFlightBatch> subMap = cachedState.subMap(floorOffset.getKey(), true, lastOffset, true);
            for (Map.Entry<Long, InFlightBatch> entry : subMap.entrySet()) {
                InFlightBatch inFlightBatch = entry.getValue();

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16755: implement lock timeout functionality in sharepartition (#16414).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
