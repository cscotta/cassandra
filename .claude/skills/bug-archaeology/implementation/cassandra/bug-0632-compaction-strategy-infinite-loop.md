# Bug 0632: Compaction Strategy Infinite Loop When Transaction Acquire Fails

**Commit**: c253ed4fa7 · **Ticket**: CASSANDRA-14079
**Subsystem**: `db/compaction` (DateTieredCompactionStrategy, LeveledCompactionStrategy, SizeTieredCompactionStrategy, TimeWindowCompactionStrategy)
**Category**: Logic error in condition

---

## Root Cause

Each compaction strategy's `getNextBackgroundTask()` looped indefinitely when `tryModify()` failed to acquire a lifecycle transaction. If the same candidate set was returned on the next iteration — because the SSTable tracker had not yet replaced the compacted SSTables in the strategy manager — the loop would spin forever re-selecting and re-failing on the same tables. There was no escape condition for the case where selection is stable but modification always fails.

Additionally, `DateTieredCompactionStrategy.getMaxSSTableTimestamp()` called `Collections.max()` on an empty list, throwing a `NoSuchElementException`, which could surface when all SSTables had been removed between strategy decisions.

### Before / After

```java
// BEFORE (STCS, same pattern in all four strategies)
while (true) {
    List<SSTableReader> hottestBucket = getNextBackgroundSSTables(gcBefore);
    if (hottestBucket.isEmpty())
        return null;
    LifecycleTransaction transaction = cfs.getTracker().tryModify(hottestBucket, OperationType.COMPACTION);
    if (transaction != null)
        return new CompactionTask(cfs, transaction, gcBefore);
    // no exit condition — loops forever if tryModify keeps failing
}
```

```java
// AFTER
List<SSTableReader> previousCandidate = null;
while (true) {
    List<SSTableReader> hottestBucket = getNextBackgroundSSTables(gcBefore);
    if (hottestBucket.isEmpty())
        return null;
    if (hottestBucket.equals(previousCandidate)) {
        logger.warn("Could not acquire references for compacting SSTables ...");
        return null;   // break the loop
    }
    LifecycleTransaction transaction = cfs.getTracker().tryModify(hottestBucket, OperationType.COMPACTION);
    if (transaction != null)
        return new CompactionTask(cfs, transaction, gcBefore);
    previousCandidate = hottestBucket;
}
```

---

## How to Spot

Any `while (true)` loop that retries an operation involving external state (lock acquisition, reference counting, lifecycle transactions) needs an explicit escape path for the case where the external state is transiently or permanently uncooperative. A reviewer should check that every iteration either makes observable progress toward success or has a maximum-retry / stable-candidate guard that returns or throws rather than looping forever.

---

## Test

`AbstractCompactionStrategyTest` adds a test that simulates a tracker that always fails `tryModify` and asserts that each strategy's `getNextBackgroundTask` returns `null` rather than hanging.
