# Bug 1769: Avoid Repairing Already Repaired Data

**Commit**: a7b72140b6 · **Ticket**: CASSANDRA-5153
**Subsystem**: src/java/org/apache/cassandra/service/ActiveRepairService.java, repair/, io/sstable/
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Cassandra had no way to distinguish SSTables that had already been validated during a repair session from those that had not, so every repair always re-examined every SSTable for every range. The fix introduces a `repairedAt` timestamp field in `StatsMetadata` (persisted in the SSTable statistics file), which is set to a non-zero value after a successful repair anticompaction. Compaction strategies then use `SSTableReader.isRepaired()` to exclude already-repaired SSTables from repair scans, and a new "parent repair session" concept coordinates the anticompaction step that marks SSTables as repaired once streaming completes successfully.

### Before / After

```java
// BEFORE – no repairedAt concept; all SSTables included in every repair
// SSTableReader had no isRepaired() method
// StatsMetadata had no repairedAt field
```

```java
// AFTER – SSTables carry a repair timestamp; compaction skips them
public boolean isRepaired() {
    return sstableMetadata.repairedAt != ActiveRepairService.UNREPAIRED_SSTABLE;
}

// StatsMetadata gains:
public final long repairedAt;   // 0 == never repaired

// Anticompaction marks SSTables after successful repair:
sstableMetadata.mutateRepairedAt(repairedAt);
```

---

## How to Spot

When a bulk operation (repair, scrub, compaction) iterates over all SSTables without filtering on a "completion state" field, it will redundantly process data on subsequent runs. Reviewers should check whether the iteration predicate can distinguish completed vs. pending items, especially when the operation is expensive and is expected to converge over time.

---

## Test

`LeveledCompactionStrategyTest` was extended with tests covering repair interactions; `AntiEntropyServiceTestAbstract` and `DifferencerTest` were updated to exercise the new parent-session protocol.
