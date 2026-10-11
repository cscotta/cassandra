# Bug 2398: LeveledManifest.levelOf has O(n) scan over all levels, making startup quadratic

**Commit**: ef01ca5bcf · **Ticket**: CASSANDRA-3952

## Detection Rule

When a method called frequently in a hot path (e.g. during compaction or startup loading) iterates over a collection to find a value that could be stored in a lookup map, add a reverse map to make lookups O(1).

## Code Smell

`levelOf(SSTableReader)` iterates every generation list with `generations[level].contains(sstable)` — linear scan per call. Any code that calls `levelOf` in a loop over all SSTables is therefore O(n²).

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: linear-scan-instead-of-map-lookup
- severity: performance
- subsystem: compaction

## Root Cause

`LeveledManifest` stored SSTable-to-level membership only in per-level `List<SSTableReader>` arrays. The `levelOf()` method had to scan every list to find which level an SSTable belonged to, making it O(total SSTables). During startup, `levelOf` is called for each SSTable being added, giving O(n²) total work. The fix adds a `Map<SSTableReader, Integer> sstableGenerations` that is kept in sync with the lists in `add()` and `remove()`, making `levelOf` O(1).

## Example

```java
// BEFORE — O(n) per call
private int levelOf(SSTableReader sstable) {
    for (int level = 0; level < generations.length; level++)
        if (generations[level].contains(sstable))
            return level;
    return -1;
}
```

```java
// AFTER — O(1) per call
private int levelOf(SSTableReader sstable) {
    Integer level = sstableGenerations.get(sstable);
    return level == null ? -1 : level.intValue();
}
private void add(SSTableReader sstable, int level) {
    generations[level].add(sstable);
    sstableGenerations.put(sstable, level);
}
private void remove(SSTableReader reader) {
    generations[levelOf(reader)].remove(reader);
    sstableGenerations.remove(reader);
}
```
