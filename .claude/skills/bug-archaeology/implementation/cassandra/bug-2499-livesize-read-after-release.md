# Bug 2499: liveSize stat goes negative — SSTable dereferenced before size is subtracted

**Commit**: 4806ef91f3 · **Ticket**: CASSANDRA-3496

## Detection Rule

When a metric counter is decremented using a value fetched from an object that is released (potentially GC'd or zeroed) before the decrement occurs, the metric will be updated with a stale or zero value instead of the correct size.

## Code Smell

`sstable.markCompacted(); sstable.releaseReference(); liveSize.addAndGet(-sstable.bytesOnDisk())` — the size is read after the reference is released, which may return 0 or an incorrect value if the underlying file is already removed.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: read-after-release
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

In `DataTracker.removeOldSSTablesSize()`, `sstable.bytesOnDisk()` was called after `sstable.releaseReference()`. If the reference count dropped to zero, the SSTable's backing files could be deleted, and `bytesOnDisk()` would return 0 (or an incorrect value). This caused `liveSize` to not be decremented properly, making the JMX stat inaccurate or negative. The fix moves the `liveSize` decrement to before `markCompacted()` and `releaseReference()`.

## Example

```java
// BEFORE
sstable.markCompacted();
sstable.releaseReference();
liveSize.addAndGet(-sstable.bytesOnDisk());  // read after release — may be 0
```

```java
// AFTER
liveSize.addAndGet(-sstable.bytesOnDisk());  // read before release — correct size
sstable.markCompacted();
sstable.releaseReference();
```
