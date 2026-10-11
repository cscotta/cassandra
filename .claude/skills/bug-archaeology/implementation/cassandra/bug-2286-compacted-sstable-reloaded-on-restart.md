# Bug 2286: Compacted-but-not-deleted SSTables Used After Restart, Causing Counter Overcount

**Commit**: 6a6b7ec1f0 · **Ticket**: CASSANDRA-4436

## Detection Rule

When loading SSTables at startup, verify that any SSTable whose generation number appears in another SSTable's ancestor list is excluded, not just those marked with a `COMPACTED_MARKER` file (which may be missing after a crash).

## Code Smell

Startup SSTable loading relies solely on `skipCompacted(true)` (marker-file presence) to filter out superseded SSTables; it does not cross-reference the `ancestors` metadata written into every output SSTable, leaving a gap when the marker file is absent.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: stale-sstable-loaded-on-startup
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

When compaction completes, output SSTables record their input generations in SSTable metadata (`ancestors`). However, after a crash the `COMPACTED_MARKER` file for the consumed inputs might not be present on disk, causing startup to re-load both the pre-compaction and post-compaction SSTables. For counters, reading the same mutation twice causes overcounting. The fix cross-references the `ancestors` list: any SSTable whose generation appears in another SSTable's ancestor set is filtered out and released (effectively deleted) at startup.

## Example

```java
// BEFORE
Directories.SSTableLister sstables = directories.sstableLister()
    .skipCompacted(true).skipTemporary(true);  // relies on marker file only
data.addInitialSSTables(SSTableReader.batchOpen(...));
```

```java
// AFTER
Collection<SSTableReader> sstables = SSTableReader.batchOpen(...);
Set<Integer> compactedGenerations = new HashSet<>();
for (SSTableReader s : sstables)
    compactedGenerations.addAll(s.getAncestors());

Set<SSTableReader> live = new HashSet<>();
for (SSTableReader s : sstables) {
    if (compactedGenerations.contains(s.descriptor.generation))
        s.releaseReference(); // triggers deletion
    else
        live.add(s);
}
data.addInitialSSTables(live);
```
