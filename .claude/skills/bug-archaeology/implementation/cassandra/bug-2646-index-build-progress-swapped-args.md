# Bug 2646: Index build reports inverted progress — bytes read and total bytes are swapped in CompactionInfo

**Commit**: 850d1a4c90 · **Ticket**: CASSANDRA-2853

## Detection Rule
When constructing a `CompactionInfo` for progress reporting, verify the argument order matches the constructor signature `(keyspace, cf, type, bytesRead, totalBytes)`; swapping these two arguments causes progress bars and percentage displays to show inverted or nonsensical values.

## Code Smell
`Table.IndexBuilder.getProgress()` constructs `new CompactionInfo(..., iter.getTotalBytes(), iter.getBytesRead())`, placing total before read; the constructor expects `(bytesRead, totalBytes)`, so progress is reported as the inverse (e.g. 90% done appears as 10% done).

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: swapped-constructor-arguments
- severity: cosmetic
- subsystem: compaction

## Root Cause
The `CompactionInfo` constructor takes `(bytesRead, totalBytes)` in that order, but `IndexBuilder.getProgress()` passed the arguments in the wrong order: `(iter.getTotalBytes(), iter.getBytesRead())`. This caused the index build progress displayed via `nodetool compactionstats` to show the completion fraction inverted — at the start it showed near 100% and decremented toward 0% as the build progressed.

## Example
```java
// BEFORE
return new CompactionInfo(cfs.table.name, cfs.columnFamily,
    CompactionType.INDEX_BUILD,
    iter.getTotalBytes(),   // ← wrong: this is total, not read
    iter.getBytesRead());

// AFTER
return new CompactionInfo(cfs.table.name, cfs.columnFamily,
    CompactionType.INDEX_BUILD,
    iter.getBytesRead(),    // ← correct order
    iter.getTotalBytes());
```
