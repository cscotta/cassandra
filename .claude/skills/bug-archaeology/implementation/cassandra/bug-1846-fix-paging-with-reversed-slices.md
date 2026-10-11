# Bug 1846: Fix Paging with Reversed Slices

**Commit**: 5008507ca2 · **Ticket**: CASSANDRA-6343
**Subsystem**: src/java/org/apache/cassandra/service/pager (query paging)
**Category**: Logic error in condition

---

## Root Cause

The query pager in `AbstractQueryPager` always treated the first column of a result page as "the last returned position" and always discarded from the head of a `ColumnFamily` when trimming overlap. For reversed slices, column order inside a `ColumnFamily` is inverted relative to query order, so "first returned" is actually the last column in the internal sort order. The pager was therefore tracking the wrong boundary position and removing the wrong end when trimming already-seen columns, producing skipped or duplicated rows on subsequent pages. The fix introduces an `isReversed()` hook so each pager subclass can report its direction, and the discard helpers swap their head/tail strategy accordingly.

### Before / After

```java
// BEFORE (AbstractQueryPager.java) – direction-unaware discard
private List<Row> discardFirst(List<Row> rows)
{
    Row first = rows.get(0);
    ColumnFamily newCf = discardFirst(first.cf);   // always removes head
    ...
}

protected boolean recordLast(Row last)
{
    lastReturned = lastName(last.cf);   // always tracks tail column
    return true;
}
```

```java
// AFTER (AbstractQueryPager.java) – direction-aware discard
protected abstract boolean isReversed();

private List<Row> discardFirst(List<Row> rows)
{
    Row first = rows.get(0);
    ColumnFamily newCf = isReversed()
                       ? discardLast(first.cf)    // reversed: remove tail
                       : discardFirst(first.cf);  // forward: remove head
    ...
}

// SliceQueryPager.java
protected boolean recordLast(Row last)
{
    lastReturned = isReversed() ? firstName(last.cf) : lastName(last.cf);
    return true;
}
```

---

## How to Spot

Whenever a pager records a "last seen" position to resume iteration, verify the position is computed from the query-order perspective, not the storage-order perspective. If the data can be returned in reverse order, every head/tail operation must be conditioned on direction.

---

## Test

`QueryPagerTest.reversedSliceQueryTest()` — fetches pages of 3 from a reversed slice over `c1`–`c8` and asserts that each page contains the correct columns in descending order without duplicates or gaps.
