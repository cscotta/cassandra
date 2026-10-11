# Bug 0865: Paging Logic Counts Deleted Partitions with Static Columns

**Commit**: 84426d183a · **Ticket**: CASSANDRA-12107
**Subsystem**: Query / DataLimits / Paging
**Category**: Logic error in condition

---

## Root Cause
`DataLimits.CQLPagingLimiter.applyToPartition` set `hasLiveStaticRow = true` only when the static row was non-empty and live, but it never reset it to `false`. When scanning a partition that had been fully deleted, `rowInCurrentPartition` was reset to 0 correctly, but `hasLiveStaticRow` retained its value from the previous partition. The pager therefore believed the deleted partition still had a live static row, counted it toward the limit, and returned fewer rows than requested.

### Before / After
```java
// BEFORE
public void applyToPartition(DecoratedKey partitionKey, Row staticRow)
{
    rowInCurrentPartition = 0;
    if (!staticRow.isEmpty() && (assumeLiveData || staticRow.hasLiveData(nowInSec)))
        hasLiveStaticRow = true;
}

// AFTER
public void applyToPartition(DecoratedKey partitionKey, Row staticRow)
{
    rowInCurrentPartition = 0;
    hasLiveStaticRow = !staticRow.isEmpty() && (assumeLiveData || staticRow.hasLiveData(nowInSec));
}
```

---

## How to Spot
When reviewing code that accumulates boolean state across partitions, check that the state is unconditionally reassigned (not just set to `true`) at each partition boundary. A flag that is only ever set `true` inside an `if` but never explicitly cleared becomes stale as soon as a partition that would yield `false` is encountered. The symptom is a LIMIT query returning fewer rows than expected when some partitions have been deleted.

---

## Test
`SelectLimitTest.testLimitWithDeletedRowsAndStaticColumns`: creates five partitions with a static column, deletes one, then verifies that `SELECT * … LIMIT 2` returns exactly 2 rows from the surviving partitions.
