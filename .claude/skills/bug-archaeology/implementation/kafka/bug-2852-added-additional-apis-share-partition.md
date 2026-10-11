**Commit**: adee6bae45 · **Ticket**: KAFKA-16740
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Added additional APIs for SharePartition which shall be used by SharePartitionManager. The lock API on SharePartition helps not issuing concurrent fetch request on replica manager for same SharePartition. The updateCacheAndOffsets API helps to update the cache and corresponding offsets when an exception is encountered in SharePartitionManager because of movement of Log Start Offset. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Updates the cached state, start and end offsets of the share partition as per the new log
     * start offset. The method is called when the log start offset is moved for the share partition.
     *
     * @param logStartOffset The new log start offset.
     */
    void updateCacheAndOffsets(long logStartOffset) {
        // TODO: Provide implementation to update cache and offsets on LSO movement.
    }

    /**
     * Checks if the number of records between startOffset and endOffset exceeds the record max
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16740: added additional apis for share partition (#16340).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
