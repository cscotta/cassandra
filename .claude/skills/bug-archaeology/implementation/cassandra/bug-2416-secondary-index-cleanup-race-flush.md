# Bug 2416: Secondary index cleanup during compaction races with concurrent memtable flush

**Commit**: 9ca84786b5 · **Ticket**: CASSANDRA-3712

## Detection Rule

When compaction deletes rows from a secondary index CFS, it must hold the same lock that `Table.apply` holds during writes; otherwise the index delete and a concurrent flush can interleave and corrupt the index.

## Code Smell

`cfs.indexManager.deleteFromIndexes(row.getKey(), indexedColumnsInRow)` called without holding `Table.switchLock`, while `Table.apply` holds the read lock of the same RRWL during index updates — the missing lock allows the compaction's index delete to race with a concurrent flush.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: missing-lock-for-index-mutation
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

During cleanup compaction, `CompactionManager` would call `indexManager.deleteFromIndexes` to remove entries for cleaned-up rows from secondary index column families. This call was made without holding `Table.switchLock`, which `Table.apply` (the write path) holds as a read lock. A concurrent flush of the secondary index CFS triggered by a write could interleave with the compaction's delete, corrupting the index. The fix acquires `Table.switchLock.readLock()` around `deleteFromIndexes` during compaction cleanup, mirroring what the write path does. Also `executor.finishCompaction(ci)` was called twice; one duplicate call was removed.

## Example

```java
// BEFORE — unprotected index delete
if (indexedColumnsInRow != null && !indexedColumnsInRow.isEmpty())
    cfs.indexManager.deleteFromIndexes(row.getKey(), indexedColumnsInRow);  // races with flush
```

```java
// AFTER — lock acquired to match write-path locking
if (indexedColumnsInRow != null && !indexedColumnsInRow.isEmpty()) {
    Table.switchLock.readLock().lock();
    try {
        cfs.indexManager.deleteFromIndexes(row.getKey(), indexedColumnsInRow);
    } finally {
        Table.switchLock.readLock().unlock();
    }
}
```
