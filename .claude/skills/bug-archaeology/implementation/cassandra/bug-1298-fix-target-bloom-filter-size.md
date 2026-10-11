# Bug 1298: Fix Anti-Compaction Target Bloom Filter Size

**Commit**: b0de327099 · **Ticket**: CASSANDRA-9060
**Subsystem**: db/compaction/CompactionManager.java
**Category**: Wrong constant / default value

---

## Root Cause

During anti-compaction, the expected bloom filter size was computed once before the per-SSTable loop using the total key count across all repaired SSTables (`SSTableReader.getApproximateKeyCount(repairedSSTables)`). Because each input SSTable is split into a repaired and an unrepaired output writer, using the aggregate count wildly over-allocated bloom filter memory for each individual output file. The fix moves the calculation inside the loop and bases it on the single input SSTable's own estimated key count, which is the correct upper bound for the keys that will land in each output.

### Before / After

```java
// BEFORE (outside loop, sum of all sstables)
int expectedBloomFilterSize = Math.max(cfs.metadata.getMinIndexInterval(),
    (int)(SSTableReader.getApproximateKeyCount(repairedSSTables)));
// ...
for (SSTableReader sstable : repairedSSTables)
{
    // expectedBloomFilterSize used here, but covers all sstables
```

```java
// AFTER (inside loop, per-sstable estimate)
for (SSTableReader sstable : repairedSSTables)
{
    int expectedBloomFilterSize = Math.max(cfs.metadata.getMinIndexInterval(),
        (int)sstable.estimatedKeys());
```

---

## How to Spot

When bloom filter or buffer sizes are computed from an aggregate statistic (sum or average of a collection) and then reused per-element inside a loop, the allocation is almost certainly wrong for each individual element. Look for size/capacity calculations that appear before a loop and are then fed unchanged into per-iteration constructors or writers.

---

## Test

No test added.
