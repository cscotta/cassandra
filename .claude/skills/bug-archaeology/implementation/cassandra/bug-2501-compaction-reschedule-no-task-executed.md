# Bug 2501: Compaction rescheduled even when all candidate tasks were skipped

**Commit**: 192cc7dc83 · **Ticket**: CASSANDRA-3483 (note: CHANGES.txt says 3484)

## Detection Rule

When a background compaction loop reschedules itself based on whether `tasks.isEmpty()` rather than whether any task actually executed, it will continuously reschedule when tasks exist but all fail the `markSSTablesForCompaction()` lock check, creating a busy-loop.

## Code Smell

`if (!tasks.isEmpty()) submitBackground(cfs)` where `tasks` is the candidate list — not a list of executed tasks. If all tasks fail `markSSTablesForCompaction()`, no compaction occurs but rescheduling still happens.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: reschedule-based-on-candidates-not-executed
- severity: performance
- subsystem: compaction

## Root Cause

`CompactionManager.BackgroundCompactionTask` used `!tasks.isEmpty()` to decide whether to re-queue a background compaction job. However `tasks` was the list of candidate compaction tasks from the strategy — not the list of tasks that actually ran. If every candidate failed `markSSTablesForCompaction()` (e.g., already locked by another thread), no work was done but the task resubmitted itself, potentially spinning. The fix introduces a `taskExecuted` boolean set only when a task's `execute()` is called.

## Example

```java
// BEFORE
// newly created sstables might have made other compactions eligible
if (!tasks.isEmpty())
    submitBackground(cfs);  // reschedules even if nothing ran
```

```java
// AFTER
boolean taskExecuted = false;
for (AbstractCompactionTask task : tasks)
{
    if (!task.markSSTablesForCompaction())
        continue;
    taskExecuted = true;
    task.execute(executor);
}
if (taskExecuted)
    submitBackground(cfs);  // only reschedule if work was done
```
