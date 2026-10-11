# Bug 2672: Truncate/compaction race corrupts data

**Commit**: cd60613ae0 · **Ticket**: CASSANDRA-2673

## Detection Rule
When recording a truncation timestamp that must be strictly after any flushed SSTable's creation time, ensure the timestamp is captured between the flush completion and the snapshot, not before the flush—and submit the actual deletion work through the compaction executor so it serializes behind in-progress compactions.

## Code Smell
`truncatedAt = System.currentTimeMillis()` followed by `snapshot()` followed by `flush()`, where the timestamp precedes the flush and the cleanup runs on the postFlush executor instead of the compaction executor.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: timestamp-before-flush-completion
- severity: data-loss
- subsystem: compaction

## Root Cause
The old truncation sequence captured `truncatedAt` before flushing, meaning any SSTable created during the flush would have a timestamp equal to or greater than `truncatedAt` and would be incorrectly excluded from truncation. Additionally, running cleanup on the `postFlushExecutor` did not serialize behind compactions that were already in progress, so a compaction that started before truncate but finished after could re-create deleted data. The fix: flush first, sleep 100 ms to guarantee a strictly-later millisecond timestamp, then snapshot, then submit cleanup through the compaction executor.

## Example
```java
// BEFORE
final long truncatedAt = System.currentTimeMillis();
snapshot(...);
// cleanup submitted to postFlushExecutor
return postFlushExecutor.submit(runnable);
```
```java
// AFTER
forceBlockingFlush();
Thread.sleep(100); // ensure truncatedAt > any flushed SSTable
long truncatedAt = System.currentTimeMillis();
snapshot(...);
return CompactionManager.instance.submitTruncate(this, truncatedAt);
```
