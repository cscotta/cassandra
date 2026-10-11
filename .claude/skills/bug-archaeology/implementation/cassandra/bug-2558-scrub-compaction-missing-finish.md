# Bug 2558: Scrub compaction never calls finishCompaction, leaking executor slot

**Commit**: 5056f05a32 · **Ticket**: CASSANDRA-3255

## Detection Rule
When `executor.beginCompaction(info)` is called inside a `try` block but `executor.finishCompaction(info)` is only in the happy path and not in the `finally` block, the compaction manager's active-task slot is never released after a scrub.

## Code Smell
A `ScrubInfo` object is created and passed to `executor.beginCompaction()` but the matching `executor.finishCompaction()` call is absent from the `finally` block; it either appears only on the success path or is missing entirely.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: missing-finally-finish
- severity: hang
- subsystem: compaction

## Root Cause
`CompactionManager.scrub()` called `executor.beginCompaction(new ScrubInfo(...))` inside the `try` body but never called `finishCompaction` in the `finally` block. As a result, the compaction manager considered a scrub slot occupied indefinitely, eventually preventing further compactions from being scheduled. The fix creates the `ScrubInfo` before the `try`, calls `beginCompaction` early, and adds `executor.finishCompaction(scrubInfo)` in the `finally` clause.

## Example
```java
// BEFORE
SSTableWriter writer = maybeCreateWriter(...);
executor.beginCompaction(new ScrubInfo(dataFile, sstable));
// ... scrub loop ...
// finally block: close files only, no finishCompaction
```
```java
// AFTER
ScrubInfo scrubInfo = new ScrubInfo(dataFile, sstable);
// ...
executor.beginCompaction(scrubInfo);
// ...
} finally {
    FileUtils.closeQuietly(dataFile);
    FileUtils.closeQuietly(indexFile);
    executor.finishCompaction(scrubInfo);
}
```
