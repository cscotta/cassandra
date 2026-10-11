# Bug 2560: CommitLog flush causes deadlock via synchronized executor

**Commit**: a40d00ba39 · **Ticket**: CASSANDRA-3253

## Detection Rule
When a task running on executor A calls a method that acquires a lock already held by a thread blocked waiting for executor A to complete, a deadlock occurs; detect by tracing `forceFlush()` calls made directly from the CommitLog executor thread.

## Code Smell
A `forceFlush()` (or equivalent method that acquires `Table.switchLock`) is called synchronously from within a Runnable submitted to the CommitLog executor, while the caller that submitted to the CommitLog executor may hold `Table.switchLock`.

## Tags
- scope: cassandra
- category: deadlock
- code-pattern: synchronized-executor-reentrant-lock
- severity: hang
- subsystem: commitlog-cdc

## Root Cause
`CommitLog` periodically forced flushes of dirty column families from its executor thread. `forceFlush()` acquires `Table.switchLock`. Meanwhile, a normal write path calls `CommitLog.getContext()` while holding `Table.switchLock`, causing it to block on the CommitLog executor. The CommitLog executor was now blocked waiting for `Table.switchLock` while the lock holder was blocked waiting for the CommitLog executor — a classic circular deadlock. The fix submits the `forceFlush` as a `Runnable` to `StorageService.optionalTasks` so it runs off the CommitLog executor thread.

## Example
```java
// BEFORE (runs on CommitLog executor, acquires Table.switchLock)
Table.open(keyspace).getColumnFamilyStore(dirtyCFId).forceFlush();
```
```java
// AFTER (dispatched to separate executor)
final ColumnFamilyStore cfs = Table.open(keyspace)
    .getColumnFamilyStore(dirtyCFId);
StorageService.optionalTasks.execute(() -> cfs.forceFlush());
```
