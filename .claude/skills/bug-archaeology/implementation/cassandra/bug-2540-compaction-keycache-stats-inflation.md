# Bug 2540: Compaction key-cache warming inflates key-cache hit statistics

**Commit**: 305a06ec0e · **Ticket**: CASSANDRA-3325

## Detection Rule
When checking whether a key is already in the key cache during compaction (to decide whether to pre-warm the new SSTable), use a non-stats-updating cache lookup; using the normal `get()` records a hit and inflates the hit-rate metric.

## Code Smell
`sstable.getCachedPosition(row.key)` called during compaction's cache-warming loop — this is a `keyCache.get()` call that counts as a cache hit in JMX statistics, so every compaction run artificially boosts the reported key-cache hit ratio.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: stats-updating-lookup-in-background-task
- severity: performance
- subsystem: compaction

## Root Cause
`CompactionTask` iterated the compacted rows and, for each key already in the key cache, recorded its new position in the replacement SSTable. The existing `getCachedPosition()` method called `keyCache.get()` which increments the hit counter. Because compaction processes every key in the SSTables (potentially millions), this could make the cache appear to have a near-100% hit rate. The fix adds a `boolean updateStats` parameter and uses `keyCache.getInternal()` (no stat update) during compaction's cache-warming path.

## Example
```java
// BEFORE — records hit during compaction warm-up
if (sstable.getCachedPosition(row.key) != null) {
    cachedKeys.put(row.key, position);
}
```
```java
// AFTER — does not update hit stats
if (sstable.getCachedPosition(row.key, false) != null) {
    cachedKeys.put(row.key, position);
}
```
