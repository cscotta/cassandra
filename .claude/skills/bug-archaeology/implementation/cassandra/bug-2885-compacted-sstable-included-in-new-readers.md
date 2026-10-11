# Bug 2885: Compacted SSTables included in newly instantiated SSTableReaders

**Commit**: 1ab976be2a · **Ticket**: CASSANDRA-1477

## Detection Rule
When scanning a directory to discover SSTable files to open, check that files with a compaction-complete marker are excluded before any read attempt, not only after opening.

## Code Smell
`SSTable.tryComponent` returned the filename pair unconditionally; callers would open SSTableReaders for files that had already been replaced by a compaction and were marked with a `COMPACTED_MARKER` file, leading to stale data being read or file-not-found errors as compaction deleted the originals.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-compaction-marker-check
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause
The file discovery path in `SSTable.tryComponent` did not check for the existence of the companion `COMPACTED_MARKER` file before returning a valid `(Descriptor, Component)` pair. A new guard was added: if `descriptor.filenameFor(Component.COMPACTED_MARKER)` exists, `tryComponent` returns null, and callers skip the file.

## Example
```java
// BEFORE — returns compacted files without checking marker
return Component.fromFilename(dir, name);
```
```java
// AFTER — returns null for compacted SSTables
Pair<Descriptor, Component> pair = Component.fromFilename(dir, name);
if (new File(pair.left.filenameFor(Component.COMPACTED_MARKER)).exists())
    return null;
return pair;
```
