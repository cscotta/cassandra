# Bug 2661: Concurrent compaction threads can compact the same SSTables because markCompacting returns stale task sstable set

**Commit**: 33983738fa · **Ticket**: CASSANDRA-2769

## Detection Rule
When marking SSTables as compacting, use the return value of `DataTracker.markCompacting()` (the subset that was actually atomically reserved) and update the task's SSTable list to that subset; do not pass the original unchecked set to the compaction execution.

## Code Smell
`DataTracker.markCompacting(AbstractCompactionTask)` calls `view.markCompacting(sstablesToMark)` but the task still holds a reference to the original `sstablesToMark` collection, not the atomically-reserved subset; a second thread can race and also mark the same SSTables before the first CAS completes, leading to two threads compacting the same files.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: compacting-mark-not-updated-in-task
- severity: data-loss
- subsystem: compaction

## Root Cause
The old `DataTracker.markCompacting(AbstractCompactionTask)` method did not return the subset of SSTables that were actually successfully marked; the task's own `sstables` field was never updated to reflect the atomically-confirmed subset. This meant `AbstractCompactionTask.execute()` could operate on SSTables that another thread had already claimed. The fix moves the marking logic into `AbstractCompactionTask.markSSTablesForCompaction()`, which updates `this.sstables` to the returned subset, ensuring the task only compacts files it successfully reserved.

## Example
```java
// BEFORE
if (!cfs.getDataTracker().markCompacting(task))  // task.sstables not updated
    continue;
task.execute(...);  // may use sstables already taken by another thread

// AFTER
if (!task.markSSTablesForCompaction())  // updates task.sstables to reserved subset
    continue;
task.execute(...);
```
