# Bug 2692: Counter updates double-applied on commitlog replay

**Commit**: dec698f840 · **Ticket**: CASSANDRA-2419

## Detection Rule
When writing SSTables during flush or compaction, embed the maximum commitlog `ReplayPosition` covered by the source data so that replay can skip mutations already persisted to those SSTables, preventing counter increments from being applied twice.

## Code Smell
`SSTableWriter` constructed without a `ReplayPosition` argument, so the generated SSTable has no replay position metadata and all of its mutations will be re-applied on the next commitlog recovery.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: missing-replay-position-in-sstable
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
Counter columns accumulate increments; applying the same increment twice produces an incorrect (inflated) count. The commitlog replay logic uses the replay position stored in each SSTable to skip mutations it has already persisted. Because `SSTableWriter` was not receiving the `ReplayPosition` context at flush or compaction time, the written SSTables had no such marker, causing all covered mutations—including counter increments—to be replayed from the commitlog on restart. The fix threads the flush-time `ReplayPosition` (or the max position across compacted SSTables) through to `SSTableWriter`.

## Example
```java
// BEFORE
public SSTableWriter createFlushWriter(long estimatedRows, long estimatedSize)
{
    return new SSTableWriter(getFlushPath(...), estimatedRows, metadata, partitioner);
}
```
```java
// AFTER
public SSTableWriter createFlushWriter(long estimatedRows, long estimatedSize, ReplayPosition context)
{
    return new SSTableWriter(getFlushPath(...), estimatedRows, metadata, partitioner, context);
}
public SSTableWriter createCompactionWriter(long estimatedRows, String location,
        Collection<SSTableReader> sstables)
{
    ReplayPosition rp = ReplayPosition.getReplayPosition(sstables);
    return new SSTableWriter(getTempSSTablePath(location), estimatedRows, metadata, partitioner, rp);
}
```
