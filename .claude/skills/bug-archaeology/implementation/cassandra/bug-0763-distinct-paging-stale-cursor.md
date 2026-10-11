# Bug 0763: DISTINCT paging skips rows due to stale lastReturnedName

**Commit**: 70e8b39b02 · **Ticket**: CASSANDRA-13017
**Subsystem**: Query pager / CQL
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`RangeSliceQueryPager` reuses `lastReturnedName` as the column-level start cursor when resuming after a page boundary. For normal range-slice queries this is correct: it lets the next page pick up right after the last cell returned. For `DISTINCT` queries — which return only one synthetic cell per partition (the partition key or a static column) — `lastReturnedName` always points into the interior of a partition that has already been fully returned. Passing it as the column start on the next page causes the server to skip every partition whose key is equal to `lastReturnedKey`, leading to missed rows and incorrect result counts.

### Before / After
```java
// BEFORE — lastReturnedName always used as column start
Composite start = lastReturnedName == null ? sf.start() : lastReturnedName;

// AFTER — DISTINCT queries must always restart from the filter's own start
// (each partition contributes exactly one "row" so there is nothing to resume mid-partition)
Composite start = lastReturnedName == null || isDistinct() ? sf.start() : lastReturnedName;
```

---

## How to Spot
Whenever a pager carries per-partition cursor state, verify that the semantics hold for query types that return at most one logical row per partition (DISTINCT, aggregates, etc.). Using a mid-partition cursor as a column-level resume point is safe only when subsequent pages can legitimately start from within the same partition.

---

## Test
`QueryPagerTest` updated: the `rangeSliceQuery` helper is changed to pass the `isDistinct=true` flag so the new code path is exercised.
