**Commit**: 49ee1fb4f9 · **Ticket**: KAFKA-19632
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR fixes the batch alignment issue when partitions are re-assigned. During initial read of state the batches can be broken arbitrarily. Say
the start offset is 10 and cache contains [15-18] batch during
initialization. When fetch happens at offset 10 and say the fetched
batch contain 10 records i.e.

**Before/After**
```java
// Before
                    gapStartOffset = entry.getValue().lastOffset() + 1;
            // of 12-13. Hence, floor entry is fetched to find the sub-map.
            // We might find a batch with floor entry but not necessarily that batch has an overlap,
            // if the request batch base offset is ahead of last offset from floor entry i.e. cached
            // batch of 10-14 and request batch of 15-18, though floor entry is found but no overlap.
            // Such scenario will be handled in the next step when considering the subMap. However,
            // if the floor entry is found and the request batch base offset is within the floor entry
            // then adjust the base offset to the floor entry so that acquire method can still work on
            // previously cached batch boundaries.
            if (floorEntry != null && floorEntry.getValue().lastOffset() >= baseOffset) {

// After
                    // If the gapStartOffset is already past the last offset of the in-flight batch,
                    // then do not consider this batch for finding the next fetch offset. For example,
                    // consider during initialization, the initialReadGapOffset is set to 5 and the
                    // first cached batch is 15-18. First read will happen at offset 5 and say the data
                    // fetched is [5-6], now next fetch offset should be 7. This works fine but say
                    // subsequent read returns batch 8-11, and the gapStartOffset will be 12. Without
                    // the max check, the next fetch offset returned will be 7 which is incorrect.
                    // The natural gaps for which no data is available shall be considered hence
                    // take the max of the gapStartOffset and the last offset of the in-flight batch.
                    gapStartOffset = Math.max(entry.getValue().lastOffset() + 1, gapStartOffset);
            // of 12-13. Hence, floor entry is fetched to find the sub-map. Secondly, when the share
            // partition is initialized with persisted state, the start offset might be moved to a later
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19632: handle overlap batch on partition re-assignment (#20395).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
