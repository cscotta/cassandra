# Bug 2891: EstimatedHistogram.max returns the wrong bucket offset

**Commit**: 5ac1aaba53 · **Ticket**: CASSANDRA-1413

## Detection Rule
When a histogram's `max()` method searches for the highest non-empty bucket and returns an offset, verify whether the offset array is left-biased (entry i represents the lower bound of bucket i) or right-biased, and adjust the return index accordingly.

## Code Smell
`EstimatedHistogram.max` iterates from the highest bucket downward and returns `bucketOffsets[i == 0 ? 0 : i - 1]`, which is the lower bound of the bucket below the one that has data — off by one bucket in the wrong direction.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: histogram-offset-wrong-index
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`bucketOffsets[i]` is the upper bound for bucket `i`. The original code returned `bucketOffsets[i-1]` (the upper bound of the previous bucket, i.e. the lower bound of the current bucket) instead of `bucketOffsets[i]` (the upper bound of the bucket that actually has data). Additionally, if the overflow bucket (`lastBucket`) was non-empty the method silently returned a misleadingly small value; the fix throws `IllegalStateException` in that case.

## Example
```java
// BEFORE — returns lower bound of occupied bucket (off by one)
for (int i = numBuckets - 1; i >= 0; i--)
    if (buckets.get(i) > 0)
        return bucketOffsets[i == 0 ? 0 : i - 1];
```
```java
// AFTER — returns upper bound (correct ceiling) of occupied bucket
int lastBucket = numBuckets - 1;
if (buckets.get(lastBucket) > 0)
    throw new IllegalStateException("Unable to compute ceiling for max when all buckets are full");
for (int i = lastBucket - 1; i >= 0; i--)
    if (buckets.get(i) > 0)
        return bucketOffsets[i];
```
