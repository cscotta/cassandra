# Bug 2194: Infinite Compaction Loop When Tombstone Estimation Exceeds Threshold but Nothing Is Dropped

**Commit**: b74a00b289 · **Ticket**: CASSANDRA-4781
**Subsystem**: src/java/org/apache/cassandra/db/compaction/AbstractCompactionStrategy.java, src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Logic error in condition

---

## Root Cause

`AbstractCompactionStrategy.worthDroppingTombstones()` used estimated statistics to decide whether a single-SSTable compaction would remove enough tombstones to be worthwhile. Because these are estimates, the compaction could be triggered repeatedly on the same SSTable even after it had just been compacted and no tombstones were actually dropped, producing an infinite loop. Two separate estimation bugs compounded the problem: (1) the column-count estimation used `percentile(remainingKeysRatio)` instead of `mean()`, which could return an inflated value, and (2) when an SSTable had fewer than two key samples the overlap calculation divided by zero or returned nonsense. The fix adds a minimum elapsed time guard (`tombstone_compaction_interval`, default 86400 s) so the same SSTable cannot be targeted for tombstone compaction again until at least that interval has passed since its creation.

### Before / After

```java
// BEFORE – no time guard; estimation alone decided eligibility
double droppableRatio = sstable.getEstimatedDroppableTombstoneRatio(gcBefore);
if (droppableRatio <= tombstoneThreshold)
    return false;
// ...
long columns = sstable.getEstimatedColumnCount().percentile(remainingKeysRatio) * remainingKeys;
```

```java
// AFTER – time guard prevents re-triggering on freshly compacted SSTable
if (System.currentTimeMillis() < sstable.getCreationTimeFor(Component.DATA)
        + tombstoneCompactionInterval * 1000)
    return false;
// ...
if (sstable.getKeySamples().size() < 2)
    return false;
long columns = sstable.getEstimatedColumnCount().mean() * remainingKeys;
```

---

## How to Spot

Whenever a compaction eligibility check is based purely on statistical estimates, ask whether a freshly compacted SSTable could still appear eligible (because the stats have not been refreshed). Any such check needs a time-based or generation-based guard to prevent the compaction manager from re-selecting the same SSTable immediately after it has already been processed.

---

## Test

Existing `CompactionsTest` was refactored to share the single-SSTable compaction logic between size-tiered and leveled strategies; the `SchemaLoader` schema sets `tombstone_compaction_interval=1` to allow the test to exercise the path without waiting 24 hours.
