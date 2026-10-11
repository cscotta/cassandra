# Bug 1505: Fix potential AssertionError in RangeTombstoneList

**Commit**: 78952731f3 · **Ticket**: CASSANDRA-7700
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Off-by-one

---

## Root Cause

`RangeTombstoneList` maintains an ordered, non-overlapping list of range tombstones. The insertion logic in `add()` located the binary-search hit position for `start` among `ends[]` and then advanced the index by one (`pos+1`) before calling `insertFrom`. This was wrong when `start` exactly equaled an existing end value: advancing past that slot skipped the element that shares the boundary, leaving the new tombstone inserted at a position where the invariant `ends[i-1] <= start <= ends[i]` was violated and triggering an `AssertionError`.

A second off-by-one existed in `addAll()`: the merge loop advanced to the next incoming element only when `tombstones.starts[j] < ends[i]` (strict), missing the case where start exactly equals the current end, so the element was processed out of order and again violated the list invariant.

### Before / After

```java
// BEFORE — add(): skips the exact-match slot
insertFrom((pos >= 0 ? pos+1 : -pos-1), start, end, markedAt, delTime);

// BEFORE — addAll(): strict comparison misses boundary equality
if (comparator.compare(tombstones.starts[j], ends[i]) < 0)
```

```java
// AFTER — add(): stay at the exact-match slot
insertFrom((pos >= 0 ? pos : -pos-1), start, end, markedAt, delTime);

// AFTER — addAll(): include boundary-equal case
if (comparator.compare(tombstones.starts[j], ends[i]) <= 0)
```

---

## How to Spot

When two collections of range tombstones share a common boundary value (e.g., the end of one tombstone equals the start of the next), any code path that inserts or merges ranges using a binary-search index should treat an exact match as "insert at this position", not "insert after this position". Review every `pos+1` adjustment after a `binarySearch` call and confirm it is only used where the invariant requires strict advancement.

---

## Test

Random stress test added in `RangeTombstoneListTest.addAllRandomTest`: generates 1000 pairs of random `RangeTombstoneList` instances (up to 50 elements each, with intervals that may share boundaries) and calls `addAll`, then asserts structural validity of the result — specifically that ranges remain in order and no duplicate singleton intervals appear at the same boundary point.
