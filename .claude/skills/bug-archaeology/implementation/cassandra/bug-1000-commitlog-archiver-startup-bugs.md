# Bug 1000: Commit Log Archiver Startup Bugs

**Commit**: 3740f815c2 · **Ticket**: CASSANDRA-10593
**Subsystem**: Commit log / archiving
**Category**: Logic error in condition

---

## Root Cause
Two independent bugs in `CommitLogArchiver` and `CommitLogSegmentManager` affected startup behavior.
First, the archiver caught `ExecutionException` and checked whether its cause was an `IOException`, but archiving tasks wrap exceptions in a `RuntimeException` before wrapping in `ExecutionException`, so the cause chain has an extra level.
Second, `recycleSegment` did not check the `commitlog_segment_recycling_enabled` flag, so segments were recycled even when recycling was explicitly disabled.

### Before / After
```java
// BEFORE — in CommitLogArchiver.waitForArchiving():
if (e.getCause() instanceof IOException)
{
    logger.error("Looks like the archiving of file {} failed earlier...", name);
    return false;
}

// AFTER — unwraps the extra RuntimeException layer:
if (e.getCause() instanceof RuntimeException)
{
    if (e.getCause().getCause() instanceof IOException)
    {
        logger.error("Looks like the archiving of file {} failed earlier...", name, e.getCause().getCause());
        return false;
    }
}

// BEFORE — in CommitLogSegmentManager.recycleSegment():
if (isCapExceeded()
    || CommitLogDescriptor.fromFileName(file.getName()).getMessagingVersion() != MessagingService.current_version)

// AFTER — also respects the recycling flag:
if (isCapExceeded()
    || CommitLogDescriptor.fromFileName(file.getName()).getMessagingVersion() != MessagingService.current_version
    || !DatabaseDescriptor.getCommitLogSegmentRecyclingEnabled())
```

---

## How to Spot
When an archiving task throws, the executor wraps the original `IOException` inside a `RuntimeException` before placing it in the `ExecutionException`. Any `instanceof` check against `e.getCause()` must mirror the full wrapping chain — checking only one level misses the real cause. Similarly, whenever a feature flag guards a behaviour, every code path that performs that behaviour must query the flag; scanning all callers of `recycleSegment` during review would reveal the missing guard.

---

## Test
No test added.
