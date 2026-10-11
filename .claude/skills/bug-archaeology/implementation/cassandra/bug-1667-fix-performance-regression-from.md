# Bug 1667: Fix Performance Regression from CASSANDRA-5614

**Commit**: 93edb123a1 · **Ticket**: CASSANDRA-6949
**Subsystem**: src/java/org/apache/cassandra/db/AtomicSortedColumns.java
**Category**: Logic error in condition

---

## Root Cause

CASSANDRA-5614 added logic in `AtomicSortedColumns#addAllWithSizeDelta` to proactively notify secondary-index indexers about columns that would be deleted by incoming range tombstones. This traversal iterated over both the existing columns and the incoming columns, calling `indexer.remove()` for each one covered by the deletion info. This was incorrect: the normal per-column add path already handles index removal when it detects a superseded column, so the extra loop was redundant and caused double-remove calls to the secondary index, degrading write performance.

### Before / After

```java
// BEFORE – extra loop removes from secondary index for every column under a range tombstone
if (cm.deletionInfo().hasRanges())
{
    for (Column currentColumn : Iterables.concat(current.map.values(), cm))
    {
        if (cm.deletionInfo().isDeleted(currentColumn))
            indexer.remove(currentColumn);
    }
}
```

```java
// AFTER – block removed entirely; per-column path handles index updates correctly
// (no replacement code needed)
```

---

## How to Spot

When a secondary-index notification is added "just to be safe" in one code path, verify it is not already emitted by another path for the same event. Double-remove or double-insert calls to secondary indexes will not cause correctness failures immediately but silently degrade write throughput and can corrupt index state.

---

## Test

The existing `testMemtableUpdateWithRangeTombstonesUpdatesSecondaryIndex` test was removed (it asserted the now-incorrect double-remove count of 2). The related `testOverwritesToDeletedColumns` test was updated to assert `assertEquals(1, index.deletes.size())` reflecting the correct single removal.
