**Commit**: 25aae99033 · **Ticket**: KAFKA-19808
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR fixes couple of issues which are related to the batch alignment:

1. Base offset not found for some acquisition lock timeouts. 2. Gaps reported by client when the topic cannot have any gaps.

**Before/After**
```java
// Before
        log.debug("Maybe initialize share partition: {}-{}", groupId, topicIdPartition);
            NavigableMap<Long, InFlightBatch> subMap = cachedState.subMap(baseOffset, true, lastBatch.lastOffset(), true);
                // Do not send the lastOffsetToAcquire as when the subMap is empty, it means that
                // there isn't any overlap itself.
                    firstBatch.baseOffset(), lastBatch.lastOffset(), batchSize, maxRecordsToAcquire);
                boolean fullMatch = checkForFullMatch(inFlightBatch, firstBatch.baseOffset(), lastBatch.lastOffset());

// After
        log.trace("Maybe initialize share partition: {}-{}", groupId, topicIdPartition);
                log.debug("Initialized share partition: {}-{} with persister read state: {}, cached state: {}",
                    groupId, topicIdPartition, result.topicsData(), cachedState);
            NavigableMap<Long, InFlightBatch> subMap = cachedState.subMap(baseOffset, true, lastOffsetToAcquire, true);
                // It's safe to use lastOffsetToAcquire instead of lastBatch.lastOffset() because there is no
                // overlap hence the lastOffsetToAcquire is same as lastBatch.lastOffset() or before that.
                    firstBatch.baseOffset(), lastOffsetToAcquire, batchSize, maxRecordsToAcquire);
                        // It's safe to use entry.getKey() - 1 as the last offset to acquire for the
                        // gap as the sub map should contain either the next batch or this line should
                        // not have been executed i.e. say there is a gap from 10-20 and cache contains
                        // [0-9, 21-30], when fetch returns single/multiple batches from 0-15, then
                        // first sub map entry has no gap and there exists only 1 entry in sub map.
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19808: handle batch alignment when share partition is at capacity (#20738).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
