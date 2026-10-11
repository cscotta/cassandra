# Bug 1066: SELECT COUNT with LIMIT Returns Wrong Result

**Commit**: 20c0acc149 · **Ticket**: CASSANDRA-10487
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement.updateLimitForQuery()` applies the user-supplied `LIMIT` value to the number of rows fetched from storage, not just to the number of rows returned to the client. For aggregate queries (e.g. `SELECT COUNT(*) ... LIMIT 2`), the LIMIT must only constrain the final result set returned to the caller; the aggregate itself must see every matching row. Because the limit was propagated into the read path, aggregate functions computed counts, sums, and maxima over a truncated row set instead of the full partition, producing silently wrong answers.

### Before / After

```java
// BEFORE — limit applied even for aggregate queries
private int updateLimitForQuery(int limit)
{
    return restrictions.isNonCompositeSliceWithExclusiveBounds() && limit != Integer.MAX_VALUE
           ? limit + 1
           : limit;
}
```

```java
// AFTER — aggregates bypass the row-fetch limit
private int updateLimitForQuery(int limit)
{
    // If the query is for an aggregate, we do not want to limit the number of rows retrieved.
    if (selection.isAggregate())
        return Integer.MAX_VALUE;
    return restrictions.isNonCompositeSliceWithExclusiveBounds() && limit != Integer.MAX_VALUE
           ? limit + 1
           : limit;
}
```

---

## How to Spot

Whenever a query layer translates a user-visible `LIMIT` into a storage-layer row count, look for a guard that distinguishes aggregate from non-aggregate selections. An aggregate query requires a full scan regardless of the LIMIT because the limit applies to the output, not the input. Any path that passes `limit` directly to a read or scan method without checking `selection.isAggregate()` (or equivalent) is a candidate for this class of bug.

---

## Test

Regression tests added to `AggregationTest`: `SELECT COUNT` and `SELECT max()` queries with a `LIMIT` clause are asserted to return the same result as the same queries without a limit, confirming that the limit does not truncate the aggregate input.
