# Bug 2342: Snapshot of secondary index uses outer CFS fields instead of inner index CFS fields

**Commit**: 783e5d4c5f · **Ticket**: CASSANDRA-4230

## Detection Rule

When iterating over secondary index stores inside a `ColumnFamilyStore` method and operating on each index's resources, verify that all field accesses use the loop variable (`cfs`) rather than the outer `this` (implicitly or via an unqualified `compactionStrategy` or `directories` reference).

## Code Smell

Inside an iteration over secondary indexes (`for (ColumnFamilyStore cfs : indexManager.getAllIndexColumnFamilyStores())`), references to `compactionStrategy` and `directories` without the `cfs.` qualifier — these resolve to the outer CFS, not the index store.

## Tags

- scope: java
- category: wrong-filter-result
- code-pattern: outer-this-reference-in-inner-loop
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause

In `ColumnFamilyStore.snapshotWithoutFlush()`, when taking a snapshot of secondary indexes, the code checked `compactionStrategy instanceof LeveledCompactionStrategy` and called `directories.snapshotLeveledManifest(snapshotName)`. Both `compactionStrategy` and `directories` resolved to the outer (parent) CFS's fields — they were missing the `cfs.` qualifier. So the leveled manifest of the parent table was saved, not the index's manifest; and if the parent was not using LCS but the index was (or vice versa), the snapshot would be inconsistent.

## Example

```java
// BEFORE (inside loop over index CFS stores)
if (compactionStrategy instanceof LeveledCompactionStrategy)      // outer this
    directories.snapshotLeveledManifest(snapshotName);            // outer this
```

```java
// AFTER
if (cfs.compactionStrategy instanceof LeveledCompactionStrategy)  // index CFS
    cfs.directories.snapshotLeveledManifest(snapshotName);        // index CFS
```
