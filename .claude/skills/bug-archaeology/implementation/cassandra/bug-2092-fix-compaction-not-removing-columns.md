# Bug 2092: Compaction Skips Column Removal When Bloom Filter Is Disabled (bf_fp_chance=1.0)

**Commit**: 6415d6bee2 · **Ticket**: CASSANDRA-5182
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionController.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CompactionController.needsCleanup` consulted the bloom filter on overlapping SSTables to decide whether a deletion tombstone could be promoted. When `bf_fp_chance` is set to `1.0` (or the filter file is missing), Cassandra replaces the real bloom filter with an `AlwaysPresentFilter` that always returns `true` for `isPresent`. This made the check always report "key is present" for every overlapping SSTable, so compaction never concluded that it was safe to drop a key — leaving columns that should have been deleted permanently alive on disk.

### Before / After

```java
// BEFORE — AlwaysPresentFilter always returns true, so no column is ever removed
if (sstable.getBloomFilter().isPresent(key.key) && sstable.getMinTimestamp() <= maxDeletionTimestamp)
    return false;
```

```java
// AFTER — detect AlwaysPresentFilter and fall back to index lookup
if (sstable.getMinTimestamp() <= maxDeletionTimestamp)
{
    if (sstable.getBloomFilter() instanceof AlwaysPresentFilter && sstable.getPosition(key, SSTableReader.Operator.EQ) != null)
        return false;
    else if (sstable.getBloomFilter().isPresent(key.key))
        return false;
}
```

---

## How to Spot

Sentinel/stub filter objects that always return `true` can silently break callers that treat the return value as a real negative. Whenever bloom-filter results gate irreversible decisions (tombstone promotion, cache eviction), verify behavior for the `AlwaysPresentFilter` and `AlwaysAbsentFilter` stub cases.

---

## Test

No test added.
