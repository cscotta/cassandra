# Bug 1138: SELECT * Returns Only the First Page of Data

**Commit**: e98be186d1 · **Ticket**: CASSANDRA-9913
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Resource leak

---

## Root Cause

Two separate bugs combined to truncate paged `SELECT *` results. First, `StatementRestrictions.isColumnRange()` incorrectly counted the fake clustering column present in static compact tables, causing those tables to be treated as range queries when they should use single-partition iteration — this affected query planning. Second, and more critically, the paging loop in `SelectStatement.execute()` called `iter.next()` to obtain a `RowIterator` but never closed it; because `RowIterator` is `AutoCloseable`, the un-closed iterator could prevent the underlying pager from advancing past the first page.

### Before / After

```java
// BEFORE
while (iter.hasNext())
    processPartition(iter.next(), options, result, nowInSec);
```

```java
// AFTER
while (iter.hasNext())
{
    try (RowIterator partition = iter.next())
    {
        processPartition(partition, options, result, nowInSec);
    }
}
```

---

## How to Spot

Whenever an iterator yields `AutoCloseable` items, every call to `next()` must be paired with a close, typically via try-with-resources. A bare `iter.next()` passed directly to another method is a signal that the resource may not be closed on all paths. In paging scenarios this is especially harmful because the pager uses the close signal to release the current page and prepare the next.

---

## Test

No dedicated test was added in this commit; the fix was verified against the existing paging test suite.
