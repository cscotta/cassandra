# Bug 2685: Bloom filter true positives counted only when key cache present

**Commit**: 61adae4fdc · **Ticket**: CASSANDRA-2637

## Detection Rule
When tracking bloom filter true positives, call `addTruePositive()` unconditionally whenever an exact key match is found, not only inside a block guarded by a key-cache capacity check.

## Code Smell
`bloomFilterTracker.addTruePositive()` nested inside `if (comparison == 0 && keyCache != null && keyCache.getCapacity() > 0)`, making the metric conditional on key-cache configuration.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: metric-inside-wrong-branch
- severity: performance
- subsystem: storage-engine

## Root Cause
The true-positive increment was placed inside the block that also stored the key in the cache, gated on `keyCache != null && keyCache.getCapacity() > 0`. When the key cache was disabled or zero-capacity, true positives were never counted, producing incorrect bloom filter false-positive rate statistics. The fix moves the `addTruePositive()` call outside the cache block so it executes for every confirmed match regardless of cache state.

## Example
```java
// BEFORE
if (comparison == 0 && keyCache != null && keyCache.getCapacity() > 0)
{
    if (op == Operator.EQ)
        bloomFilterTracker.addTruePositive();
    if (decoratedKey.key != null)
        cacheKey(decoratedKey, dataPosition);
}
return dataPosition;
```
```java
// AFTER
if (comparison == 0 && keyCache != null && keyCache.getCapacity() > 0)
{
    if (decoratedKey.key != null)
        cacheKey(decoratedKey, dataPosition);
}
if (op == Operator.EQ)
    bloomFilterTracker.addTruePositive();
return dataPosition;
```
