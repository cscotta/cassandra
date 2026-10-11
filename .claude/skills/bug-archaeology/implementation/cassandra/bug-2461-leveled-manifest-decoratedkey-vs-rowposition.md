# Bug 2461: LeveledCompactionStrategy crashes due to DecoratedKey used where RowPosition required

**Commit**: e75c53ea14 · **Ticket**: CASSANDRA-3691

## Detection Rule

When SSTable generation pre-allocation changes the minimum token representation from a concrete key type to a more abstract position type, verify that all arrays and fields storing that type are updated accordingly.

## Code Smell

`lastCompactedKeys` declared as `DecoratedKey[]` and initialized with `new DecoratedKey(cfs.partitioner.getMinimumToken(), null)`; after a change that requires `RowPosition`, the array type and initializer are out of date and cause a `ClassCastException` during compaction candidate selection.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: wrong-type-after-refactor
- severity: crash
- subsystem: compaction

## Root Cause

`LeveledManifest` stored the last-compacted key per level as `DecoratedKey[]` and initialized it with `new DecoratedKey(minimumToken, null)`. After the pre-allocation change, the compaction logic compared these against `RowPosition` objects. `DecoratedKey` does not satisfy the expected `RowPosition` interface in all comparison paths, causing a `ClassCastException`. The fix changes the field type to `RowPosition[]` and the initialization to `cfs.partitioner.getMinimumToken().minKeyBound()`.

## Example

```java
// BEFORE
private final DecoratedKey[] lastCompactedKeys;
...
lastCompactedKeys[i] = new DecoratedKey(cfs.partitioner.getMinimumToken(), null);

// AFTER
private final RowPosition[] lastCompactedKeys;
...
lastCompactedKeys[i] = cfs.partitioner.getMinimumToken().minKeyBound();
```
