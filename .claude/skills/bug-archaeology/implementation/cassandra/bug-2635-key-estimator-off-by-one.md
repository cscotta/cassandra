# Bug 2635: SSTable key estimator returns 0 for non-empty ranges causing streaming failures

**Commit**: 466ff41a7d · **Ticket**: CASSANDRA-2916

## Detection Rule
When computing the number of keys in an SSTable range by index sampling, ensure the range boundary comparison is `left > right` (exclusive empty) rather than `left >= right` (which excludes single-sample ranges), and clamp the result to at least 1 for any non-empty range.

## Code Smell
`getSampleIndexesForRanges()` discards a sample range when `left >= right`, which incorrectly drops a range containing exactly one sample index; downstream, `estimatedKeysForRanges()` can then return 0, causing streaming to think a non-empty range has no data.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: inclusive-range-boundary
- severity: crash
- subsystem: streaming

## Root Cause
In `SSTableReader.getSampleIndexesForRanges()`, the condition `if (left >= right)` treated a range where `left == right` (a single-index range) as empty and skipped it. This caused `estimatedKeysForRanges()` to return 0 for ranges that overlapped exactly one sample point. Streaming code used this estimate to decide how to transfer data; a 0-key estimate for a non-empty range could cause the stream to skip or mishandle the transfer. Two fixes were applied: use `left > right` for the empty check, and clamp the final estimate with `Math.max(1, ...)`.

## Example
```java
// BEFORE
if (left >= right)
    continue;  // drops single-sample ranges
return sampleKeyCount * DatabaseDescriptor.getIndexInterval();  // can return 0

// AFTER
if (left > right)
    continue;
return Math.max(1, sampleKeyCount * DatabaseDescriptor.getIndexInterval());
```
