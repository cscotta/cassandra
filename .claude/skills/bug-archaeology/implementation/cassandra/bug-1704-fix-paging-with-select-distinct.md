# Bug 1704: Fix paging with SELECT DISTINCT

**Commit**: c843b6b85c · **Ticket**: CASSANDRA-6857
**Subsystem**: db/PagedRangeCommand, service/pager/QueryPagers
**Category**: Wrong constant / default value

---

## Root Cause

`PagedRangeCommand.countCQL3Rows()` unconditionally returned `true`, which caused incorrect row-counting logic when executing a `SELECT DISTINCT` query. A `SELECT DISTINCT` uses a `SliceQueryFilter` with `compositesToGroup == -1` and `count == 1` to fetch only the first cell of each partition; treating each such cell as a CQL3 row is correct for DISTINCT, but the hardcoded `true` forced the pager to treat these partitions as if they were normal multi-cell CQL3 rows, skewing the page-size limit. Additionally, `QueryPagers.mayNeedPaging` always returned `true` for `RangeSliceCommand`, even when the full result set fit within a single page.

### Before / After

```java
// BEFORE – PagedRangeCommand.java
public boolean countCQL3Rows()
{
    return true;
}
```

```java
// AFTER – PagedRangeCommand.java
public boolean countCQL3Rows()
{
    SliceQueryFilter filter = (SliceQueryFilter)predicate;
    return filter.compositesToGroup >= 0 || filter.count != 1;
}
```

---

## How to Spot

When a boolean flag method on a command object always returns a constant, verify that every caller scenario is actually covered by that constant — especially when the same command class is reused for multiple query shapes (e.g., normal SELECT vs. SELECT DISTINCT).

---

## Test

Existing unit tests in `ColumnFamilyStoreTest` were updated to pass the new `countCQL3Rows` parameter; no new dedicated DISTINCT paging test was added in this commit.
