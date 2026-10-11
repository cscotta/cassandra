# Bug 0098: RowFilter Purges Deletions When Reconciliation Is Required

**Commit**: 725e4ba3eb · **Ticket**: CASSANDRA-20541
**Subsystem**: db/filter (replica filtering protection / SAI)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`RowFilter.CQLFilter.applyToRow` unconditionally called `row.purge(DeletionPurger.PURGE_ALL, ...)` before evaluating the filter expression. When replica filtering protection (RFP) is active, the coordinator re-evaluates rows fetched from individual replicas using the full `RowFilter`. Purging deletions before that evaluation hides tombstones that RFP relies on to detect and discard rows that have since been deleted on some replicas. With the purge in place, a row that was deleted on one replica but not yet propagated would appear live on the coordinator, producing phantom rows in query results. The fix skips purging when `needsReconciliation()` returns true.

### Before / After

```java
// BEFORE
Row purged = row.purge(DeletionPurger.PURGE_ALL, nowInSec, metadata.enforceStrictLiveness());
if (purged == null)
    return null;
```

```java
// AFTER
// If we purge deletions when reconciliation is required, we hide information replica filtering
// protection would require to filter rows that are no longer matches at the coordinator.
Row purged = needsReconciliation() ? row
                                   : row.purge(DeletionPurger.PURGE_ALL, nowInSec, metadata.enforceStrictLiveness());
if (purged == null)
    return null;
```

---

## How to Spot

Any code that discards tombstones or cell-level deletions as an early optimisation must first verify that no downstream consumer depends on those deletions being present. Reconciliation paths (read repair, replica filtering protection, speculative reads) are the primary consumers; whenever a filter or transformation is inserted between raw replica data and the reconciler, check whether deletion information must be preserved.

---

## Test

`ReplicaFilteringWithStaticsTest.testRowFilterDeletePurging` (and a SAI variant): a row deleted on node 1 with a higher timestamp than the insert on node 3 must be absent from `ALLOW FILTERING` results; the assertion verifies only the surviving row is returned.
