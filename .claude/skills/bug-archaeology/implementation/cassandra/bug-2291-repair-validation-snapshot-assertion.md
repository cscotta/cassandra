# Bug 2291: Repair Validation Calls getOverlappingSSTables on Snapshot SSTables, Triggering Assertion

**Commit**: aba1f16be8 · **Ticket**: CASSANDRA-4456

## Detection Rule

When constructing a `CompactionController` for repair or validation, verify it does not call `cfs.getOverlappingSSTables()` on the provided SSTable set, since those SSTables may be snapshot copies not present in the live tracker.

## Code Smell

`new CompactionController(cfs, sstables, gcBefore, true)` is called in a validation context where `sstables` comes from a snapshot; `CompactionController`'s constructor calls `cfs.getOverlappingSSTables(sstables)`, which asserts that all provided SSTables are live.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: snapshot-sstables-passed-to-live-query
- severity: crash
- subsystem: repair

## Root Cause

Repair runs validation compaction on a snapshot, so the SSTable set passed to the compaction controller is not guaranteed to be in the live SSTable tracker. `CompactionController` called `cfs.getOverlappingSSTables(sstables)` internally, which asserts that every input SSTable is registered as live — an assertion that fails for snapshot SSTables. The fix introduces `ValidationCompactionController`, a subclass that bypasses the overlapping-SSTable lookup and unconditionally returns `false` from `shouldPurge`.

## Example

```java
// BEFORE — controller calls getOverlappingSSTables, asserts sstables are live
new CompactionController(cfs, sstables, getDefaultGcBefore(cfs), true)
// assertion fires for snapshot sstables
```

```java
// AFTER — dedicated subclass that skips the live-sstable lookup
private static class ValidationCompactionController extends CompactionController {
    public ValidationCompactionController(ColumnFamilyStore cfs, Collection<SSTableReader> sstables) {
        super(cfs, Integer.MAX_VALUE, true, null,
              cfs.getCompactionStrategy().isKeyExistenceExpensive(ImmutableSet.copyOf(sstables)));
    }
    @Override
    public boolean shouldPurge(DecoratedKey key) { return false; }
}
```
