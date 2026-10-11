# Bug 0170: SAI Over-Skipping of Static-Column Key Iterators During Mixed Intersections

**Commit**: 87f2a7b763 · **Ticket**: CASSANDRA-19278
**Subsystem**: index/sai (KeyRangeIntersectionIterator, PrimaryKey, RowMapping)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a Storage-Attached Index (SAI) intersection query combines a predicate on a regular (WIDE) column with a predicate on a static column, the posting lists contain two different kinds of `PrimaryKey`: `WIDE` keys that encode a full partition+clustering pair, and `STATIC` keys that encode only a partition. The intersection iterator uses `skipTo(highestKey)` to advance lagging iterators. Advancing a static-key iterator directly to a `WIDE` target causes it to skip past the correct partition because the comparator treats `STATIC` and `WIDE` keys in the same partition as equal (both resolve to the same token/partition bytes), but `skipTo` internally overshoots to the next partition. The fix introduces `compareToStrict()` which orders `WIDE` after `STATIC` within the same partition, and a separate `toStatic()` projection so that static-key iterators are only asked to skip to the partition boundary rather than the full wide-key target.

Additionally, `RowMapping` was tracking a single set of min/max/count for all keys; static-column index segments were receiving the row-based min/max bounds of the whole SSTable instead of the narrower static-key bounds, causing the segment metadata to claim a range that was wider than its actual posting list, which in turn caused the intersection to misidentify which segments could contribute matches.

### Before / After

```java
// BEFORE – advance static iterator directly to a WIDE highestKey
PrimaryKey nextKey = nextOrNull(range, highestKey);
if (nextKey == null || nextKey.compareTo(highestKey) > 0) { ... }
```

```java
// AFTER – advance static iterator only to the partition of highestKey
PrimaryKey nextKey = range.getCurrent().kind() == Kind.STATIC
                     ? nextOrNull(range, highestKey.toStatic())
                     : nextOrNull(range, highestKey);
if (nextKey == null || nextKey.compareToStrict(highestKey) > 0) { ... }
```

---

## How to Spot

Any intersection iterator that mixes posting lists with different key granularities (e.g., row-level vs. partition-level) must use a target key projection when advancing the coarser iterator; passing the fine-grained key directly causes overshooting. Review `compareTo` implementations for types with multiple sub-kinds: if two keys of different sub-kinds can compare equal yet represent different positions in a posting list, a strict comparator variant is needed for intersection logic to avoid skipping valid matches.

---

## Test

New `RandomMixedIntersectionTest` and `TwoRegularOneStaticIntersectionTest` randomized tests assert that queries combining static and non-static SAI predicates return the same results as a reference full-table scan.
