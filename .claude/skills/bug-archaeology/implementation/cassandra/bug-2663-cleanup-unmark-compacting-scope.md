# Bug 2663: Cleanup prematurely unmarks SSTables as compacting due to finally block at wrong scope

**Commit**: 27f189c6d2 · **Ticket**: CASSANDRA-2769

## Detection Rule
When an SSTable cleanup operation marks files as compacting and wraps the scan/write in a try-finally, place the `unmarkCompacting()` call in the outer finally block that encompasses the entire cleanup, not in an inner finally that runs before post-compaction processing completes.

## Code Smell
`CompactionManager.doCleanupCompaction()` had two nested try-finally blocks: the inner one called `executor.finishCompaction(ci)` and `scanner.close()`, while the outer one called `cfs.getDataTracker().unmarkCompacting(...)`. The inner finally ran before the outer finally, which was correct; but the code had the actual compaction work in the inner `try` and the unmark in the outer `finally`, which unmaked before a replacement SSTable was registered — another thread could then re-compact the same source file.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: unmark-compacting-wrong-scope
- severity: data-loss
- subsystem: compaction

## Root Cause
`doCleanupCompaction()` had a structure where the outer `finally` block called `cfs.getDataTracker().unmarkCompacting(Arrays.asList(sstable))` while the inner `finally` (which should run first) handled scanner and executor cleanup. Due to how the blocks were nested, `unmarkCompacting` could fire before the output SSTable from cleanup was registered, leaving a window where a concurrent compaction thread could pick up the same source SSTable again. The fix flattens the try-finally structure so scanner/executor cleanup happens in the single finally block, with `unmarkCompacting` removed (it is now handled by the caller via `task.unmarkSSTables()`).

## Example
```java
// BEFORE
try {
    try { /* scan + write */ }
    finally { scanner.close(); executor.finishCompaction(ci); }
} finally {
    cfs.getDataTracker().unmarkCompacting(...);  // too late / wrong scope
}

// AFTER
try { /* scan + write */ }
finally {
    scanner.close();
    executor.finishCompaction(ci);
    // unmarkCompacting handled by caller
}
```
