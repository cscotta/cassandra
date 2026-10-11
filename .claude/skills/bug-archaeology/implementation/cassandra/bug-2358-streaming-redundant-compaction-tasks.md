# Bug 2358: Streaming triggers one background compaction per SSTable instead of one per batch

**Commit**: 150b3167f2 · **Ticket**: CASSANDRA-4174

## Detection Rule

When multiple SSTables are added to a `ColumnFamilyStore` during a batch operation (e.g., stream-in), verify that compaction scheduling is triggered once after all tables are added rather than once per individual SSTable addition.

## Code Smell

A loop calls `cfs.addSSTable(sstable)` for each received SSTable; if `addSSTable` internally calls `CompactionManager.submitBackground(this)`, N SSTables will submit N identical compaction tasks, all but one of which will find no work to do and waste scheduler overhead.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: per-item-vs-batch-trigger
- severity: performance
- subsystem: compaction

## Root Cause

`StreamInSession` was calling `cfs.addSSTable(sstable)` inside the per-SSTable loop. `addSSTable` called `data.addSSTables(...)` and then `CompactionManager.instance.submitBackground(this)`, so N streaming SSTables resulted in N background compaction submissions. The fix introduces `addSSTables(Collection<SSTableReader>)` that adds all tables at once before submitting a single background compaction, and `StreamInSession` is updated to group SSTables by `ColumnFamilyStore` and call the new batch method once per CFS.

## Example

```java
// BEFORE (per-SSTable submission)
cfs.addSSTable(sstable); // calls submitBackground internally — runs N times
```

```java
// AFTER (batch submission)
// group into cfstores map, then:
entry.getKey().addSSTables(entry.getValue()); // one submitBackground for the whole batch
```
