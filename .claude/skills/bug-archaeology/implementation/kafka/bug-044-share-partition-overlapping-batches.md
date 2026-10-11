**Commit**: 18dbd52278 · **Ticket**: KAFKA-19954
**Subsystem**: core/src/main/java/kafka/server/share/SharePartition.java
**Category**: Off-by-one

**Root cause**
After a `SharePartition` is re-initialized with a moved `startOffset`, the `acquire` method adjusted `baseOffset` to align with the floor cache entry. When searching for overlapping batches with `cachedState.subMap(baseOffset, ...)`, the code used the adjusted `baseOffset` as the lower key. If the batch's actual `firstBatch.baseOffset()` was less than the adjusted `baseOffset`, no overlap was detected, and a new cache entry was created with the wrong first offset — causing overlapping entries keyed by different offsets for the same underlying data. The fix uses `Math.min(firstBatch.baseOffset(), baseOffset)` as the submap lower bound to capture all potential overlaps.

**Before/After**
```java
// Before – submap lower bound misses pre-adjustment base offset
NavigableMap<Long, InFlightBatch> subMap =
    cachedState.subMap(baseOffset, true, lastOffsetToAcquire, true);

// After – always include the fetch batch's own base offset in overlap check
final NavigableMap<Long, InFlightBatch> subMap = cachedState.subMap(
    Math.min(firstBatch.baseOffset(), baseOffset), true, lastOffsetToAcquire, true);
```

**How to spot**
After any `baseOffset` adjustment (e.g., aligning to `startOffset` or a floor entry), the adjusted value must not silently exclude the original batch start from overlap checks. Always use the minimum of original and adjusted offsets as the lower bound.

**Test**
`SharePartitionTest#testAcquireBatchInRecordLimitModeWithMovedStartOffset`: initializes a share partition with `startOffset=5`, sends a batch of 0-99, acquires 10 records, acknowledges 5-14, then re-acquires from the same 0-99 batch and asserts only offsets 15-24 are acquired with no spurious cache entries.
