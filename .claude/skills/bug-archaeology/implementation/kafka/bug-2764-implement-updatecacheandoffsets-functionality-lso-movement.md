**Commit**: 35baa0ac4f · **Ticket**: KAFKA-17026
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Implemented the functionality which takes care of archiving the records when LSO moves past them. Implemented the following functions -

1. updateCacheAndOffsets - Updates the cached state, start and end offsets of the share partition as per the new log start offset. The method is called when the log start offset is moved for the share partition.

**Before/After**
```java
// Before
              files="RemoteLogManagerTest.java"/>
        // TODO: Provide implementation to update cache and offsets on LSO movement.

// After
              files="(RemoteLogManagerTest|SharePartitionTest).java"/>
        lock.writeLock().lock();
        try {
            if (logStartOffset <= startOffset) {
                log.error("The log start offset: {} is not greater than the start offset: {} for the share partition: {}-{}",
                        logStartOffset, startOffset, groupId, topicIdPartition);
                return;
            }
            log.debug("Updating start offset for share partition: {}-{} from: {} to: {} since LSO has moved to: {}",
                    groupId, topicIdPartition, startOffset, logStartOffset, logStartOffset);
            if (cachedState.isEmpty()) {
                // If the cached state is empty, then the start and end offset will be the new log start offset.
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17026: implement updatecacheandoffsets functionality on lso movement (#16459).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
