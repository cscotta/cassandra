# Bug 1822: Row Tombstones Lost in Larger-Than-Memory Compactions

**Commit**: 3edb62bf77 · **Ticket**: CASSANDRA-6008
**Subsystem**: src/java/org/apache/cassandra/db/compaction, src/java/org/apache/cassandra/db
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`LazilyCompactedRow` (used for compactions where rows don't fit in memory) merged input SSTables' `ColumnFamily` objects together using `rawCf.delete(cf)`, which combined deletion info but also loaded columns into memory — exactly what lazy compaction tries to avoid. More critically, `ColumnFamilyStore.removeDeleted()` was called on this merged `rawCf` to purge tombstones, but the resulting `emptyColumnFamily` then had its deletion info stripped when the per-column iterator's `container.clear()` was called during the merge phase. The fix separates concerns: deletion info is accumulated directly from `DeletionInfo` objects (never through full CF merging), stored in `LazilyCompactedRow.deletionInfo`, and explicitly re-applied to the container on each iteration step. The `purgeTombstones` call (renamed from `maybeResetDeletionTimes`) is applied to `emptyColumnFamily` only once, at construction time. This ensures that row-level tombstones are preserved and correctly suppress older columns even when the row spans multiple SSTable chunks.

### Before / After

```java
// BEFORE – merged full CFs (loads columns); tombstone info lost when container cleared
ColumnFamily rawCf = null;
for (OnDiskAtomIterator row : rows)
{
    ColumnFamily cf = row.getColumnFamily();
    if (rawCf == null) rawCf = cf;
    else               rawCf.delete(cf);
}
ColumnFamily purgedCf = ColumnFamilyStore.removeDeleted(rawCf, overriddenGcBefore);
emptyColumnFamily = purgedCf == null ? ArrayBackedSortedColumns.factory.create(...) : purgedCf;
```

```java
// AFTER – accumulates only DeletionInfo; deletion re-applied per iteration step
deletionInfo = DeletionInfo.live();
for (OnDiskAtomIterator row : rows)
{
    DeletionInfo delInfo = row.getColumnFamily().deletionInfo();
    deletionInfo = deletionInfo.add(delInfo);
}
emptyColumnFamily = ArrayBackedSortedColumns.factory.create(controller.cfs.metadata);
emptyColumnFamily.setDeletionInfo(deletionInfo.copy());
if (shouldPurge)
    emptyColumnFamily.purgeTombstones(controller.gcBefore);
// ... inside getReduced():
container.setDeletionInfo(deletionInfo);  // re-applied after each container.clear()
```

---

## How to Spot

Any compaction path that merges deletion metadata through a full `ColumnFamily` object should be treated with suspicion: `CF.delete(CF)` merges tombstones but also pulls column data, defeating lazy loading. Additionally, look for any place where a container is cleared (via `clear()`) while an outer object holds a reference to deletion info that was set on that container — the clear silently resets tombstone state.

---

## Test

`CompactionsPurgeTest.testRowTombstoneObservedBeforePurging` was added. It writes a row to one SSTable, a row tombstone to a second SSTable, compacts with a `gcBefore` that does NOT purge the tombstone (verifying the tombstone survives and still hides the data), then repeats with a `gcBefore` that IS old enough to purge (verifying both data and tombstone are removed).
