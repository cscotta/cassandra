# Bug 0410: SetStatistics commit fails permanently on concurrent write with no retry

**Commit**: bfc52d218adf9fc0229601055c2238f580bcac3a · **Ticket**: #15148

## Detection Rule

When a metadata-update operation calls `ops.commit(base, newMetadata)` directly, verify that it uses the same retry-on-`CommitFailedException` pattern as all other `UpdateX` operations, otherwise it will fail permanently on any concurrent write.

## Code Smell

A `commit()` method calls `ops.commit(base, newMetadata)` in a single un-retried attempt using a `base` snapshot captured before the call; if another writer commits concurrently the `CommitFailedException` surfaces directly to the caller.

## Tags

- scope: project-specific
- category: race-condition
- code-pattern: non-retried-commit
- severity: crash
- subsystem: core/statistics

## Root Cause

`SetStatistics.commit()` captured the current table metadata in `base` and issued a single `ops.commit(base, newMetadata)` with no retry loop. Every other `UpdateX` implementation wraps its commit in a `Tasks.foreach(...).retry(...).onlyRetryOn(CommitFailedException.class)` loop that refreshes `base` before each attempt. Because statistics writes are expected to race with normal data commits, the missing retry caused `compute_table_stats` calls to fail whenever another transaction landed between the stats computation and the commit.

## Example

```java
// BEFORE
public void commit() {
    TableMetadata base = ops.current();
    TableMetadata newMetadata = internalApply(base);
    ops.commit(base, newMetadata);  // throws CommitFailedException on any concurrent write
}
```

```java
// AFTER
public void commit() {
    Tasks.foreach(ops)
        .retry(ops.current().propertyAsInt(COMMIT_NUM_RETRIES, COMMIT_NUM_RETRIES_DEFAULT))
        .exponentialBackoff(...)
        .onlyRetryOn(CommitFailedException.class)
        .run(taskOps -> {
            TableMetadata base = taskOps.refresh();
            TableMetadata updated = internalApply(base);
            taskOps.commit(base, updated);
        });
}
```
