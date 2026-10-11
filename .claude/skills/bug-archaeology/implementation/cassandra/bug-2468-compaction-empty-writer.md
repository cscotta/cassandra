# Bug 2468: Empty, uncleaned SSTable writer created at compaction boundary when no data remains

**Commit**: c6e1e5029b · **Ticket**: CASSANDRA-3616

## Detection Rule

When a new writer is created unconditionally at each compaction split boundary, the last iteration creates a writer that is never populated nor properly closed, leaving behind an empty/dirty artifact.

## Code Smell

A new writer is allocated immediately after closing the previous one, without first checking whether there are more rows to write. Pattern: `writer = createWriter(); writers.add(writer);` at the end of a loop body with no guard.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: unconditional-writer-allocation-in-loop
- severity: data-loss
- subsystem: compaction

## Root Cause

Inside `CompactionTask`, each time a size threshold was crossed, the current writer was closed and a new writer was immediately created and added to the `writers` list. On the final iteration, after closing the last real writer, a new empty writer was created that would never have rows written to it. This empty, non-finalized writer would leave dangling SSTable resources.

## Example

```java
// BEFORE
SSTableReader toIndex = writer.closeAndOpenReader(getMaxDataAge(toCompact));
cachedKeyMap.put(toIndex, cachedKeys);
sstables.add(toIndex);
writer = cfs.createCompactionWriter(keysPerSSTable, compactionFileLocation, toCompact);
writers.add(writer);
cachedKeys = new HashMap<DecoratedKey, Long>();
```

```java
// AFTER
SSTableReader toIndex = writer.closeAndOpenReader(getMaxDataAge(toCompact));
cachedKeyMap.put(toIndex, cachedKeys);
sstables.add(toIndex);
if (nni.hasNext())
{
    writer = cfs.createCompactionWriter(keysPerSSTable, compactionFileLocation, toCompact);
    writers.add(writer);
    cachedKeys = new HashMap<DecoratedKey, Long>();
}
```
