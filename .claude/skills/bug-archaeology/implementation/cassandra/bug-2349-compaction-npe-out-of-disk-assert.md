# Bug 2349: Compaction crashes with NPE when out-of-disk-space assertion is disabled

**Commit**: abb39c2bc4 · **Ticket**: CASSANDRA-3985

## Detection Rule

When a method returns `null` to signal an error condition and the caller unconditionally asserts the result is non-null, that assert becomes a latent NPE crash whenever assertions are disabled in production.

## Code Smell

`compactionFileLocation` is populated by a call that can return `null` (no disk space), followed by `assert compactionFileLocation != null` — this pattern silently crashes (NPE) when Java assertions are disabled (`-ea` not set), instead of failing gracefully.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: assert-instead-of-null-check
- severity: crash
- subsystem: compaction

## Root Cause

`CompactionTask.execute()` called `getDataFileLocation()` which returns `null` when no disk with sufficient free space is found. The subsequent `assert compactionFileLocation != null` only fires when assertions are enabled. In production (assertions disabled), the null propagated to `SSTableWriter` construction, producing a confusing NullPointerException rather than a clean "insufficient space" log message and early return. The fix replaces the assert with an explicit null check and graceful abort.

## Example

```java
// BEFORE
compactionFileLocation = cfs.table.getDataFileLocation(
        cfs.getExpectedCompactedFileSize(toCompact), ensureFreeSpace());
// ...
assert compactionFileLocation != null;  // NPE in prod when assertions disabled
```

```java
// AFTER
compactionFileLocation = cfs.table.getDataFileLocation(
        cfs.getExpectedCompactedFileSize(toCompact));
// ...
if (compactionFileLocation == null) {
    logger.warn("insufficient space to compact even the two smallest files, aborting");
    return 0;
}
```
