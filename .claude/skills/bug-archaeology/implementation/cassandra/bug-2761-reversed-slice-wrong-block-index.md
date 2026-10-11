# Bug 2761: Reversed Slice Queries Return Wrong Columns on Large Rows

**Commit**: d416cccfd3 · **Ticket**: CASSANDRA-2212

## Detection Rule
When binary-searching an SSTable block index for a reversed slice, compare the query start against `firstName` of each index slot (not `lastName`) and subtract an extra 1 from the insertion point to start at the block just before the match.

## Code Smell
`IndexHelper.getComparator` always compares against `o1.lastName`, and `indexFor` with `reversed=true` computes `-1 * (index + 1)` from the binary search result, causing the starting block to be one too far right for reversed scans.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-binary-search-comparator
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
For a forward slice, binary search compares the start column against the `lastName` of each index slot to find the first slot that may contain the start. For a reversed slice, the search must instead compare against `firstName` and subtract an additional 1 from the binary search insertion point (to land in the slot just before the first slot whose `firstName` exceeds the start). Using `lastName` and the same offset formula produced an off-by-one that skipped the first matching block in reversed queries on large, multi-block rows.

## Example
```java
// BEFORE (always uses lastName comparator regardless of direction)
int index = Collections.binarySearch(indexList, target, getComparator(comparator));
return index < 0 ? -1 * (index + 1) : index;
```
```java
// AFTER (uses firstName for reversed, different offset formula)
int index = Collections.binarySearch(indexList, target,
    getComparator(comparator, reversed));
return index < 0 ? -index - (reversed ? 2 : 1) : index;
```
