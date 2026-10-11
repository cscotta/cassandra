# Bug 2321: LCS promotes tiny L0 SSTable to L1 even when it should stay in L0

**Commit**: a4fab90af0 · **Ticket**: CASSANDRA-4341

## Detection Rule

When a leveled compaction strategy decides which level to promote a compacted SSTable to, check whether the result size is strictly smaller than the max SSTable size before promoting, not just equal to it (off-by-one with `<` vs `<=`).

## Code Smell

A condition `SSTable.getTotalBytes(removed) < maxSSTableSizeInBytes` that uses strict-less-than, causing an SSTable that is exactly `maxSSTableSizeInBytes` bytes to be incorrectly promoted rather than kept at L0.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: boundary-comparison-exclusive-vs-inclusive
- severity: performance
- subsystem: compaction

## Root Cause

LCS's `promote()` method had a special case for tiny L0 SSTables: if both input and output were at L0 and the total bytes were small, keep the result at L0 instead of promoting. The condition `< maxSSTableSizeInBytes` excluded the exact-boundary case, promoting an SSTable that was exactly at the threshold. The fix changes `<` to `<=`. The same commit also fixed the `maxSSTableSizeInMB`-vs-`maxSSTableSizeInBytes` naming confusion that led to a unit mismatch in `estimated[i]` calculations.

## Example

```java
// BEFORE: strict less-than misses the exact boundary
if (minimumLevel == 0 && maximumLevel == 0 && SSTable.getTotalBytes(removed) < maxSSTableSizeInBytes)
    newLevel = 0;  // stay in L0
```

```java
// AFTER: inclusive — at exactly the threshold, also stay in L0
if (minimumLevel == 0 && maximumLevel == 0 && SSTable.getTotalBytes(removed) <= maxSSTableSizeInBytes)
    newLevel = 0;
```
