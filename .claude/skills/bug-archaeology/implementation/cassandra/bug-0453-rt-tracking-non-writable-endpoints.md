# Bug 0453: Range Tombstone Tracking for Non-Writable Endpoints During Read Repair

**Commit**: 9b6e54ee02 · **Ticket**: CASSANDRA-16084
**Subsystem**: `src/java/org/apache/cassandra/service/reads/repair/RowIteratorMergeListener.java`
**Category**: Missing null / bounds check

---

## Root Cause

`RowIteratorMergeListener` tracks the open/close state of range tombstone markers per endpoint so it can emit the correct repair mutations. The `writeBackTo` bitset records which endpoints actually need repair writes. However, the range-tombstone tracking loop iterated over all `versions` entries regardless of whether that index had `writeBackTo.get(i)` set. When a new node was bootstrapping and its token range was pending, the node would appear in the replica list but not in `writeBackTo`. Accessing its slot in the tombstone-tracking state without a guard caused an `AssertionError` (or an incorrect mutation) when the state machine encountered a marker it had no business tracking.

### Before / After

```java
// BEFORE
for (int i = 0; i < versions.length; i++)
{
    RangeTombstoneMarker marker = versions[i];
    // update what the source now thinks is the current deletion
    ...
}
```

```java
// AFTER
for (int i = 0; i < versions.length; i++)
{
    if (!writeBackTo.get(i))
        continue; // not collecting a mutation for this version/source, skip
    RangeTombstoneMarker marker = versions[i];
    ...
}
```

---

## How to Spot

Any loop that iterates a parallel array of per-replica state and modifies internal tracking structures should be guarded by the same "will we actually write to this replica?" check used in the outer repair logic. Missing guards become visible during range movements (bootstrap, decommission) when the replica list and write-back set diverge.

---

## Test

`ReadRepairTest.readRepairRTRangeMovementTest` builds a 3-node cluster, seeds range tombstones, then bootstraps a fourth node mid-read to force a pending-range scenario, and asserts the coordinated read completes without error.
