# Bug 2542: Truncate allows post-restart data replay from commitlog

**Commit**: 59dfe48638 · **Ticket**: CASSANDRA-3297

## Detection Rule
When implementing truncate, flush the target column family *before* calling `CommitLog.forceNewSegment()` so that the truncated CF's memtable is written to disk and the old commitlog segment is eligible for deletion; failing to do so leaves unflushed mutations in the commitlog that will be replayed on restart.

## Code Smell
`CommitLog.instance.forceNewSegment(); ReplayPosition position = CommitLog.instance.getContext(); for (CFS cfs : all()) cfs.forceFlush();` — the new segment is created and the position is recorded *before* the truncated CF is flushed, so on recovery the replay position points past the truncated data's segment, but that segment may not have been written to disk yet and is replayed again.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: flush-after-segment-rotation-too-late
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
`ColumnFamilyStore.truncate()` rotated the commitlog to a new segment and captured the replay position, then flushed all CFs. However if the truncated CF's memtable had not been flushed before the segment rotation, its in-memory mutations were not yet on disk. When the commitlog cleaning code ran, the old segment (containing the pre-truncation data) was not yet eligible for deletion. On node restart, replay would re-apply those mutations, making data that the user thought was truncated reappear. The fix calls `forceBlockingFlush()` on the truncated CF *before* creating the new segment, ensuring its memtable is on disk and its old commitlog segment can be safely discarded.

## Example
```java
// BEFORE — flush happens after segment rotation
CommitLog.instance.forceNewSegment();
ReplayPosition position = CommitLog.instance.getContext();
for (ColumnFamilyStore cfs : ColumnFamilyStore.all())
    cfs.forceFlush();
```
```java
// AFTER — flush target CF first, then rotate
forceBlockingFlush();                     // flush this CF before rotation
CommitLog.instance.forceNewSegment();
ReplayPosition position = CommitLog.instance.getContext();
for (ColumnFamilyStore cfs : ColumnFamilyStore.all())
    cfs.forceFlush();
```
