# Bug 0463: Empty Range Tombstones Emitted from RangeTombstoneList

**Commit**: ab6a87bf60 · **Ticket**: CASSANDRA-15924
**Subsystem**: `src/java/org/apache/cassandra/db/RangeTombstoneList.java`, `db/rows/RowAndDeletionMergeIterator.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When iterating range tombstones intersected against a query slice, `RangeTombstoneList` computed a clamped sub-range by taking the maximum of the tombstone start and the query-slice start, and the minimum of the tombstone end and the query-slice end. It did not check whether this intersection produced an empty range (i.e., the clamped start was already past the clamped end), so it could emit a `RangeTombstone` whose start bound equaled or exceeded its end bound. Downstream, `RowAndDeletionMergeIterator` consumed these degenerate tombstones and attempted to suppress rows based on them; during read repair this caused phantom deletions to be propagated between replicas in a loop, as each side would send an empty-range repair mutation to the other. A second guard was added in `RowAndDeletionMergeIterator` to skip any range tombstone whose slice `isEmpty()`.

### Before / After

```java
// BEFORE — no check; could emit range tombstone with start >= end
Slice.Bound s = comparator.compare(starts[start], slice.start()) < 0
    ? slice.start() : starts[start];
Slice.Bound e = comparator.compare(slice.end(), ends[start]) < 0
    ? slice.end() : ends[start];
return Iterators.singletonIterator(rangeTombstoneWithNewBounds(start, s, e));
```

```java
// AFTER — skip the tombstone if the intersection is empty
if (Slice.isEmpty(comparator, s, e))
    return Collections.emptyIterator();
return Iterators.singletonIterator(rangeTombstoneWithNewBounds(start, s, e));

// RowAndDeletionMergeIterator also skips empty-slice tombstones
if ((removeShadowedData && partitionLevelDeletion().supersedes(nextRange.deletionTime()))
    || nextRange.deletedSlice().isEmpty(metadata.comparator))
    nextRange = null;
```

---

## How to Spot

Any code that clips a range to an intersection and then creates a range object from the clipped bounds should verify that the resulting range is non-empty before using it. In Cassandra, `Slice.isEmpty(comparator, s, e)` is the idiomatic check; skipping it means callers will receive tombstones with degenerate bounds that can trigger assertion failures, incorrect row suppression, or cascading read-repair loops. Reviewers should look for paired min/max bound clipping followed by an object construction with no emptiness guard.

---

## Test

A two-node distributed test (`ReadRepairTest`) creates a table, inserts a range tombstone on one replica, then executes queries with slice predicates that produce empty intersections (e.g., `column1 > 10 AND column1 <= 10`) at consistency ALL, asserting that read repair does not loop indefinitely and that subsequent deletes on the second node succeed, verifying that empty range tombstones are not propagated.
