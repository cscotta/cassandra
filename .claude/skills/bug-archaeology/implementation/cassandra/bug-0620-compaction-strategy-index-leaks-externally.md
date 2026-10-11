# Bug 0620: CompactionManager Uses Internal Strategy Index Instead of DiskBoundaries

**Commit**: e208a6a210 · **Ticket**: CASSANDRA-14082
**Subsystem**: `db/compaction/CompactionManager.java`, `db/compaction/CompactionStrategyManager.java`
**Category**: Logic error in condition

---

## Root Cause

`CompactionManager.relocateSSTables` and `Scrubber` obtained a per-disk index by calling `CompactionStrategyManager.getCompactionStrategyIndex()`, which is an internal implementation detail of how strategies are partitioned across disks. This method could diverge from the actual disk layout recorded in `DiskBoundaries` when boundaries were refreshed between calls, leading to an SSTable being considered in the "wrong" location even when it was correct, or vice versa. The fix removes the public method and replaces all call sites with `DiskBoundaries.getDiskIndex()`, making the single source of truth for disk placement explicit.

### Before / After

```java
// BEFORE
Map<Integer, List<SSTableReader>> groupedByDisk =
    needsRelocation.stream().collect(Collectors.groupingBy((s) ->
        cfs.getCompactionStrategyManager().getCompactionStrategyIndex(s)));

int directoryIndex = cfs.getCompactionStrategyManager().getCompactionStrategyIndex(sstable);
Directories.DataDirectory location = locations[directoryIndex];
PartitionPosition diskLast = diskBoundaries.get(directoryIndex);
```

```java
// AFTER
Map<Integer, List<SSTableReader>> groupedByDisk = groupByDiskIndex(needsRelocation);
// groupByDiskIndex uses diskBoundaries.getDiskIndex(s)

int diskIndex = diskBoundaries.getDiskIndex(sstable);
File diskLocation = diskBoundaries.directories.get(diskIndex).location;
PartitionPosition diskLast = diskBoundaries.positions.get(diskIndex);
```

---

## How to Spot

When two independent objects (here `CompactionStrategyManager` and `DiskBoundaries`) both encode the same mapping — which disk an SSTable belongs to — they can drift out of sync. Any code that queries one object to filter or group data that is logically owned by the other is a code smell. Reviewers should verify that operations on SSTable disk placement consistently use a single snapshot of `DiskBoundaries` captured at the start of the operation.

---

## Test

No test added.
