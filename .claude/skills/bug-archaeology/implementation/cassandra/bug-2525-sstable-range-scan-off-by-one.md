# Bug 2525: Off-by-one in SSTable range scan causes assertion error with ordered partitioners

**Commit**: 118513ad9d · **Ticket**: CASSANDRA-3369

## Detection Rule
When converting a binary-search result to an index boundary, handle the case where the negated insertion-point equals zero separately; decrementing zero produces -1, which is interpreted as "not found" and causes an assertion or incorrect empty-range decision.

## Code Smell
`right = (right + 1) * -1; if (right > 0) right--;` — if binary search returns -1 (not found, insertion point 0), then `right` becomes 0, the condition `right > 0` is false, no decrement happens, and the subsequent `left >= right` comparison (with `left == 0`) produces `0 >= 0` which incorrectly drops the range instead of asserting.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: binary-search-zero-boundary
- severity: crash
- subsystem: storage-engine

## Root Cause
`SSTableReader.getPositionsForRanges()` converts a binary search "not found" result to an index by computing `right = (right + 1) * -1`. When the first key in the index is already strictly greater than the range's right bound, `binarySearch` returns -1, making the converted `right` equal to 0. The code then checked `if (right > 0) right--` — missing the `right == 0` case — and fell through to `if (left >= right)` which incorrectly continued rather than skipping the range. With ordered (non-random) partitioners this triggered an assertion on subsequent range processing.

## Example
```java
// BEFORE
right = (right + 1) * -1;
if (right > 0)
    right--;
// right==0 falls through — wrong
```
```java
// AFTER
right = (right + 1) * -1;
if (right == 0)
    continue;   // first key already beyond right bound, skip range
right--;
```
