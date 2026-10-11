# Bug 0915: Range query paging crashes when all clustering columns are specified

**Commit**: 474d3bf8c1 · **Ticket**: CASSANDRA-11669
**Subsystem**: Query paging
**Category**: Logic error in condition

---

## Root Cause
`PartitionRangeReadCommand.getPager()` branched on `isNamesQuery()` and routed "names" queries (those where all clustering columns are fully specified) to `RangeNamesQueryPager`. That pager's constructor contained `assert !command.isNamesQuery()` — wait, it was the inverse: the slice pager asserted the opposite. The real problem was that `RangeNamesQueryPager` used completely different in-partition paging logic that did not handle the general case correctly, producing wrong or missing rows when paging through a range scan where every clustering column had been supplied. The fix removes the branch entirely: `RangeNamesQueryPager` is deleted and the unified `PartitionRangeQueryPager` (renamed from `RangeSliceQueryPager`) handles all range queries regardless of whether the filter is a names or slice query.

### Before / After
```java
// BEFORE
public QueryPager getPager(PagingState pagingState, int protocolVersion) {
    if (isNamesQuery())
        return new RangeNamesQueryPager(this, pagingState, protocolVersion);
    else
        return new RangeSliceQueryPager(this, pagingState, protocolVersion);
}

// AFTER
public QueryPager getPager(PagingState pagingState, int protocolVersion) {
    return new PartitionRangeQueryPager(this, pagingState, protocolVersion);
}
```

---

## How to Spot
Any code that dispatches to separate pager implementations based on whether a query is a "names" vs "slice" query is a red flag. When two pagers share responsibility for the same command type but differ only in the filter style, they must remain strictly in sync; the easier fix is a single, unified pager. Also watch for assertions in pager constructors that constrain which command variants they accept — these assertions become landmines when the dispatch logic is incorrect.

---

## Test
No new test added in this commit; coverage relies on existing paging tests that exercise range queries with fully-specified clustering columns.
