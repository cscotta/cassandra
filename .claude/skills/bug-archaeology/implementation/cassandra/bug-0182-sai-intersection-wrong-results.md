# Bug 0182: SAI Intersection Queries Return Wrong Results

**Commit**: 6a7bef12ec · **Ticket**: CASSANDRA-19011
**Subsystem**: index/sai/disk/v1 (PostingListRangeIterator, KeyLookup, WidePrimaryKeyMap)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Three independent defects combined to produce incorrect results for SAI multi-column intersection queries:

1. **`PostingListRangeIterator.performSkipTo`**: the guard `skipToToken.compareTo(nextKey) >= 0` was an off-by-one — using `>=` instead of `>`. This caused the iterator to skip calls when `nextKey` equalled the stored skip target, preventing forward progress to the next qualifying key.

2. **`KeyLookup.clusteredSeekToKey`** (binary search): the original block-level search used a flawed "split" strategy that repeatedly halved a split offset rather than a proper `[low, high)` interval, leading to the wrong block being selected and clustering-key lookups returning stale or incorrect keys.

3. **`WidePrimaryKeyMap.startOfNextPartition`**: it located the start of the next partition by calling `partitionArray.indexOf(++partitionId)`, which performs a binary search in the monotonic `rowId→partitionId` array. Because that array stores partition IDs per row rather than partition boundaries, the search was unreliable. The fix introduces a new on-disk component (`PARTITION_TO_SIZE`) that maps each partition ID to its row count, enabling a direct O(1) calculation: `rowId + partitionSize`.

### Before / After

```java
// BEFORE – PostingListRangeIterator: skips when equal, preventing re-visit
if (skipToToken != null && skipToToken.compareTo(nextKey) >= 0)
    return;
```

```java
// AFTER – only skip if the stored target is strictly greater
if (skipToKey != null && skipToKey.compareTo(nextKey) > 0)
    return;
```

```java
// BEFORE – WidePrimaryKeyMap.startOfNextPartition: unreliable indexOf
long partitionId = partitionArray.get(rowId);
long nextPartitionRowId = partitionArray.indexOf(++partitionId);
if (nextPartitionRowId == -1) nextPartitionRowId = partitionArray.length();
return nextPartitionRowId;
```

```java
// AFTER – direct lookup via PARTITION_TO_SIZE component
long partitionSize = partitionIdToSizeArray.get(rowIdToPartitionIdArray.get(rowId));
return partitionSize == -1 ? rowIdToPartitionIdArray.length() : rowId + partitionSize;
```

---

## How to Spot

Iterator `skipTo` guards that use `>=` instead of `>` cause the iterator to ignore a valid skip target that equals the current key, effectively making the iterator unable to revisit a position. During review, verify that skip-guard comparisons use strict inequality (`>`) so a call to advance to the current position is not silently dropped.

---

## Test

A new `RandomIntersectionTest` was added that generates random data, executes multi-column intersection queries, and validates results against a reference implementation across many random seeds.
