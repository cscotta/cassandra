# Bug 0592: SASI View Creation Corrupted During Index Redistribution

**Commit**: ab8348c578 · **Ticket**: CASSANDRA-14055
**Subsystem**: `org.apache.cassandra.index.sasi.conf.view`
**Category**: Logic error in condition

---

## Root Cause

When the `IndexSummaryManager` redistributes index summaries it reopens SSTable readers, producing new `SSTableReader` instances that wrap the same on-disk files as the old ones. The SASI `View` constructor built the new view by iterating `Iterables.concat(currentView, newIndexes)` and skipping any index whose SSTable was in `oldSSTables`. Because the redistribution may reopen an SSTable that also appears in `newIndexes`, the old-SSTable set and the new-index set could overlap, so the skip condition fired for newly-created indexes and they were silently dropped from the view, making the index invisible to queries.

The secondary flaw was iteration order: `currentView` was prepended before `newIndexes`, so the old entry won the `newView.containsKey` duplicate check and the fresh entry was again discarded.

### Before / After

```java
// BEFORE
for (SSTableIndex sstableIndex : Iterables.concat(currentView, newIndexes))
{
    SSTableReader sstable = sstableIndex.getSSTable();
    if (oldSSTables.contains(sstable) || sstable.isMarkedCompacted()
            || newView.containsKey(sstable.descriptor))
    {
        sstableIndex.release();
        continue;
    }
```

```java
// AFTER
Collection<SSTableReader> toRemove = new HashSet<>(oldSSTables);
toRemove.removeAll(newIndexes.stream().map(SSTableIndex::getSSTable)
                             .collect(Collectors.toSet()));
// newIndexes first so they win the containsKey duplicate check
for (SSTableIndex sstableIndex : Iterables.concat(newIndexes, currentView))
{
    SSTableReader sstable = sstableIndex.getSSTable();
    if (toRemove.contains(sstable) || sstable.isMarkedCompacted()
            || newView.containsKey(sstable.descriptor))
    {
        sstableIndex.release();
        continue;
    }
```

---

## How to Spot

Any view-building loop that skips entries based on a set membership check where membership was computed before the loop is a candidate for this pattern. Reviewers should ask: "Can an element appear in *both* the 'remove' set and the 'keep' set?" — if yes, the remove set must be narrowed to the genuine removals (the set difference) before the loop executes. Iteration order also matters when a duplicate-detection map is involved: new entries must come first so they are not shadowed by stale ones.

---

## Test

`testIndexRedistribution` repeatedly calls `IndexSummaryManager.redistributeSummaries()` after loading data and asserts that all 10 expected rows remain queryable via the SASI index after each redistribution.
