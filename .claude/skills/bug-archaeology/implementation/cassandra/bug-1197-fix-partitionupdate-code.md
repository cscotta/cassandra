# Bug 1197: PartitionUpdate Sort/Merge Only Merges Cell Data, Loses Liveness and Deletion Info

**Commit**: 87c6daf6e3 · **Ticket**: CASSANDRA-9743
**Subsystem**: src/java/org/apache/cassandra/db/partitions/PartitionUpdate.java
**Category**: Logic error in condition

---

## Root Cause

During the deduplication sort in `PartitionUpdate`, when two rows with the same clustering key were found, the code called `data.merge(current, previous, nowInSec)` and `data.move(current, previous)` directly on the underlying cell-data array. This silently skipped merging the per-row liveness information (`livenessInfos`) and deletion state (`deletions`), which are parallel arrays managed separately. The result was that after sorting, surviving rows could have stale or missing timestamps and deletion markers. The fix introduces `merge()` and `move()` methods on `AbstractPartitionData` that operate on all three parallel arrays atomically.

### Before / After

```java
// BEFORE — only merges cell data, ignores livenessInfos and deletions
data.merge(current, previous, nowInSec);
// ...
data.move(current, previous);
```

```java
// AFTER — merge() and move() on AbstractPartitionData cover all arrays
merge(current, previous, nowInSec);
// ...
move(current, previous);
```

---

## How to Spot

When a data structure stores logically related fields in multiple parallel arrays (cells, liveness, deletions), every mutation operation that touches one array must touch all related arrays. Look for helpers that only accept or return one of the parallel structures — they are likely incomplete. Review all sort/merge/move primitives to confirm they are defined at the level that owns all the arrays.

---

## Test

`RowBuilderTest` was added, exercising a multi-row mutation built with `RowUpdateBuilder` to verify that both rows survive through the sort/merge path and are correctly persisted.
