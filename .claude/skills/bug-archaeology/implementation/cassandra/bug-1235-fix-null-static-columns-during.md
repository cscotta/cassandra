# Bug 1235: Fix Null Static Columns During Paging, Reversed Queries

**Commit**: d075540c46 · **Ticket**: CASSANDRA-8502
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java, db/filter/ColumnCounter.java, service/pager/AbstractQueryPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The paging infrastructure tracked progress by recording the last column seen per page, then resuming from that position. For tables with static columns, the pager could record a static column as the last-seen cell and use it as the resume point, causing subsequent pages to start in the wrong place—or skip the static columns entirely. Additionally, the reversed-query code path lacked a `GroupByPrefixReversed` counter variant, so column grouping (needed to honour `LIMIT` correctly) fell back to the forward-direction counter, which miscounted rows in reverse scans. A related gap was that `withUpdatedStart` did not inject a dedicated static-column slice when advancing past the static range.

### Before / After

```java
// BEFORE: pager used any column, including statics, as the page-boundary marker
protected static Column firstColumn(ColumnFamily cf)
{
    return cf.iterator().next();
}
```

```java
// AFTER: pager skips static columns when looking for the resume point
protected Column firstNonStaticColumn(ColumnFamily cf)
{
    for (Column column : cf)
    {
        ColumnDefinition def = cfm.getColumnDefinitionFromColumnName(column.name());
        if (def == null || def.type != ColumnDefinition.Type.STATIC)
            return column;
    }
    return null;
}
```

---

## How to Spot

When a query layer handles static columns and regular columns together, every operation that selects a "representative" cell (first, last, boundary) must explicitly filter out statics, because static cells exist outside the normal clustering-key order and will mislead any position-based logic. Review paging, range scanning, and cursor-advancement code for calls to `iterator().next()` or similar that lack a static-column exclusion check.

---

## Test

`AbstractQueryPagerTest` was updated; `MultiColumnRelationTest` also gained coverage for the reversed-query case.
