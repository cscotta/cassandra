# Bug 0421: SSTable Skipping Broken for Pre-3.0 COMPACT STORAGE Tables Due to Missing Primary Key Liveness

**Commit**: f8500ee911 · **Ticket**: CASSANDRA-16226
**Subsystem**: db (SinglePartitionReadCommand / SSTable read path)
**Category**: Logic error in condition

---

## Root Cause

`SinglePartitionReadCommand.canRemoveRow()` (renamed `isRowComplete()` by the fix) decided it could skip reading an older SSTable only if the row's `primaryKeyLivenessInfo` timestamp was newer than that SSTable's max timestamp.  COMPACT STORAGE tables created before 3.0 never write a primary key liveness token, so `primaryKeyLivenessInfo().isEmpty()` is always true; the old code treated an empty liveness info as "not newer" and always returned `false`, forcing every query to read all SSTables.  The fix checks liveness info only when it is present and falls back to checking whether any non-tombstone regular cell is present and recent enough, restoring the skip optimisation for compact tables.

### Before / After

```java
// BEFORE – empty liveness (always the case for pre-3.0 COMPACT tables) immediately returns false
if (row.primaryKeyLivenessInfo().isEmpty() || row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp)
    return false;
```

```java
// AFTER – skip the liveness check when it is absent; fall back to cell content check
// Note that compact tables will always have an empty primary key liveness info.
if (!row.primaryKeyLivenessInfo().isEmpty() && row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp)
    return false;
// ... check cells; return hasLiveCell || !metadata().isCQLTable()
```

---

## How to Spot

Conditions that treat a missing/empty optional field as a failure case are dangerous when the field is legitimately absent for a whole class of data.  When reviewing "can we skip this SSTable?" logic, confirm that each field being tested is guaranteed to be present for all table types; if not, the absence must be handled as a distinct third case rather than as a negative answer.

---

## Test

New `SSTablesIteratedTest.testCompactTableSkipping()` verifies that a point-lookup on a COMPACT STORAGE table reads only one SSTable (the most recent one) before and after `ALTER TABLE ... DROP COMPACT STORAGE`.
