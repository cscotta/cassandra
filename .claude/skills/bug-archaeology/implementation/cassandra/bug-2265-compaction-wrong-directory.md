# Bug 2265: Compaction Writes SSTables to Wrong Directory

**Commit**: 4e6167da57 · **Ticket**: n/a

## Detection Rule

When a raw `File` or directory object is passed to a method that expects a resolved file-system path, verify that the object has been translated through the appropriate path-resolution API before use.

## Code Smell

A `dataDirectory` variable of type `Directories.DataDirectory` (a logical object) is passed directly to `createCompactionWriter` which expects an actual filesystem path string; the method call omits the `getLocationForDisk()` translation step.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: unresolved-directory-object
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

`CompactionTask` obtained a `dataDirectory` (a `Directories.DataDirectory` logical handle) and passed it directly to `cfs.createCompactionWriter`, which expected a resolved filesystem path via `cfs.directories.getLocationForDisk(dataDirectory)`. Without the translation, compaction writers targeted the wrong on-disk location. The fix wraps each `dataDirectory` argument with `cfs.directories.getLocationForDisk(dataDirectory)` at both writer creation sites.

## Example

```java
// BEFORE
SSTableWriter writer = cfs.createCompactionWriter(
    keysPerSSTable, dataDirectory, toCompact);
// ...
writer = cfs.createCompactionWriter(
    keysPerSSTable, dataDirectory, toCompact);
```

```java
// AFTER
SSTableWriter writer = cfs.createCompactionWriter(
    keysPerSSTable, cfs.directories.getLocationForDisk(dataDirectory), toCompact);
// ...
writer = cfs.createCompactionWriter(
    keysPerSSTable, cfs.directories.getLocationForDisk(dataDirectory), toCompact);
```
