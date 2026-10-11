# Bug 1020: SELECT with IN restriction, ORDER BY and LIMIT returns wrong results

**Commit**: 7e6c1d5483 · **Ticket**: CASSANDRA-10729
**Subsystem**: CQL / Query Execution
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a `SELECT` used an `IN` restriction on the partition key together with `ORDER BY` and `LIMIT`, the user-specified row limit was applied at the storage layer (`DataLimits`) before the in-memory post-query ordering step. Because each partition's rows are fetched and limited independently, the early truncation discarded rows that would have survived after re-ordering, causing the final result set to be smaller than or different from what the user requested. The fix separates the user limit from the `DataLimits` object used for fetching: when post-query ordering is needed, `DataLimits` carries no limit (so all relevant rows are fetched), and the user limit is applied *after* `orderResults()` via an explicit `cqlRows.trim(userLimit)` call.

### Before / After
```java
// BEFORE — limit baked into DataLimits even when post-ordering is required
public DataLimits getLimit(QueryOptions options) throws InvalidRequestException
{
    int userLimit = -1;
    if (limit != null && !selection.isAggregate())
    {
        // ... parse userLimit from options ...
    }
    return userLimit < 0 ? DataLimits.NONE : DataLimits.cqlLimits(userLimit);
}
// result set trimmed at storage layer, before orderResults()

// AFTER — limit only applied at storage layer when no post-ordering is needed
private DataLimits getDataLimits(int userLimit)
{
    int cqlRowLimit = DataLimits.NO_LIMIT;
    if (!selection.isAggregate() && !needsPostQueryOrdering())
        cqlRowLimit = userLimit;
    ...
    return cqlRowLimit == DataLimits.NO_LIMIT ? DataLimits.NONE : DataLimits.cqlLimits(cqlRowLimit);
}
// user limit applied after ordering:
orderResults(cqlRows);
cqlRows.trim(userLimit);
```

---

## How to Spot
Any query path that performs post-fetch sorting must not apply row limits until after sorting is complete. In review, trace the lifetime of the user `LIMIT` value: if it reaches `DataLimits` (used by the storage engine) and the same statement also sets `needsPostQueryOrdering()`, the limit is being applied too early. Look for `ORDER BY` combined with multi-partition access patterns (e.g., `IN` on partition key, secondary index range scans) where sorting cannot happen inside the storage layer.

---

## Test
`SelectOrderByTest` was extended with a test that issues a `SELECT ... WHERE pk IN (...) ORDER BY ck LIMIT N` and verifies the correct number and order of results are returned.
