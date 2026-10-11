# Bug 0345: Useless SSTable Reads During Single-Partition Queries When Row Is Already Deleted

**Commit**: 9f492844d5 · **Ticket**: CASSANDRA-16944
**Subsystem**: src/java/org/apache/cassandra/db/SinglePartitionReadCommand.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`isRowComplete` was called to decide whether to stop reading older SSTables once a row had been found. The method checked for a live primary-key liveness info, but it never checked whether the row itself had already been covered by a row deletion or range deletion that post-dated the current SSTable's max timestamp. As a result, when a newer SSTable contained only a row deletion (no live data), the code kept opening older SSTables unnecessarily — and in some configurations could produce incorrect results by returning stale data that the deletion should have suppressed. Additionally, static rows were inadvertently passed through the non-static liveness check path.

### Before / After

```java
// BEFORE
// Note that compact tables will always have an empty primary key liveness info.
if (metadata().isCQLTable() && (row.primaryKeyLivenessInfo().isEmpty()
        || row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp))
    return false;
```

```java
// AFTER
if (!row.isStatic())
{
    // If the row has been deleted we know we have enough information.
    if (!row.deletion().isLive() && row.deletion().time().deletes(sstableTimestamp))
        return true;

    if (metadata().isCQLTable() && (row.primaryKeyLivenessInfo().isEmpty()
            || row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp))
        return false;
}
```

---

## How to Spot

When implementing an early-exit optimization that stops reading lower-priority sources once data is "complete," ensure that tombstones and deletions are treated as completing the row, not just liveness markers. Any SSTable iterator short-circuit that checks only positive liveness without also checking covering deletions will miss a class of valid termination conditions.

---

## Test

Adds `testCompactAndNonCompactTableWithRowOnOneNodeAndRowDeletionOnTheOther` and a range-deletion variant in `SinglePartitionReadCommandTest`: two nodes hold a row on one and a covering deletion on the other; the test asserts both correct results and, via `SSTablesIteratedTest`, that only the minimum number of SSTables are read.
