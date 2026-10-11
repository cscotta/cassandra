# Bug 2645: describeOwnership for OrderPreservingPartitioner uses wrong split size 1 instead of index interval

**Commit**: 0b0d7eadfd · **Ticket**: CASSANDRA-2800

## Detection Rule
When calling `getSplits()` to count keys per token range for ownership calculation, pass `DatabaseDescriptor.getIndexInterval()` as the split size rather than the literal `1`, which produces a split per key and wildly inflates ownership estimates.

## Code Smell
`OrderPreservingPartitioner.describeOwnership()` calls `StorageService.instance.getSplits(ks, cfName, r, 1)`, using a hardcoded split size of `1`; `getSplits` with size 1 returns one split per index entry, causing every range to appear to have the same number of keys and yielding completely incorrect ownership percentages.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: hardcoded-split-size-one
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
In `OrderPreservingPartitioner.describeOwnership()`, the call `getSplits(ks, cfmd.cfName, r, 1)` passed a split size of `1`, which caused `getSplits` to create one split per sampled key. The result was that every token range reported the same count (number of samples), making the ownership calculation return uniform values regardless of actual data distribution. The correct argument is `DatabaseDescriptor.getIndexInterval()`, which is the sampling granularity used elsewhere.

## Example
```java
// BEFORE
allTokens.put(r.right, allTokens.get(r.right)
    + StorageService.instance.getSplits(ks, cfmd.cfName, r, 1).size());

// AFTER
allTokens.put(r.right, allTokens.get(r.right)
    + StorageService.instance.getSplits(ks, cfmd.cfName, r,
        DatabaseDescriptor.getIndexInterval()).size());
```
