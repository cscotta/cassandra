# Bug 3087: LeveledCompactionStrategy allows multiple simultaneous compaction tasks

**Commit**: 9361543f80 · **Ticket**: CASSANDRA-3087

## Detection Rule
When a compaction strategy's `getBackgroundTasks()` method creates a new task object on every call without checking whether the previous task is still running, multiple simultaneous compaction tasks can be scheduled.

## Code Smell
A `synchronized` method (or no locking at all) on `getBackgroundTasks()` that unconditionally calls `new LeveledCompactionTask(...)` and returns it will issue overlapping tasks to the thread pool.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: missing-in-progress-guard
- severity: crash
- subsystem: compaction

## Root Cause
`LeveledCompactionStrategy.getBackgroundTasks()` was `synchronized` but that only prevented concurrent entry into the method — it did nothing to prevent the compaction executor from scheduling a second task after the first had already been returned and enqueued. The fix stores the current task in an `AtomicReference`, skips scheduling if the current task is not yet done, and uses a CAS to atomically replace it, ensuring only one leveled compaction runs at a time.

## Example
```java
// BEFORE
public synchronized List<AbstractCompactionTask> getBackgroundTasks(int gcBefore) {
    Collection<SSTableReader> sstables = manifest.getCompactionCandidates();
    if (sstables.isEmpty()) return Collections.emptyList();
    LeveledCompactionTask task = new LeveledCompactionTask(cfs, sstables, gcBefore, maxSSTableSize);
    return Collections.<AbstractCompactionTask>singletonList(task);
}
```
```java
// AFTER
public List<AbstractCompactionTask> getBackgroundTasks(int gcBefore) {
    LeveledCompactionTask currentTask = task.get();
    if (currentTask != null && !currentTask.isDone())
        return Collections.emptyList();
    Collection<SSTableReader> sstables = manifest.getCompactionCandidates();
    if (sstables.isEmpty()) return Collections.emptyList();
    LeveledCompactionTask newTask = new LeveledCompactionTask(cfs, sstables, gcBefore, maxSSTableSize);
    return task.compareAndSet(currentTask, newTask)
           ? Collections.<AbstractCompactionTask>singletonList(newTask)
           : Collections.<AbstractCompactionTask>emptyList();
}
```
