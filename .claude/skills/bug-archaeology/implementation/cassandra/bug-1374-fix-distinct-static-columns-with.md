# Bug 1374: DISTINCT on Static Columns Returns Non-Distinct Results When Paging Is Enabled

**Commit**: 2acbab6516 · **Ticket**: CASSANDRA-8087
**Subsystem**: src/java/org/apache/cassandra/db/PagedRangeCommand.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`PagedRangeCommand.countCQL3Rows()` used a heuristic—`compositesToGroup >= 0 || count != 1`—to detect `SELECT DISTINCT` and return `false` (one cell per partition). The heuristic was fragile: it assumed a DISTINCT filter always has `compositesToGroup == -1` and `count == 1`, but that invariant was not structurally guaranteed and broke when paging introduced a different filter count. Returning `true` from `countCQL3Rows()` caused `ColumnFamilyStore.filter()` to apply CQL3-row counting logic, which consumed the entire page budget against a single partition's static cell and stopped returning results for subsequent partitions.

### Before / After

```java
// BEFORE: brittle heuristic that conflated two different conditions
public boolean countCQL3Rows()
{
    SliceQueryFilter filter = (SliceQueryFilter)predicate;
    return filter.compositesToGroup >= 0 || filter.count != 1;
}
```

```java
// AFTER: simplified rule relying on the invariant that paged queries always have count > 1
public boolean countCQL3Rows()
{
    // For CQL3 queries, unless this is a DISTINCT query, the slice filter count is the LIMIT.
    // We don't page queries with LIMIT <= pageSize so a paged query with count == 1 is DISTINCT.
    return ((SliceQueryFilter)predicate).count != 1;
}
```

---

## How to Spot

When a boolean method relies on multiple independent field values to infer semantic intent (here: "is this a DISTINCT query?"), it is inherently fragile—any refactoring that changes how the filter is constructed can silently break the inference. Prefer encoding the intent explicitly (a dedicated flag or subtype) rather than reconstructing it from incidental property combinations. Also review `SliceQueryFilter.lastCounted()`, which was separately overcounting by one without the `Math.min` guard.

---

## Test

No test added in this commit; the fix was validated by the existing paging test suite and manual verification noted in the ticket.
