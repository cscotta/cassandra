# Bug 1254: Tombstone Counter Counts Partition-Shadowed Cells, Triggering False TombstoneOverwhelmingException

**Commit**: bed42c2104 · **Ticket**: CASSANDRA-9299
**Subsystem**: src/java/org/apache/cassandra/db/filter/ColumnCounter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnCounter.count()` incremented the `ignored` counter for every cell that was not live, including cells that were already shadowed by a higher-level partition tombstone. A partition tombstone renders all underlying cell tombstones invisible—they will never be returned to the client—so counting them against the `tombstone_failure_threshold` was incorrect. A query touching a partition that had been fully deleted could therefore throw `TombstoneOverwhelmingException` even though zero tombstones were actually visible to the user. The fix adds an early return when `tester.isDeleted(column)` is true, so only genuine cell-level tombstones increment the counter.

### Before / After

```java
// BEFORE – counts cells shadowed by partition tombstone
public void count(Column column, DeletionInfo.InOrderTester tester) {
    if (!isLive(column, tester, timestamp))
        ignored++;   // fired even for partition-shadowed cells
    else
        live++;
}
```

```java
// AFTER – shadowed cells are skipped entirely
public void count(Column column, DeletionInfo.InOrderTester tester) {
    if (tester.isDeleted(column))
        return;      // partition tombstone shadows this cell; don't count it
    if (column.isLive(timestamp))
        live++;
    else
        tombstones++;
}
```

---

## How to Spot

Threshold checks on tombstone counts should mirror what the query engine considers "scanned tombstones"—cells that are evaluated and potentially returned or skipped by the read path, not cells that are pre-filtered by higher-level deletions. Any tombstone counter that does not first test for enclosing range/partition deletions will overcount in the presence of partition-level deletes.

---

## Test

`SliceQueryFilterWithTombstonesTest` verifies that queries below the threshold succeed, queries above it throw `TombstoneOverwhelmingException`, and queries where all tombstones are shadowed by a partition-level delete do not throw even when the raw cell count exceeds the threshold.
