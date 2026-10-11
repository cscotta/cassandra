# Bug 0099: Multiple SAI Single-Node Query Bugs with Static Columns

**Commit**: 4e2e5f3c57 · **Ticket**: CASSANDRA-20338
**Subsystem**: index/sai
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Four independent defects in the Storage-Attached Index (SAI) subsystem, all triggered by the presence of static columns or composite partition keys:

1. **MemtableIndexWriter min/max tracking**: When an index covers a partition-key element, the static-row key (kind `STATIC`) was not counted when updating `minKey`/`maxKey` statistics, causing the index to report incorrect bounds and miss or misorder results.

2. **Dead rows indexed**: `StorageAttachedIndexGroup.insertRow` used `row.deletion().isLive()` to skip deletions, but that check only tests the row-level deletion marker, not whether any live cells remain. Rows consisting entirely of cell-level tombstones were still indexed, creating phantom postings. The fix uses `row.hasLiveData(nowInSec, false)` in both `insertRow` and `updateRow`, and the flush-time `StorageAttachedIndexWriter` gains a matching guard.

3. **Intersection min > max with static keys**: `KeyRangeIntersectionIterator` derives the intersection range as `[maxOf(mins), minOf(maxs)]`. With static keys the `min` could advance past the `max`, producing an invalid (inverted) range and an empty or incorrect result. After updating `min` and `max`, the fix clamps `min = nullSafeMin(min, max)`.

4. **Last-key tracking ignores static key type**: The searcher in `StorageAttachedIndexSearcher` compared successive keys using `key.equals(lastKey)`, which internally calls `compareTo` and therefore treats a static-row key and its corresponding regular-row key as equal (same partition). Skipping what appeared to be a duplicate caused genuine regular rows to be dropped. The fix passes `strict=false` to a new `equals(Object, boolean)` overload that compares only the partition component when `strict` is false, so a static key and a regular key in the same partition are correctly treated as distinct.

### Before / After

```java
// BEFORE — insertRow only skips row-level deletion marker
if (row.deletion().isLive())
    for (Index.Indexer indexer : indexers)
        indexer.insertRow(row);
```

```java
// AFTER — skips rows with no live cells at all
if (row.hasLiveData(nowInSec, false))
    for (Index.Indexer indexer : indexers)
        indexer.insertRow(row);
```

```java
// BEFORE — intersection min/max, min can exceed max
min = nullSafeMax(min, range.getMinimum());
max = nullSafeMin(max, range.getMaximum());
```

```java
// AFTER — clamp min so it never exceeds max
min = nullSafeMax(min, range.getMinimum());
max = nullSafeMin(max, range.getMaximum());
min = nullSafeMin(min, max);   // correct inversion from static keys
```

---

## How to Spot

Index writers and iterators that distinguish between static rows and regular rows must apply those distinctions consistently across every path: insertion guards, statistics tracking, deduplication, and range computation. A mismatch — such as testing `row.deletion().isLive()` instead of `row.hasLiveData()` — can be caught during review by asking "does this check handle all forms of deletion (cell-level, range, partition)?" Similarly, whenever a comparator-based range is narrowed iteratively, add a post-condition assert that `min <= max`.

---

## Test

`CompositePartitionKeyIndexTest.testStaticAndNonStaticKeysOnFlush` and `testIgnoreCellDeletions`: verify that after multiple flushes with mixed inserts, partition-key deletes, and cell deletions the SAI query returns exactly the expected rows; `StaticColumnIndexTest.testTupleAndBlobFiltering` exercises static-plus-partition-key intersections with complex types.
