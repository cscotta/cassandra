# Bug 0973: DISTINCT paging repeats first row when partition key changes

**Commit**: fa2fa602d9 · **Ticket**: CASSANDRA-10010
**Subsystem**: Query paging
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`RangeSliceQueryPager.isPreviouslyReturnedPartition()` used the same code path for both DISTINCT and non-DISTINCT range queries. For non-DISTINCT queries the method correctly checks whether the first cell in the next page is the same CQL row as the last cell returned, in order to skip already-seen data. But for DISTINCT queries, which return at most one row per partition, the check must stop as soon as it finds a row with the same partition key — it should not continue to compare cell names. Without the early return, the cell-name comparison path was reached for DISTINCT queries, and because static-only or single-row partitions have no regular cells, the comparison could fail to identify the row as previously returned, causing the first row of a new page to be emitted twice.

The fix adds an `isDistinct()` helper (using `!command.countCQL3Rows` as the discriminant, which is reliable for non-Thrift pagers) and returns `true` immediately when the pager is in DISTINCT mode and the partition key matches.

### Before / After
```java
// BEFORE
if (lastReturnedKey == null || !lastReturnedKey.equals(first.key))
    return false;
// falls through to cell-name comparison for ALL queries

// AFTER
if (lastReturnedKey == null || !lastReturnedKey.equals(first.key))
    return false;

// If the query is a DISTINCT one we can stop there
if (isDistinct())
    return true;

// falls through to cell-name comparison only for non-DISTINCT queries
```

---

## How to Spot
Paging "is this already returned?" logic is often shared across query types that have subtly different continuation semantics. Whenever a pager method handles both DISTINCT and non-DISTINCT execution, look for whether each branch performs the correct amount of comparison: DISTINCT needs only partition-key equality; non-DISTINCT needs cell-level equality. A shared code path that always reaches the deeper comparison is likely broken for the shallower case.

---

## Test
No test added.
