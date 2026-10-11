# Bug 2381: LeveledManifest.replace() crashes when cleanup produces zero output SSTables

**Commit**: 3fd0fb6aa0 · **Ticket**: CASSANDRA-4112

## Detection Rule

When a compaction "replace" helper unconditionally asserts exactly one input and one output SSTable, check whether operations like cleanup can legitimately produce zero output files (e.g., when all rows are deleted), which would violate the `assert Iterables.size(added) == 1` assertion.

## Code Smell

`assert Iterables.size(added) == 1` in `LeveledManifest.replace()` throws `AssertionError` whenever a cleanup or scrub operation drops all rows from an SSTable and produces no output file — a valid and expected outcome.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: wrong-assert-cardinality
- severity: crash
- subsystem: compaction

## Root Cause

`LeveledManifest.replace()` was designed for single-SSTable non-merging operations (cleanup, scrub, upgradesstables) and asserted that exactly one SSTable was removed and exactly one was added. However, cleanup can legitimately produce zero output files when all data in the input SSTable has been deleted. The `Iterables.size(added) == 1` assertion then fired, crashing the compaction task. The fix relaxes the postcondition to `added <= 1` and only calls `add()` when `added` is non-empty, preserving the level assignment from the removed SSTable.

## Example

```java
// BEFORE
assert Iterables.size(removed) == 1;
assert Iterables.size(added) == 1;   // crashes when cleanup drops all rows
SSTableReader toAdd = added.iterator().next();
add(toAdd, remove(toRemove));
```

```java
// AFTER
assert Iterables.size(removed) == 1 : Iterables.size(removed);
assert Iterables.size(added) <= 1   : Iterables.size(added);
int level = remove(removed.iterator().next());
if (!Iterables.isEmpty(added))
    add(added.iterator().next(), level);
```
