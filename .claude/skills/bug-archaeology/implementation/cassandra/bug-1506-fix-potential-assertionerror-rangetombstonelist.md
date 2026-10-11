# Bug 1506: Fix potential AssertionError in RangeTombstoneList

**Commit**: 5bd37b92d2 · **Ticket**: CASSANDRA-7700
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Off-by-one

---

## Root Cause

This is a backport of the same fix as Bug 1505 to the 2.0.x branch. The `RangeTombstoneList` in the 2.0 codebase (which uses raw `ByteBuffer` comparisons rather than `Composite`) had identical off-by-one errors in `add()` and `addAll()`. When inserting a new tombstone whose `start` exactly matched an existing `ends[]` entry, the binary-search result was incorrectly advanced by one position (`pos+1`), causing `insertFrom` to violate the structural invariant and assert. The same boundary-equality gap in the merge loop could corrupt the list when tombstones from two lists shared a common boundary value.

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

Whenever a sorted structure is maintained with a binary search and the code adjusts the returned index for an exact match, verify whether the invariant requires the exact-match position or the next position. Here the list invariant is `ends[i-1] <= start <= ends[i]`, so an exact match belongs at `pos`, not `pos+1`.

---

## Test

Random stress test added in `RangeTombstoneListTest.addAllRandomTest`: generates 1000 pairs of random `RangeTombstoneList` objects (with intervals that may touch at boundaries) and merges them, asserting structural validity after each merge.
