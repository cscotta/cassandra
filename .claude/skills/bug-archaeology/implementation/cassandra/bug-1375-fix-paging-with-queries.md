# Bug 1375: Multi-Partition IN Query Paging Returns Too Many Rows and Ignores Query LIMIT

**Commit**: 621899355a · **Ticket**: CASSANDRA-8408
**Subsystem**: src/java/org/apache/cassandra/service/pager/MultiPartitionPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`MultiPartitionPager` initialised its `remaining` counter by summing `maxRemaining()` across all per-partition pagers, which for an `IN` query with N partitions inflated the total to `N × limit`. The pager then served up to `pageSize` rows per page call without any cap against the query's original `LIMIT`, allowing it to return far more rows than the user requested. The fix passes the original `limitForQuery` directly as `remaining`, so the pager correctly terminates once the user's limit is satisfied regardless of how many partitions remain.

### Before / After

```java
// BEFORE: remaining inflated to sum of all per-pager limits
private static int computeRemaining(SinglePartitionPager[] pagers)
{
    long remaining = 0;
    for (SinglePartitionPager pager : pagers)
        remaining += pager.maxRemaining();
    return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)remaining;
}
// ...
remaining = state == null ? computeRemaining(pagers) : state.remaining;
```

```java
// AFTER: remaining is exactly the query-level LIMIT
remaining = state == null ? limitForQuery : state.remaining;
// fetch loop also capped by remaining:
int remainingThisQuery = Math.min(remaining, pageSize);
```

---

## How to Spot

When a pager wraps multiple sub-pagers, the total `remaining` budget must reflect the query-level `LIMIT`, not the sum of sub-pager capacities. Summing sub-pager budgets is correct for a streaming scan (where each sub-pager represents a disjoint range), but wrong for a bounded `IN` query where a single LIMIT applies across all partitions. Review any `MultiPartition` or fan-out pager to confirm its initial `remaining` is sourced from the original query limit rather than derived from sub-pager state.

---

## Test

`QueryPagerTest` updated to pass `limitForQuery` to `Pageable.ReadCommands` constructor, exercising the corrected pager initialisation path.
