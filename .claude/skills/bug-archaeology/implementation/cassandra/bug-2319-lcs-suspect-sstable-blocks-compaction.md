# Bug 2319: LCS blacklisted (suspect) SSTables cause compaction to loop or stall

**Commit**: 0cc168a966 · **Ticket**: CASSANDRA-4343

## Detection Rule

When a compaction strategy iterates over SSTables to find a compaction candidate and needs to skip blacklisted entries, verify that the iteration wraps around and terminates rather than always returning the same suspect entry or returning an empty set without retrying.

## Code Smell

A linear scan over `generations[level]` that returns the first SSTable found without checking `isMarkedSuspect()`, or code that uses `hasSuspectSSTables(candidates)` as a filter that prematurely returns an empty candidate list when suspects are mixed in.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: suspect-sstable-not-skipped-in-rotation
- severity: hang
- subsystem: compaction

## Root Cause

Two problems combined. First, the previous code used `hasSuspectSSTables()` as a gate and called `filterSuspectSSTables()` on the result, which could return an empty list and trigger spurious "nothing to compact" paths. Second, in non-L0 compaction candidate selection, the code iterated forward from `lastCompactedKeys[level]` and returned the first SSTable's overlaps without checking if any candidate was suspect. The fix introduces a `while(true)` loop with ring-wrap that tries each starting position until it finds one whose overlap set contains no suspect SSTables, breaking out cleanly if none exists.

## Example

```java
// BEFORE: returns candidates that may include suspect SSTables; caller filters afterward
return hasSuspectSSTables(candidates)
        ? filterSuspectSSTables(candidates)  // could return empty set
        : candidates;
// ...
for (SSTableReader sstable : generations[level])
{
    if (sstable.first.compareTo(lastCompactedKeys[level]) > 0)
        return overlapping(sstable, generations[(level + 1)]);  // no suspect check
}
```

```java
// AFTER: loop with ring-wrap to find first non-suspect candidate set
int i = start;
outer:
while (true)
{
    SSTableReader sstable = generations[level].get(i);
    List<SSTableReader> candidates = overlapping(sstable, generations[(level + 1)]);
    for (SSTableReader candidate : candidates)
    {
        if (candidate.isMarkedSuspect())
        {
            i = (i + 1) % generations[level].size();
            if (i == start) break outer;
            continue outer;
        }
    }
    return candidates;
}
return Collections.emptyList();
```
