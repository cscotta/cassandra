# Bug 2103: Tombstone Removal Check Uses Wrong Comparison Direction

**Commit**: 42ceccb90c · **Ticket**: CASSANDRA-5248
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionController.java
**Category**: Logic error in condition

---

## Root Cause

`CompactionController.shouldPurge` iterated over overlapping SSTables to decide whether a tombstone was safe to remove. The intent was: if any SSTable has data with a timestamp at or below `maxDeletionTimestamp`, the tombstone might be masking live data and must not be purged. The condition used `>=` instead of `<=`, inverting the logic — it skipped purging only when an SSTable's minimum timestamp was *above* the deletion timestamp, which is exactly backwards. SSTable data with an old minimum timestamp (potentially covered by the tombstone) would be incorrectly treated as safe to ignore, leading to tombstones being dropped when they should have been kept.

### Before / After

```java
// BEFORE
if (sstable.getBloomFilter().isPresent(key.key) && sstable.getMinTimestamp() >= maxDeletionTimestamp)
    return false;
```

```java
// AFTER
if (sstable.getBloomFilter().isPresent(key.key) && sstable.getMinTimestamp() <= maxDeletionTimestamp)
    return false;
```

---

## How to Spot

Timestamp range guard conditions (`>=` vs `<=`) controlling tombstone GC eligibility are high-risk one-character bugs. The safest review heuristic: state the condition in plain English ("an SSTable whose min timestamp is at or below the deletion time may contain data the tombstone covers") and verify the operator matches the English. A unit test that writes data, inserts a covering tombstone, compacts, and confirms the data does not resurface would catch this.

---

## Test

No test added.
